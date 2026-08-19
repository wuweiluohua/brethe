package com.breath.trainer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.breath.trainer.R
import com.breath.trainer.breathing.BreathingEngine
import com.breath.trainer.breathing.pattern.BreathingPattern
import com.breath.trainer.breathing.pattern.BreathingPatterns
import com.breath.trainer.breathing.pattern.BreathingStep
import com.breath.trainer.breathing.pattern.toPhaseOrNull
import com.breath.trainer.ui.TrainerViewModel
import com.breath.trainer.ui.components.BreathOrb
import com.breath.trainer.ui.components.SettingsSheet
import com.breath.trainer.ui.theme.BreathTrainerTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Phase -> 显示文案资源 ID。
 *
 * 之所以放在 Composable 函数内是因为要用到 stringResource 之外的 resources。
 */
private fun BreathingEngine.Phase.labelStringRes(): Int = when (this) {
    BreathingEngine.Phase.READY -> R.string.phase_ready
    BreathingEngine.Phase.INHALE -> R.string.phase_inhale
    BreathingEngine.Phase.HOLD_AFTER_INHALE -> R.string.phase_hold_inhale
    BreathingEngine.Phase.HOLD_AFTER_EXHALE -> R.string.phase_hold_exhale
    BreathingEngine.Phase.EXHALE -> R.string.phase_exhale
    BreathingEngine.Phase.COMPLETE -> R.string.phase_complete
}

/** 当前 Step 短标签，用于底部 PhaseTrack 等。 */
private fun BreathingStep.StepKind.labelStringRes(): Int = when (this) {
    BreathingStep.StepKind.INHALE -> R.string.phase_inhale
    BreathingStep.StepKind.HOLD_AFTER_INHALE -> R.string.phase_hold_inhale
    BreathingStep.StepKind.EXHALE -> R.string.phase_exhale
    BreathingStep.StepKind.HOLD_AFTER_EXHALE -> R.string.phase_hold_exhale
}

/** 呼吸节奏名对应的 string id。 */
private fun BreathingPattern.nameStringRes(): Int = when (id) {
    BreathingPatterns.FOUR_SEVEN_EIGHT.id -> R.string.pattern_478_name
    BreathingPatterns.FOUR_TWO_SIX.id -> R.string.pattern_426_name
    BreathingPatterns.BOX.id -> R.string.pattern_box_name
    else -> R.string.pattern_478_name
}

private fun BreathingStep.StepKind.tipStringRes(): Int = when (this) {
    BreathingStep.StepKind.INHALE -> R.string.tip_inhale
    BreathingStep.StepKind.HOLD_AFTER_INHALE -> R.string.tip_hold_inhale
    BreathingStep.StepKind.EXHALE -> R.string.tip_exhale
    BreathingStep.StepKind.HOLD_AFTER_EXHALE -> R.string.tip_hold_exhale
}

