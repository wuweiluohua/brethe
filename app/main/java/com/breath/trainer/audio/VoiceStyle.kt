package com.breath.trainer.audio

/**
 * TTS 阶段播报的风格。
 *
 * - [SHORT]：单个动词（"吸气" / "屏住" / "呼气" / "停顿"），节奏紧凑。
 * - [GENTLE_LONG]：整句温柔引导（"慢慢地吸气，让气息充满腹部" 等），更放松。
 *
 * 通过 [SettingsRepository] 持久化到 DataStore。
 */
enum class VoiceStyle(val id: String) {
    SHORT(id = "short"),
    GENTLE_LONG(id = "long");

    companion object {
        fun fromId(id: String?): VoiceStyle = when (id) {
            SHORT.id -> SHORT
            else -> GENTLE_LONG
        }
    }
}
