package com.breath.trainer.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.breath.trainer.BreathApplication
import com.breath.trainer.audio.AmbientSound
import com.breath.trainer.audio.AmbientSounds
import com.breath.trainer.audio.VoiceGender
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

/** 与 SettingsSheet 滑块 / BreathingEngine.MAX_ROUNDS 保持一致。 */
private const val MAX_ROUNDS: Int = 12

data class TrainerUiSettings(
    val pattern: BreathingPattern = BreathingPatterns.FOUR_SEVEN_EIGHT,
    val totalRounds: Int = BreathingPatterns.FOUR_SEVEN_EIGHT.totalRounds,
    val voicePromptEnabled: Boolean = true,
    val chimePromptEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val ambient: AmbientSound = AmbientSounds.OCEAN,
    val voiceGender: VoiceGender = VoiceGender.FEMALE,
    val themeMode: Int = 0,
)

/** 开关标志位 + 主题模式：合并后避免和 audio / pattern 一起塞进同一个 8-flow combine。 */
private data class TrainerFlags(
    val voicePrompt: Boolean,
    val chimePrompt: Boolean,
    val music: Boolean,
    val keepScreenOn: Boolean,
    val themeMode: Int,
)

/** 音频偏好：环境音 + 发声性别。 */
private data class TrainerAudioPrefs(
    val ambient: AmbientSound,
    val voiceGender: VoiceGender,
)

/** 节奏 + 轮数：耦合在一起，轮数要 clamp 到 MAX_ROUNDS（用户最多 12 轮）。 */
private data class TrainerPatternRounds(
    val pattern: BreathingPattern,
    val totalRounds: Int,
)

class TrainerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BreathApplication

    private val engine = BreathingEngine(app.ttsManager).also { e ->
        // 训练开始（READY 阶段）不再播报"这是哪种训练法"——用户要求取消该提示音。
        // 引擎仍会触发 onTrainStart，但此处不接任何播报。

        e.onPhaseStart = { stepKind, _ ->
            // 颂钵提示 + 女声提示按规范序列播放（钵 → ~220ms → 人声），
            // 两者分别由 chimePromptEnabled（颂钵）/ voicePromptEnabled（人声）独立控制。
            app.ttsManager.playStage(
                gender = uiSettings.value.voiceGender,
                mode = uiSettings.value.pattern.id,
                stepKind = stepKind,
                voiceEnabled = uiSettings.value.voicePromptEnabled,
                bowlEnabled = uiSettings.value.chimePromptEnabled,
            )
        }
        e.onTrainFinished = {
            app.ttsManager.playComplete(
                gender = uiSettings.value.voiceGender,
                voiceEnabled = uiSettings.value.voicePromptEnabled,
                bowlEnabled = uiSettings.value.chimePromptEnabled,
            )
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
        // 轮数上限放宽到 MAX_ROUNDS = 12，不再被节奏 default 的 4/6/5 截断。
        TrainerPatternRounds(pattern, rounds.coerceIn(1, MAX_ROUNDS))
    }

    private val flagsFlow: Flow<TrainerFlags> = combine(
        app.settingsRepository.voicePromptEnabled,
        app.settingsRepository.chimePromptEnabled,
        app.settingsRepository.musicEnabled,
        app.settingsRepository.keepScreenOn,
        app.settingsRepository.themeMode,
    ) { voice, chime, music, keep, theme ->
        TrainerFlags(voice, chime, music, keep, theme)
    }

    private val audioPrefsFlow: Flow<TrainerAudioPrefs> = combine(
        app.settingsRepository.ambientId,
        app.settingsRepository.voiceGenderId,
    ) { ambientId, voiceGenderId ->
        TrainerAudioPrefs(
            ambient = AmbientSounds.findById(ambientId),
            voiceGender = VoiceGender.fromId(voiceGenderId),
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
            voicePromptEnabled = flags.voicePrompt,
            chimePromptEnabled = flags.chimePrompt,
            musicEnabled = flags.music,
            keepScreenOn = flags.keepScreenOn,
            ambient = audio.ambient,
            voiceGender = audio.voiceGender,
            themeMode = flags.themeMode,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TrainerUiSettings(),
    )

    /** 可选的发声性别列表（按 UI 顺序）。 */
    val availableVoiceGenders: List<VoiceGender> = listOf(VoiceGender.FEMALE, VoiceGender.MALE)

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
            engine.setTotalRounds(settings.totalRounds.coerceIn(1, MAX_ROUNDS))
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
        // 不再根据节奏 default 强行下调用户的 totalRounds；用户已选 12 轮就保持 12 轮。
        // 切换节奏时若正在训练，先停掉 TTS / 音乐 / 引擎，回到新节奏的"准备好"起始界面。
        if (engine.state.value.running) {
            engine.stop()
            app.ttsManager.pause()
            app.backgroundMusic.stop()
        }
        engine.setPattern(pattern)
        engine.setTotalRounds(uiSettings.value.totalRounds.coerceIn(1, MAX_ROUNDS))
    }

    fun setTotalRounds(value: Int) = viewModelScope.launch {
        app.settingsRepository.setTotalRounds(value)
        engine.setTotalRounds(value)
    }

    fun setVoicePromptEnabled(value: Boolean) = viewModelScope.launch {
        app.settingsRepository.setVoicePromptEnabled(value)
    }

    fun setChimePromptEnabled(value: Boolean) = viewModelScope.launch {
        app.settingsRepository.setChimePromptEnabled(value)
    }

    fun setMusicEnabled(value: Boolean) = viewModelScope.launch {
        app.settingsRepository.setMusicEnabled(value)
        app.backgroundMusic.enabled = value
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

    fun selectVoiceGender(gender: VoiceGender) = viewModelScope.launch {
        app.settingsRepository.setVoiceGenderId(gender.id)
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
