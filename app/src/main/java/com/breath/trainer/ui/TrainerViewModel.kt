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
import kotlinx.coroutines.flow.Flow
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
    val themeMode: Int = 0,
)

/** 开关标志位 + 主题模式：合并后避免和 audio / pattern 一起塞进同一个 8-flow combine。 */
private data class TrainerFlags(
    val sound: Boolean,
    val music: Boolean,
    val haptics: Boolean,
    val keepScreenOn: Boolean,
    val themeMode: Int,
)

/** 音频偏好：环境音 + 播报方式。 */
private data class TrainerAudioPrefs(
    val ambient: AmbientSound,
    val voiceStyle: VoiceStyle,
)

/** 节奏 + 轮数：耦合在一起，轮数要 clamp 到 pattern.totalRounds。 */
private data class TrainerPatternRounds(
    val pattern: BreathingPattern,
    val totalRounds: Int,
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

    // Kotlin 2.0 / coroutines 1.9 起，8-flow combine 走的是 vararg 形式
    // (suspend (Array<T>) -> R)，原代码 8 参 lambda 会被推断成 SuspendFunction8，
    // 与目标 SuspendFunction1 不匹配；这里拆成 3 个 2/3/4-flow 子 combine 再合并，
    // 既保留类型安全，又能让 K2 编译器正确推导。
    private val patternRoundsFlow: Flow<TrainerPatternRounds> = combine(
        app.settingsRepository.patternId,
        app.settingsRepository.totalRounds,
    ) { patternId, rounds ->
        val pattern = BreathingPatterns.findById(patternId)
        TrainerPatternRounds(pattern, rounds.coerceIn(1, pattern.totalRounds))
    }

    private val flagsFlow: Flow<TrainerFlags> = combine(
        app.settingsRepository.soundEnabled,
        app.settingsRepository.musicEnabled,
        app.settingsRepository.hapticsEnabled,
        app.settingsRepository.keepScreenOn,
        app.settingsRepository.themeMode,
    ) { sound, music, haptics, keep, theme ->
        TrainerFlags(sound, music, haptics, keep, theme)
    }

    private val audioPrefsFlow: Flow<TrainerAudioPrefs> = combine(
        app.settingsRepository.ambientId,
        app.settingsRepository.voiceStyleId,
    ) { ambientId, voiceStyleId ->
        TrainerAudioPrefs(
            ambient = AmbientSounds.findById(ambientId),
            voiceStyle = VoiceStyle.fromId(voiceStyleId),
        )
    }

    val uiSettings: StateFlow<TrainerUiSettings> = combine(
        patternRoundsFlow,
        flagsFlow,
        audioPrefsFlow,
    ) { pr, flags, audio ->
        TrainerUiSettings(
            pattern = pr.pattern,
            totalRounds = pr.totalRounds,
            soundEnabled = flags.sound,
            musicEnabled = flags.music,
            hapticsEnabled = flags.haptics,
            keepScreenOn = flags.keepScreenOn,
            ambient = audio.ambient,
            voiceStyle = audio.voiceStyle,
            themeMode = flags.themeMode,
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
        try {
            val settings = uiSettings.value
            val pattern = settings.pattern
            engine.setPattern(pattern)
            engine.setTotalRounds(settings.totalRounds.coerceIn(1, pattern.totalRounds))
            // MediaPlayer 初始化失败不能让整进程崩；只是该次训练没背景音而已
            runCatching { app.backgroundMusic.start() }
                .onFailure { Log.w("TrainerViewModel", "backgroundMusic.start failed", it) }
            engine.start()
        } catch (e: Throwable) {
            Log.e("TrainerViewModel", "start() failed", e)
        }
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
        app.backgroundMusic.enabled = value
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
            app.backgroundMusic.ambient = ambient
        }
    }

    fun selectVoiceStyle(style: VoiceStyle) = viewModelScope.launch {
        app.settingsRepository.setVoiceStyleId(style.id)
    }

    fun setThemeMode(value: Int) = viewModelScope.launch {
        app.settingsRepository.setThemeMode(value)
    }

    init {
        // 启动时把持久化的环境音同步到管理器
        viewModelScope.launch {
            uiSettings.collect { s ->
                // ambient 通过属性赋值切换（含热切换逻辑）
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
