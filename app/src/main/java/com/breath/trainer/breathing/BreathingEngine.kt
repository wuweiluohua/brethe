package com.breath.trainer.breathing

import android.util.Log
import com.breath.trainer.audio.TtsManager
import com.breath.trainer.breathing.pattern.BreathingPattern
import com.breath.trainer.breathing.pattern.BreathingPatterns
import com.breath.trainer.breathing.pattern.BreathingStep
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 多节奏呼吸训练引擎。
 *
 * 通过 [BreathingPattern] 抽象一种呼吸节奏的具体步骤。引擎按顺序逐个执行
 * 每个 step，并在每一步内按 [TICK_MS] 推进当前进度 (0..1)。
 *
 * 支持的 step:
 *  - [BreathingStep.StepKind.INHALE]
 *  - [BreathingStep.StepKind.HOLD_AFTER_INHALE]
 *  - [BreathingStep.StepKind.EXHALE]
 *  - [BreathingStep.StepKind.HOLD_AFTER_EXHALE]
 *
 * 引擎不直接依赖 Android 资源，便于复用与测试。
 */
class BreathingEngine(
    @Suppress("unused") private val ttsManager: TtsManager? = null,
) {
    enum class Phase(val secondsDefault: Int = 0) {
        READY(secondsDefault = 3),
        INHALE,
        HOLD_AFTER_INHALE,
        EXHALE,
        HOLD_AFTER_EXHALE,
        COMPLETE,
    }

    data class State(
        val phase: Phase,
        /** 当前阶段归一化进度 (0..1)。 */
        val progress: Float = 0f,
        /** 当前轮（从 1 开始）。 */
        val round: Int = 1,
        /** 总轮数。 */
        val totalRounds: Int = 4,
        /** 当前节奏。 */
        val pattern: BreathingPattern = BreathingPatterns.FOUR_SEVEN_EIGHT,
        val running: Boolean = false,
        val paused: Boolean = false,
    ) {
        /** 当前阶段剩余秒数（向上取整）。 */
        val remainingSeconds: Int
            get() = ((pattern.maxStepSeconds - (pattern.maxStepSeconds * progress)).toInt().coerceAtLeast(0) + 1)
    }

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default +
            CoroutineExceptionHandler { _, e ->
                // 兜底：onPhaseStart / onTick 等用户回调里如果抛异常，
                // 不要让进程直接闪退；记日志后让训练自然停止。
                Log.e("BreathingEngine", "Engine coroutine crashed, stopping training", e)
                _state.update { it.copy(running = false, paused = false) }
            }
    )
    private var job: Job? = null

    private val _state = MutableStateFlow(
        State(
            phase = Phase.READY,
            totalRounds = BreathingPatterns.FOUR_SEVEN_EIGHT.totalRounds,
            pattern = BreathingPatterns.FOUR_SEVEN_EIGHT,
        )
    )
    val state: StateFlow<State> = _state.asStateFlow()

    /** 当前选中的呼吸节奏。 */
    private var currentPattern: BreathingPattern = BreathingPatterns.FOUR_SEVEN_EIGHT

    /** 配置变更的回调（让 UI / ViewModel 触发语音与震动）。 */
    var onPhaseStart: ((BreathingStep.StepKind, Int) -> Unit)? = null
    /** 训练正式开始前的热身播报（READY 阶段，仅触发一次）：节奏名称 + "准备开始"。 */
    var onTrainStart: (() -> Unit)? = null
    var onRoundChanged: ((Int) -> Unit)? = null
    var onTrainFinished: (() -> Unit)? = null
    var onTick: ((Phase, Float) -> Unit)? = null

    /** 切换呼吸节奏。允许在 idle 状态替换，或在训练中即时热替换。 */
    fun setPattern(pattern: BreathingPattern) {
        currentPattern = pattern
        // 保留用户的 totalRounds（最高 MAX_ROUNDS = 12），不再用节奏内嵌的
        // totalRounds（4/6/5）覆盖——用户已经选好 12 轮，切节奏时不应该被悄悄改回 4/5/6。
        val safeRounds = _state.value.totalRounds.coerceIn(1, MAX_ROUNDS)
        _state.update {
            it.copy(
                pattern = pattern,
                totalRounds = safeRounds,
                round = it.round.coerceIn(1, safeRounds),
            )
        }
    }

    /** 单轮覆盖默认轮数（仅在 idle 状态生效）。 */
    fun setTotalRounds(rounds: Int) {
        _state.update {
            // 上限放宽到 12（SettingsRepository / SettingsSheet 滑块一致），
            // 不再被节奏内嵌的 totalRounds（4/6/5）锁死，让用户每天最多能练 12 轮。
            val safe = rounds.coerceIn(1, MAX_ROUNDS)
            it.copy(totalRounds = safe, round = it.round.coerceAtMost(safe).coerceAtLeast(1))
        }
    }

    fun start(initialRound: Int = 1) {
        if (_state.value.running) return
        _state.update {
            it.copy(
                phase = Phase.READY,
                progress = 0f,
                running = true,
                paused = false,
                round = initialRound.coerceIn(1, it.totalRounds),
                totalRounds = it.totalRounds.coerceIn(1, MAX_ROUNDS),
                pattern = currentPattern,
            )
        }
        job = scope.launch {
            runTraining()
        }
    }

    fun pauseToggle(): Boolean {
        val s = _state.value
        if (!s.running) return false
        val nowPaused = !s.paused
        _state.update { it.copy(paused = nowPaused) }
        if (nowPaused) {
            ttsManager?.pause()
        }
        return nowPaused
    }

    fun stop() {
        job?.cancel()
        job = null
        ttsManager?.pause()
        _state.update {
            it.copy(
                phase = Phase.READY,
                progress = 0f,
                running = false,
                paused = false,
            )
        }
    }

    fun release() {
        stop()
        scope.cancel()
    }

    private suspend fun runTraining() {
        try {
            // 准备阶段 3s：先做一次热身播报（节奏名称 + "准备开始"），仅此一次；
            // 真正的每一步由下方循环里的 onPhaseStart 触发，首轮首吸也会正常播报"吸气"。
            _state.update { it.copy(phase = Phase.READY, progress = 0f) }
            safeInvokeOnTrainStart()
            if (!awaitTick(Phase.READY, Phase.READY.secondsDefault)) return

            while (true) {
                val stateNow = _state.value
                if (!stateNow.running) return
                val currentRound = stateNow.round
                if (currentRound > stateNow.totalRounds) break

                safeInvokeOnRoundChanged(currentRound)

                // 顺序执行节奏中的每一步
                for ((index, step) in currentPattern.steps.withIndex()) {
                    val phase = step.kind.toPhase()
                    _state.update { it.copy(phase = phase, progress = 0f) }
                    safeInvokeOnPhaseStart(step.kind, currentRound)
                    if (!awaitTick(phase, step.seconds)) return

                    if (_state.value.round > _state.value.totalRounds) return
                    if (index == currentPattern.steps.lastIndex) {
                        val nextRound = currentRound + 1
                        // 先更新 round，再判断是否超过总轮数；
                        // 旧写法 break 在 update 之前，会让 round 永远停在 totalRounds 触发 while 死循环。
                        _state.update { it.copy(round = nextRound) }
                        if (nextRound > stateNow.totalRounds) break
                    }
                }
                if (_state.value.round > _state.value.totalRounds) break
            }

            _state.update {
                it.copy(phase = Phase.COMPLETE, progress = 1f, running = false, paused = false)
            }
            safeInvokeOnTrainFinished()
        } catch (ce: kotlinx.coroutines.CancellationException) {
            // stop() 会取消 job，把 CancellationException 透传上去
            throw ce
        } catch (e: Throwable) {
            Log.e("BreathingEngine", "runTraining failed", e)
            _state.update { it.copy(running = false, paused = false, phase = Phase.READY) }
        }
    }

    /** 用户在 ViewModel 里挂的回调里若抛异常，会把整个协程拖死；这里统一包一层。 */
    private inline fun safeInvokeOnPhaseStart(kind: BreathingStep.StepKind, round: Int) {
        try {
            onPhaseStart?.invoke(kind, round)
        } catch (e: Throwable) {
            Log.e("BreathingEngine", "onPhaseStart callback threw", e)
        }
    }

    private inline fun safeInvokeOnTrainStart() {
        try {
            onTrainStart?.invoke()
        } catch (e: Throwable) {
            Log.e("BreathingEngine", "onTrainStart callback threw", e)
        }
    }

    private inline fun safeInvokeOnRoundChanged(round: Int) {
        try {
            onRoundChanged?.invoke(round)
        } catch (e: Throwable) {
            Log.e("BreathingEngine", "onRoundChanged callback threw", e)
        }
    }

    private inline fun safeInvokeOnTrainFinished() {
        try {
            onTrainFinished?.invoke()
        } catch (e: Throwable) {
            Log.e("BreathingEngine", "onTrainFinished callback threw", e)
        }
    }

    private inline fun safeInvokeOnTick(phase: Phase, progress: Float) {
        try {
            onTick?.invoke(phase, progress)
        } catch (_: Throwable) {
            // tick 是高频调用，吞掉异常只记一行调试
        }
    }

    private fun BreathingStep.StepKind.toPhase(): Phase = when (this) {
        BreathingStep.StepKind.INHALE -> Phase.INHALE
        BreathingStep.StepKind.HOLD_AFTER_INHALE -> Phase.HOLD_AFTER_INHALE
        BreathingStep.StepKind.EXHALE -> Phase.EXHALE
        BreathingStep.StepKind.HOLD_AFTER_EXHALE -> Phase.HOLD_AFTER_EXHALE
    }

    /** 在该阶段内推进进度；返回 true 表示完成，false 表示被取消或结束。 */
    private suspend fun awaitTick(
        phase: Phase,
        seconds: Int,
    ): Boolean {
        if (seconds <= 0) return true
        val totalSteps = (seconds * 1000) / TICK_MS
        var step = 0
        while (scope.isActive) {
            val s = _state.value
            if (!s.running) return false
            if (s.paused) {
                delay(50)
                continue
            }
            step++
            val progress = (step.toFloat() / totalSteps).coerceIn(0f, 1f)
            _state.update { it.copy(phase = phase, progress = progress) }
            safeInvokeOnTick(phase, progress)
            if (step >= totalSteps) return true
            delay(TICK_MS)
        }
        return false
    }

    private inline fun updatePhase(
        phase: Phase,
        seconds: Int,
        crossinline block: (Phase, Int) -> Unit,
    ) {
        _state.update { it.copy(phase = phase, progress = 0f) }
        block(phase, seconds)
    }

    companion object {
        const val TICK_MS = 50L

        /** 全局允许的最大轮数；与 SettingsSheet / SettingsRepository 保持一致。 */
        const val MAX_ROUNDS: Int = 12
    }
}
