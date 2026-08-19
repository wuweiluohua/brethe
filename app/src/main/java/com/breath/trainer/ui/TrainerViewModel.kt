package com.breath.trainer.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.breath.trainer.BreathApplication
import com.breath.trainer.R
import com.breath.trainer.audio.AmbientSound
import com.breath.trainer.audio.AmbientSounds
import com.breath.trainer.audio.VoiceStyle
import com.breath.trainer.breathing.BreathingEngine
import com.breath.trainer.breathing.pattern.BreathingPattern
import com.breath.trainer.breathing.pattern.BreathingPatterns
import com.breath.trainer.breathing.pattern.BreathingStep
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TrainerUiSettings(
    val pattern: BreathingPattern = BreathingPatterns.FOUR_SEVEN_EIGHT,
    val totalRounds: Int = BreathingPatterns.FOUR_SEVEN_EIGHT.totalRounds,
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val ambient: AmbientSound = AmbientSounds.CALM,
    val voiceStyle: VoiceStyle = VoiceStyle.GENTLE_LONG,
)

class TrainerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BreathApplication

    private val engine = BreathingEngine(app.ttsManager).also { e ->
        e.onPhaseStart = { stepKind, round ->
            // 触感反馈（独立于声音开关）
            when (stepKind) {
                BreathingStep.StepKind.INHALE,
                BreathingStep.StepKind.EXHALE -> {
                    if (uiSettings.value.hapticsEnabled) app.haptics.pulseSoft()
                }
                else -> Unit
            }

            if (uiSettings.value.soundEnabled) {
                // 训练正式开始后第一次播报"准备"，之后根据 voiceStyle 选择单词或长句
                val isFirstStep = round == 1 && stepKind == BreathingStep.StepKind.INHALE
                val startRes = when (uiSettings.value.pattern.id) {
                    "426" -> R.string.tts_start_426
                    "box" -> R.string.tts_start_box
                    else -> R.string.tts_start_478
                }
                val stepShortRes = when (stepKind) {
                    BreathingStep.StepKind.INHALE -> R.string.tts_inhale
                    BreathingStep.StepKind.HOLD_AFTER_INHALE -> R.string.tts_hold_inhale
                    BreathingStep.StepKind.EXHALE -> R.string.tts_exhale
                    BreathingStep.StepKind.HOLD_AFTER_EXHALE -> R.string.tts_hold_exhale
                }
                val stepLongRes = when (stepKind) {
                    BreathingStep.StepKind.INHALE -> R.string.tts_phrase_inhale
                    BreathingStep.StepKind.HOLD_AFTER_INHALE -> R.string.tts_phrase_hold_inhale
                    BreathingStep.StepKind.EXHALE -> R.string.tts_phrase_exhale
                    BreathingStep.StepKind.HOLD_AFTER_EXHALE -> R.string.tts_phrase_hold_exhale
                }
                val phrase = when {
                    isFirstStep -> app.getString(startRes)
                    uiSettings.value.voiceStyle == VoiceStyle.SHORT -> app.getString(stepShortRes)
                    else -> app.getString(stepLongRes)
                }
                // 长句播报用 flush，把上一句截断以跟上节奏；单词模式追加到队列
                app.ttsManager.speakPhase(
                    phrase,
                    flush = !isFirstStep && uiSettings.value.voiceStyle == VoiceStyle.GENTLE_LONG,
                )

                when (stepKind) {
                    BreathingStep.StepKind.INHALE -> app.ttsManager.playInhaleChime()
                    BreathingStep.StepKind.EXHALE -> app.ttsManager.playExhaleChime()
                    BreathingStep.StepKind.HOLD_AFTER_EXHALE -> app.ttsManager.playHoldChime(variant = 1)
                    BreathingStep.StepKind.HOLD_AFTER_INHALE -> app.ttsManager.playHoldChime(variant = 0)
                }
            }

            // 用 round 简单回调（保留 hook）
            @Suppress("UNUSED_PARAMETER")
            val r = round
        }
        e.onTrainFinished = {
            if (uiSettings.value.soundEnabled) {
                app.ttsManager.speakPhase(app.getString(R.string.tts_finish))
            }
        }
    }

    val state: StateFlow<BreathingEngine.State> = engine.state

    val uiSettings: StateFlow<TrainerUiSettings> = combine(
        app.settingsRepository.patternId,
        app.settingsRepository.totalRounds,
        app.settingsRepository.soundEnabled,
        app.settingsRepository.musicEnabled,
        app.settingsRepository.hapticsEnabled,
        app.settingsRepository.keepScreenOn,
        app.settingsRepository.ambientId,
        app.settingsRepository.voiceStyleId,
    ) { patternId, rounds, sound, music, haptics, keep, ambientId, voiceStyleId ->
        val pattern = BreathingPatterns.findById(patternId)
        val ambient = AmbientSounds.findById(ambientId)
        TrainerUiSettings(
            pattern = pattern,
            totalRounds = rounds.coerceIn(1, pattern.totalRounds),
            soundEnabled = sound,
            musicEnabled = music,
            hapticsEnabled = haptics,
            keepScreenOn = keep,
            ambient = ambient,
            voiceStyle = VoiceStyle.fromId(voiceStyleId),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TrainerUiSettings(),
    )

    /** 可选的播报方式列表（按 UI 顺序）。 */
    val availableVoiceStyles: List<VoiceStyle> = listOf(VoiceStyle.SHORT, VoiceStyle.GENTLE_LONG)

    /** 当前可用的所有节奏。 */
    val availablePatterns: List<BreathingPattern> = BreathingPatterns.ALL

    /** 当前可用的所有环境音。 */
    val availableAmbients: List<AmbientSound> = AmbientSounds.ALL

    /** 启动训练 */
    fun start() {
        val settings = uiSettings.value
        val pattern = settings.pattern
        engine.setPattern(pattern)
        engine.setTotalRounds(settings.totalRounds.coerceIn(1, pattern.totalRounds))
        app.backgroundMusic.start()
        engine.start()
    }

    fun togglePause(): Boolean = engine.pauseToggle()

    fun stop() {
        engine.stop()
        app.backgroundMusic.stop()
    }

    fun selectPattern(pattern: BreathingPattern) = viewModelScope.launch {
        app.settingsRepository.setPatternId(pattern.id)
        val current = uiSettings.value
        if (current.totalRounds > pattern.totalRounds) {
            app.settingsRepository.setTotalRounds(pattern.totalRounds)
        }
        engine.setPattern(pattern)
        engine.setTotalRounds(uiSettings.value.totalRounds.coerceIn(1, pattern.totalRounds))
    }

    fun setTotalRounds(value: Int) = viewModelScope.launch {
        app.settingsRepository.setTotalRounds(value)
        engine.setTotalRounds(value)
    }

    fun setSoundEnabled(value: Boolean) = viewModelScope.launch {
        app.settingsRepository.setSoundEnabled(value)
    }

    fun setMusicEnabled(value: Boolean) = viewModelScope.launch {
        app.settingsRepository.setMusicEnabled(value)
        app.backgroundMusic.setEnabled(value)
    }

    fun setHapticsEnabled(value: Boolean) = viewModelScope.launch {
        app.settingsRepository.setHapticsEnabled(value)
        app.haptics.enabled = value
    }

    fun setKeepScreenOn(value: Boolean) = viewModelScope.launch {
        app.settingsRepository.setKeepScreenOn(value)
    }

    fun selectAmbient(ambient: AmbientSound) = viewModelScope.launch {
        app.settingsRepository.setAmbientId(ambient.id)
        if (uiSettings.value.musicEnabled) {
            app.backgroundMusic.setAmbient(ambient)
        }
    }

    fun selectVoiceStyle(style: VoiceStyle) = viewModelScope.launch {
        app.settingsRepository.setVoiceStyleId(style.id)
    }

    init {
        // 启动时把持久化的环境音同步到管理器
        viewModelScope.launch {
            uiSettings.collect { s ->
                app.backgroundMusic.ambient = s.ambient
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        engine.release()
        Log.i("TrainerViewModel", "onCleared")
    }
}