@Composable
fun BreathingScreen(
    viewModel: TrainerViewModel,
    keepScreenOn: Boolean,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uiSettings by viewModel.uiSettings.collectAsStateWithLifecycle()

    var settingsVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            HeaderRow(onSettingsClick = { settingsVisible = true })

            Spacer(modifier = Modifier.height(10.dp))

            PatternSelector(
                patterns = BreathingPatterns.ALL,
                selected = uiSettings.pattern,
                onSelect = { viewModel.selectPattern(it) },
            )

            Spacer(modifier = Modifier.height(8.dp))

            RoundIndicator(
                round = state.round,
                totalRounds = uiSettings.totalRounds.coerceAtMost(uiSettings.pattern.totalRounds),
                paused = state.paused,
                running = state.running,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 中部呼吸区
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.62f),
                contentAlignment = Alignment.Center,
            ) {
                BreathOrb(state = state)

                // 阶段文字覆盖在中央
                Column(
                    modifier = Modifier.wrapContentSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(id = state.phase.labelStringRes()),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AnimatedVisibility(
                        visible = state.running,
                        enter = fadeIn(tween(400)),
                        exit = fadeOut(tween(200)),
                    ) {
                        val currentStepSeconds = state.pattern.steps
                            .firstOrNull { it.kind.toPhaseOrNull() == state.phase }
                            ?.seconds ?: 0
                        Text(
                            text = if (state.paused) "已暂停"
                            else "${currentStepSeconds} 秒 · 第 ${state.round}/${uiSettings.totalRounds.coerceAtMost(uiSettings.pattern.totalRounds)} 轮",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            PhaseTrack(phase = state.phase, pattern = state.pattern)

            Spacer(modifier = Modifier.height(20.dp))

            // 操作按钮区
            ActionRow(
                running = state.running,
                paused = state.paused,
                phase = state.phase,
                onStart = viewModel::start,
                onTogglePause = viewModel::togglePause,
                onStop = viewModel::stop,
            )

            Spacer(modifier = Modifier.height(6.dp))

            val tipRes = when (state.phase) {
                BreathingEngine.Phase.READY -> R.string.tip_ready
                BreathingEngine.Phase.COMPLETE -> R.string.tip_complete
                else -> state.pattern.steps.firstOrNull {
                    it.kind.toPhaseOrNull() == state.phase
                }?.kind?.tipStringRes() ?: R.string.tip_inhale
            }
            TipLine(text = stringResource(id = tipRes))
        }
    }

    SettingsSheet(
        visible = settingsVisible,
        settings = uiSettings,
        patterns = BreathingPatterns.ALL,
        ambients = viewModel.availableAmbients,
        voiceStyles = viewModel.availableVoiceStyles,
        onRoundsChange = viewModel::setTotalRounds,
        onSoundChange = viewModel::setSoundEnabled,
        onMusicChange = viewModel::setMusicEnabled,
        onHapticsChange = viewModel::setHapticsEnabled,
        onKeepScreenOnChange = viewModel::setKeepScreenOn,
        onPatternChange = viewModel::selectPattern,
        onAmbientChange = viewModel::selectAmbient,
        onVoiceStyleChange = viewModel::selectVoiceStyle,
        onDismiss = { settingsVisible = false },
    )
}

@Composable
private fun HeaderRow(onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(id = R.string.intro_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = CircleShape,
        ) {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(id = R.string.cd_settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PatternSelector(
    patterns: List<BreathingPattern>,
    selected: BreathingPattern,
    onSelect: (BreathingPattern) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(50),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            patterns.forEach { p ->
                val isSelected = p.id == selected.id
                Surface(
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(p) },
                ) {
                    Text(
                        text = p.displayName,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RoundIndicator(round: Int, totalRounds: Int, running: Boolean, paused: Boolean) {
    Surface(
        color = if (running) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(50),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.round_label, round, totalRounds),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = when {
                    !running -> "准备中"
                    paused -> "已暂停"
                    else -> "进行中"
                },
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun PhaseTrack(phase: BreathingEngine.Phase, pattern: BreathingPattern) {
    val currentKind = pattern.steps.firstOrNull { it.kind.toPhaseOrNull() == phase }?.kind
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        pattern.steps.forEach { step ->
            val active = currentKind == step.kind
            Surface(
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(50),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(id = step.kind.labelStringRes()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    running: Boolean,
    paused: Boolean,
    phase: BreathingEngine.Phase,
    onStart: () -> Unit,
    onTogglePause: () -> Unit,
    onStop: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!running || phase == BreathingEngine.Phase.COMPLETE) {
            Button(
                onClick = onStart,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(50),
            ) {
                Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.start_training),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            OutlinedButton(
                onClick = {},
                enabled = false,
                modifier = Modifier
                    .height(56.dp)
                    .width(56.dp),
                shape = CircleShape,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Stop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
        } else {
            Button(
                onClick = onTogglePause,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
            ) {
                Text(
                    text = if (paused) stringResource(id = R.string.resume_training)
                    else stringResource(id = R.string.pause_training),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            OutlinedButton(
                onClick = onStop,
                modifier = Modifier
                    .height(56.dp)
                    .width(56.dp),
                shape = CircleShape,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Stop,
                    contentDescription = stringResource(id = R.string.stop_training),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun TipLine(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun PreviewScreen() {
    BreathTrainerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            BreathOrb(
                state = BreathingEngine.State(
                    phase = BreathingEngine.Phase.INHALE,
                    progress = 0.4f,
                    pattern = BreathingPatterns.FOUR_SEVEN_EIGHT,
                ),
            )
        }
    }
}
