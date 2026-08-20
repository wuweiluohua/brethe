package com.breath.trainer.breathing.pattern

/**
 * 呼吸节奏中的一步。
 *
 * `kind` 用于驱动 UI 动画 / 颜色 / 提示音 / 语音。
 * `seconds` 控制在引擎中停留多久。
 */
data class BreathingStep(
    val kind: StepKind,
    val seconds: Int,
) {
    enum class StepKind {
        /** 吸气（4-7-8 / 4-2-6 / Box 通用） */
        INHALE,
        /** 吸气后屏息 */
        HOLD_AFTER_INHALE,
        /** 呼气 */
        EXHALE,
        /** 呼气后屏息（仅盒式呼吸会用到） */
        HOLD_AFTER_EXHALE,
    }
}

/** step kind -> engine phase（包含 READY/COMPLETE 时返回 null）。 */
fun BreathingStep.StepKind.toPhaseOrNull(): com.breath.trainer.breathing.BreathingEngine.Phase? =
    when (this) {
        BreathingStep.StepKind.INHALE -> com.breath.trainer.breathing.BreathingEngine.Phase.INHALE
        BreathingStep.StepKind.HOLD_AFTER_INHALE -> com.breath.trainer.breathing.BreathingEngine.Phase.HOLD_AFTER_INHALE
        BreathingStep.StepKind.EXHALE -> com.breath.trainer.breathing.BreathingEngine.Phase.EXHALE
        BreathingStep.StepKind.HOLD_AFTER_EXHALE -> com.breath.trainer.breathing.BreathingEngine.Phase.HOLD_AFTER_EXHALE
    }

/**
 * 描述一种完整的呼吸节奏。
 *
 * 比如 4-7-8 节奏由 INHALE(4) → HOLD_AFTER_INHALE(7) → EXHALE(8) 三步组成；
 * 盒式节奏 4-4-4-4 由 INHALE(4) → HOLD_AFTER_INHALE(4) → EXHALE(4) → HOLD_AFTER_EXHALE(4) 四步组成。
 *
 * 引擎按顺序逐个执行 steps，每完成一轮即计数 +1，直到 [totalRounds]。
 */
data class BreathingPattern(
    val id: String,
    val displayName: String,
    val description: String,
    val totalRounds: Int,
    val steps: List<BreathingStep>,
) {
    /** 单轮时长（秒），用于 UI 展示。 */
    val roundSeconds: Int get() = steps.sumOf { it.seconds }

    /** 所有步骤最大秒数，用于动画时长自适应。 */
    val maxStepSeconds: Int get() = steps.maxOfOrNull { it.seconds } ?: 0
}
