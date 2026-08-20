package com.breath.trainer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.breath.trainer.breathing.BreathingEngine
import com.breath.trainer.breathing.pattern.toPhaseOrNull
import com.breath.trainer.ui.theme.BreathTrainerTheme
import com.breath.trainer.ui.theme.PhaseExhale
import com.breath.trainer.ui.theme.PhaseHold
import com.breath.trainer.ui.theme.PhaseInhale
import com.breath.trainer.ui.theme.PhaseReady

/**
 * 呼吸光球：随阶段缩放与呼吸。
 *
 * - READY/EXHALE 起点: 0.5
 * - INHALE: 撑到 0.92
 * - HOLD: 维持 0.92
 * 同时显示外圈光环（更柔和的呼吸感），并附带进度环。
 *
 * 整体半径被夹在 canvas 短边的 0.45 倍以内，确保外圈柔光 / 角落点 / 进度环
 * 在窄屏设备上也不会伸出 PhaseTrack 之外或压到 RoundIndicator。
 */
@Composable
fun BreathOrb(
    state: BreathingEngine.State,
    modifier: Modifier = Modifier,
) {
    val phase = state.phase
    val progress = state.progress
    val scheme = MaterialTheme.colorScheme

    // 颜色随阶段变化
    val phaseColor = when (phase) {
        BreathingEngine.Phase.INHALE -> PhaseInhale
        BreathingEngine.Phase.HOLD_AFTER_INHALE -> PhaseHold
        BreathingEngine.Phase.HOLD_AFTER_EXHALE -> PhaseHold
        BreathingEngine.Phase.EXHALE -> PhaseExhale
        else -> PhaseReady
    }

    // 目标缩放（吸气逐渐放大；屏息保持；呼气逐渐收缩）。
    // 把最大缩放从 1.0 降到 0.92，让外圈柔光 / 角落点 / 进度环在 4-7-8、盒式这种
    // 节奏里也不至于压到 PhaseTrack 或伸出屏外；屏幕较矮的设备上仍可呼吸。
    val maxScale = 0.92f
    val minScale = 0.5f
    val targetScale = when (phase) {
        BreathingEngine.Phase.INHALE -> minScale + (maxScale - minScale) * progress
        BreathingEngine.Phase.HOLD_AFTER_INHALE -> maxScale
        BreathingEngine.Phase.EXHALE -> maxScale - (maxScale - minScale) * progress
        BreathingEngine.Phase.HOLD_AFTER_EXHALE -> minScale
        BreathingEngine.Phase.READY -> minScale
        BreathingEngine.Phase.COMPLETE -> 0.48f + 0.04f * kotlin.math.sin(progress * 3.14f)
    }

    // 该阶段剩余的时长（用于动画时长）。如果是 0/未开始，回退到固定的 4s。
    val phaseSeconds = when (phase) {
        BreathingEngine.Phase.INHALE,
        BreathingEngine.Phase.EXHALE -> (state.pattern.steps.firstOrNull {
            it.kind.toPhaseOrNull() == phase
        }?.seconds ?: 4)
        BreathingEngine.Phase.HOLD_AFTER_INHALE,
        BreathingEngine.Phase.HOLD_AFTER_EXHALE -> (state.pattern.steps.firstOrNull {
            it.kind.toPhaseOrNull() == phase
        }?.seconds ?: 4)
        BreathingEngine.Phase.READY -> 3
        else -> 0
    }.coerceAtLeast(1)

    // 缩放动画
    val scale = remember { Animatable(minScale) }
    LaunchedEffect(phase, phaseSeconds) {
        when (phase) {
            BreathingEngine.Phase.INHALE ->
                scale.animateTo(targetScale, tween(phaseSeconds * 1000 - 100, easing = LinearEasing))
            BreathingEngine.Phase.EXHALE ->
                scale.animateTo(targetScale, tween(phaseSeconds * 1000 - 100, easing = LinearEasing))
            BreathingEngine.Phase.HOLD_AFTER_INHALE ->
                scale.animateTo(maxScale, tween(400, easing = FastOutSlowInEasing))
            BreathingEngine.Phase.HOLD_AFTER_EXHALE ->
                scale.animateTo(minScale, tween(400, easing = FastOutSlowInEasing))
            BreathingEngine.Phase.READY ->
                scale.animateTo(minScale, tween(600, easing = FastOutSlowInEasing))
            BreathingEngine.Phase.COMPLETE ->
                scale.animateTo(minScale, tween(800, easing = FastOutSlowInEasing))
        }
    }

    // 持续微呼吸：用 progress 推动的颜色"心跳"
    val infinite = rememberInfiniteTransition(label = "breath-orb")
    val pulse by infinite.animateFloat(
        initialValue = 0.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = this.size
            val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
            // 用最短边的 45% 作为半径上限，给外圈柔光 / 进度环 / 角落脉冲点留出余量，
            // 避免在窄屏或低高度设备上压到 PhaseTrack 或伸出左右边界。
            val maxRadius = kotlin.math.min(canvasSize.width, canvasSize.height) * 0.45f
            val radius = maxRadius * scale.value

            // 最外层柔光
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(phaseColor.copy(alpha = 0.05f), Color.Transparent),
                    center = center,
                    radius = radius * 1.18f,
                ),
                radius = radius * 1.18f,
                center = center,
            )

            // 第 2 圈
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(phaseColor.copy(alpha = 0.16f), Color.Transparent),
                    center = center,
                    radius = radius * 1.06f,
                ),
                radius = radius * 1.06f,
                center = center,
            )

            // 主体光球：从中心向外渐变
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        phaseColor.copy(alpha = 0.95f),
                        phaseColor.copy(alpha = 0.55f),
                        phaseColor.copy(alpha = 0.0f),
                    ),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )

            // 最内层亮点
            drawCircle(
                color = Color.White.copy(alpha = 0.45f + 0.3f * (1f - scale.value)),
                radius = radius * 0.32f,
                center = center,
            )

            // 描边圈
            drawCircle(
                color = phaseColor.copy(alpha = 0.7f),
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx()),
            )
            drawCircle(
                color = scheme.outline.copy(alpha = 0.3f),
                radius = radius * 1.22f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx()),
            )

            // 角落脉冲小点：收回到主球外缘附近，不再乘 1.2 跑到屏外
            val smallDotRadius = 3.dp.toPx() + 1.5.dp.toPx() * pulse
            val dotOrbit = radius * 1.08f
            drawCircle(color = phaseColor.copy(alpha = 0.4f), radius = smallDotRadius, center = center.copy(x = center.x - dotOrbit))
            drawCircle(color = phaseColor.copy(alpha = 0.4f), radius = smallDotRadius, center = center.copy(x = center.x + dotOrbit))
            drawCircle(color = phaseColor.copy(alpha = 0.4f), radius = smallDotRadius, center = center.copy(y = center.y - dotOrbit))
            drawCircle(color = phaseColor.copy(alpha = 0.4f), radius = smallDotRadius, center = center.copy(y = center.y + dotOrbit))

            // 当前阶段秒数进度环：紧贴主球外缘，绝不超出可绘制区域
            val arcRadius = radius * 1.14f
            val sweep = 360f * progress
            drawArc(
                color = phaseColor,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                size = Size(arcRadius * 2, arcRadius * 2),
                style = Stroke(width = 3.5.dp.toPx()),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BreathOrbPreview() {
    BreathTrainerTheme {
        BreathOrb(state = BreathingEngine.State(phase = BreathingEngine.Phase.INHALE, progress = 0.5f))
    }
}
