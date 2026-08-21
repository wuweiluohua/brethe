package com.breath.trainer.audio

/**
 * 提示音的发声性别。
 *
 * 按「呼吸训练小程序声音制作参数 V1.0」规范：男女声使用完全相同的引导词，
 * 仅声音性别与音色不同。这里由用户在设置中切换，App 运行时据此选择
 * [TtsManager] 中对应的预录音频（voice_f_* / voice_m_*）。
 *
 * 通过 [SettingsRepository] 持久化到 DataStore。
 */
enum class VoiceGender(val id: String) {
    FEMALE(id = "female"),
    MALE(id = "male");

    companion object {
        fun fromId(id: String?): VoiceGender = if (id == MALE.id) MALE else FEMALE
    }
}
