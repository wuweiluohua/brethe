package com.breath.trainer.breathing.pattern

import com.breath.trainer.breathing.pattern.BreathingStep.StepKind

/**
 * 内置的呼吸节奏。
 *
 * - [FOUR_SEVEN_EIGHT]：4-7-8 呼吸法，延长呼气帮助放松、入睡。
 * - [FOUR_TWO_SIX]：4-2-6 节奏，平衡吸气 / 呼气，适合日常减压。
 * - [BOX]：4-4-4-4 盒式呼吸，节奏对称，常用于专注、缓解焦虑。
 */
object BreathingPatterns {

    /** 4-7-8 放松呼吸法（Andrew Weil）。吸气 4s → 屏息 7s → 呼气 8s。 */
    val FOUR_SEVEN_EIGHT = BreathingPattern(
        id = "478",
        displayName = "4-7-8 放松",
        description = "通过延长呼气激活副交感神经，适合睡前与感到焦虑时使用。",
        totalRounds = 4,
        steps = listOf(
            BreathingStep(StepKind.INHALE, seconds = 4),
            BreathingStep(StepKind.HOLD_AFTER_INHALE, seconds = 7),
            BreathingStep(StepKind.EXHALE, seconds = 8),
        ),
    )

    /** 4-2-6 平衡呼吸法。吸气 4s → 屏息 2s → 呼气 6s。 */
    val FOUR_TWO_SIX = BreathingPattern(
        id = "426",
        displayName = "4-2-6 平衡",
        description = "节奏柔和、不屏息太久，适合刚开始练习或工作间隙使用。",
        totalRounds = 6,
        steps = listOf(
            BreathingStep(StepKind.INHALE, seconds = 4),
            BreathingStep(StepKind.HOLD_AFTER_INHALE, seconds = 2),
            BreathingStep(StepKind.EXHALE, seconds = 6),
        ),
    )

    /** 4-4-4-4 盒式呼吸法（Box / Square Breathing）。每段 4s 共四段。 */
    val BOX = BreathingPattern(
        id = "box",
        displayName = "盒式 4-4-4-4",
        description = "四段相等的节奏，常用于专注、演讲前或军警减压训练。",
        totalRounds = 5,
        steps = listOf(
            BreathingStep(StepKind.INHALE, seconds = 4),
            BreathingStep(StepKind.HOLD_AFTER_INHALE, seconds = 4),
            BreathingStep(StepKind.EXHALE, seconds = 4),
            BreathingStep(StepKind.HOLD_AFTER_EXHALE, seconds = 4),
        ),
    )

    val ALL: List<BreathingPattern> = listOf(FOUR_SEVEN_EIGHT, FOUR_TWO_SIX, BOX)

    fun findById(id: String?): BreathingPattern = ALL.firstOrNull { it.id == id } ?: FOUR_SEVEN_EIGHT
}
