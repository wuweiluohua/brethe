package com.breath.trainer.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.breath.trainer.R
import com.breath.trainer.breathing.pattern.BreathingStep

/**
 * 管理训练期间的提示音（按「呼吸训练小程序声音制作参数 V1.0」规范）：
 *
 * 1) 颂钵提示（阶段边界）：每个呼吸阶段开始播放 [R.raw.bowl_stage_01]；
 *    训练结束播放 [R.raw.bowl_complete_01]。由 SoundPool 低延迟播放，不与人声重叠。
 *
 * 2) 人声提示（预录音频）：按「节奏 id（478 / 426 / box）× 阶段（inhale / hold / exhale）
 *    × 性别（f / m）」选择对应 voice_*.wav，由 edge-tts 神经网络语音预先生成，
 *    避免各设备自带 TTS 声音生硬或识别能力弱导致读错字。运行时不再调用系统 TextToSpeech。
 *
 * 播放序列（规范 6）：颂钵 → 150~300ms 留白 → 人声；本类在 [playStage] / [playComplete]
 * 内部用主线程 Handler 延迟 ~220ms / ~400ms 触发人声，两个提示由调用方传入的开关独立控制。
 */
class TtsManager(private val appContext: Context) {

    private val soundPool: SoundPool by lazy {
        val attrs = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_GAME)
            .build()
        SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attrs)
            .build()
    }

    private val tag = "TtsManager"

    // 颂钵
    private var bowlStageId: Int = 0
    private var bowlCompleteId: Int = 0

    // 人声提示：key = "${gender}_${mode}_${step}" 或 "${gender}_complete"
    private val voiceIds = mutableMapOf<String, Int>()

    private var soundsLoaded = false

    private val handler = Handler(Looper.getMainLooper())
    private var pendingVoiceRunnable: Runnable? = null

    // 用于暂停时停止正在播放的提示
    private var lastVoiceStreamId: Int = 0
    private var lastBowlStreamId: Int = 0

    /** 初始化音效，异步加载完成后才会正常播放。 */
    fun initialize(onReady: ((Boolean) -> Unit)? = null) {
        // 颂钵
        bowlStageId = soundPool.load(appContext, R.raw.bowl_stage_01, 1)
        bowlCompleteId = soundPool.load(appContext, R.raw.bowl_complete_01, 1)

        // 人声：gender(f/m) × mode(478/426/box) × step(inhale/hold/exhale)
        voiceIds["f_478_inhale"] = soundPool.load(appContext, R.raw.voice_f_inhale_478, 1)
        voiceIds["f_478_hold"] = soundPool.load(appContext, R.raw.voice_f_hold_478, 1)
        voiceIds["f_478_exhale"] = soundPool.load(appContext, R.raw.voice_f_exhale_478, 1)
        voiceIds["f_426_inhale"] = soundPool.load(appContext, R.raw.voice_f_inhale_426, 1)
        voiceIds["f_426_hold"] = soundPool.load(appContext, R.raw.voice_f_hold_426, 1)
        voiceIds["f_426_exhale"] = soundPool.load(appContext, R.raw.voice_f_exhale_426, 1)
        voiceIds["f_box_inhale"] = soundPool.load(appContext, R.raw.voice_f_inhale_box, 1)
        voiceIds["f_box_hold"] = soundPool.load(appContext, R.raw.voice_f_hold_box, 1)
        voiceIds["f_box_exhale"] = soundPool.load(appContext, R.raw.voice_f_exhale_box, 1)

        voiceIds["m_478_inhale"] = soundPool.load(appContext, R.raw.voice_m_inhale_478, 1)
        voiceIds["m_478_hold"] = soundPool.load(appContext, R.raw.voice_m_hold_478, 1)
        voiceIds["m_478_exhale"] = soundPool.load(appContext, R.raw.voice_m_exhale_478, 1)
        voiceIds["m_426_inhale"] = soundPool.load(appContext, R.raw.voice_m_inhale_426, 1)
        voiceIds["m_426_hold"] = soundPool.load(appContext, R.raw.voice_m_hold_426, 1)
        voiceIds["m_426_exhale"] = soundPool.load(appContext, R.raw.voice_m_exhale_426, 1)
        voiceIds["m_box_inhale"] = soundPool.load(appContext, R.raw.voice_m_inhale_box, 1)
        voiceIds["m_box_hold"] = soundPool.load(appContext, R.raw.voice_m_hold_box, 1)
        voiceIds["m_box_exhale"] = soundPool.load(appContext, R.raw.voice_m_exhale_box, 1)

        // 结束提示
        voiceIds["f_complete"] = soundPool.load(appContext, R.raw.voice_f_complete_01, 1)
        voiceIds["m_complete"] = soundPool.load(appContext, R.raw.voice_m_complete_01, 1)

        soundPool.setOnLoadCompleteListener { _, _, _ -> soundsLoaded = true }
        // 资源较小，加载通常在数百毫秒内完成；SoundPool 加载完成后即可播放。
        onReady?.invoke(true)
    }

    /**
     * 播放某一阶段的提示：先播颂钵（若 [bowlEnabled]），约 220ms 后再播人声（若 [voiceEnabled]）。
     * [mode] 为节奏 id（"478" / "426" / "box"），[stepKind] 为当前阶段。
     */
    fun playStage(
        gender: VoiceGender,
        mode: String,
        stepKind: BreathingStep.StepKind,
        voiceEnabled: Boolean,
        bowlEnabled: Boolean,
    ) {
        if (!soundsLoaded) return

        if (bowlEnabled) {
            if (lastBowlStreamId != 0) soundPool.stop(lastBowlStreamId)
            lastBowlStreamId = soundPool.play(bowlStageId, 0.9f, 0.9f, 1, 0, 1.0f)
        }

        if (voiceEnabled) {
            val id = voiceKeyId(gender, mode, stepKind) ?: return
            scheduleVoice(id, 220L)
        }
    }

    /** 训练结束提示：先播结束颂钵（若 [bowlEnabled]），约 400ms 后再播结束人声（若 [voiceEnabled]）。 */
    fun playComplete(gender: VoiceGender, voiceEnabled: Boolean, bowlEnabled: Boolean) {
        if (!soundsLoaded) return

        if (bowlEnabled) {
            if (lastBowlStreamId != 0) soundPool.stop(lastBowlStreamId)
            lastBowlStreamId = soundPool.play(bowlCompleteId, 0.9f, 0.9f, 1, 0, 1.0f)
        }

        if (voiceEnabled) {
            val id = voiceIds[if (gender == VoiceGender.FEMALE) "f_complete" else "m_complete"] ?: return
            scheduleVoice(id, 400L)
        }
    }

    private fun scheduleVoice(sampleId: Int, delayMs: Long) {
        pendingVoiceRunnable?.let { handler.removeCallbacks(it) }
        val runnable = Runnable {
            if (lastVoiceStreamId != 0) soundPool.stop(lastVoiceStreamId)
            lastVoiceStreamId = soundPool.play(sampleId, 0.9f, 0.9f, 1, 0, 1.0f)
        }
        pendingVoiceRunnable = runnable
        handler.postDelayed(runnable, delayMs)
    }

    private fun voiceKeyId(gender: VoiceGender, mode: String, stepKind: BreathingStep.StepKind): Int? {
        val g = if (gender == VoiceGender.FEMALE) "f" else "m"
        val step = when (stepKind) {
            BreathingStep.StepKind.INHALE -> "inhale"
            BreathingStep.StepKind.HOLD_AFTER_INHALE -> "hold"
            BreathingStep.StepKind.EXHALE -> "exhale"
            // 盒式第二段屏息与第一段屏息共用同一段引导词
            BreathingStep.StepKind.HOLD_AFTER_EXHALE -> "hold"
        }
        val m = if (mode in setOf("478", "426", "box")) mode else "478"
        return voiceIds["${g}_${m}_${step}"]
    }

    /** 暂停：取消待播人声并停止正在播放的颂钵/人声。 */
    fun pause() {
        pendingVoiceRunnable?.let { handler.removeCallbacks(it) }
        pendingVoiceRunnable = null
        if (lastVoiceStreamId != 0) soundPool.stop(lastVoiceStreamId)
        if (lastBowlStreamId != 0) soundPool.stop(lastBowlStreamId)
    }

    /** 完全释放资源。 */
    fun release() {
        try {
            soundPool.release()
        } catch (e: Exception) {
            Log.w(tag, "Error releasing soundPool: ${e.message}")
        }
    }
}
