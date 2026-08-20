package com.breath.trainer.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import com.breath.trainer.R
import java.util.Locale

/**
 * 管理训练期间的提示音：
 * 1) 优先使用系统 [TextToSpeech] 引擎的"温柔女声"合成语音。
 * 2) 如果 TTS 引擎未就绪/语言不支持，则降级播放本地柔和钟音。
 *
 * 必须配合 AndroidManifest 中的 TTS_SERVICE 查询才能在 Android 11+ 找到引擎。
 */
class TtsManager(private val appContext: Context) {

    private var tts: TextToSpeech? = null
    private var ttsReady: Boolean = false
    private val tag = "TtsManager"

    private val soundPool: SoundPool by lazy {
        val attrs = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_GAME)
            .build()
        SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(attrs)
            .build()
    }

    private var chimeInhaleId: Int = 0
    private var chimeExhaleId: Int = 0
    private var chimePhaseId: Int = 0
    private var soundsLoaded = false

    /** 初始化 TTS 与音效，异步完成后才会正常播报。 */
    fun initialize(onReady: ((Boolean) -> Unit)? = null) {
        // 预加载所有音效
        chimeInhaleId = soundPool.load(appContext, R.raw.chime_breath_in, 1)
        chimeExhaleId = soundPool.load(appContext, R.raw.chime_breath_out, 1)
        chimePhaseId = soundPool.load(appContext, R.raw.chime_phase, 1)
        soundPool.setOnLoadCompleteListener { _, _, _ -> soundsLoaded = true }

        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                configureTts(onReady)
            } else {
                Log.w(tag, "TTS engine init failed: status=$status")
                ttsReady = false
                onReady?.invoke(false)
            }
        }
    }

    private fun configureTts(onReady: ((Boolean) -> Unit)?) {
        val engine = tts ?: return
        // 尝试以中文为默认语言；如不支持再回退到英文
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            listOf(Locale.SIMPLIFIED_CHINESE, Locale.CHINESE, Locale.US)
                .firstOrNull { engine.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE }
                ?: Locale.getDefault()
        } else {
            Locale.getDefault()
        }

        engine.language = locale
        engine.setPitch(1.05f)        // 略升调，更柔和亲切
        // 旧值 0.48 太慢：长句"慢慢地吸气，让气息充满腹部"用 0.48x 念完接近 5 秒，
        // 而 INHALE 阶段只有 4 秒，下一阶段 start() 时会把上一句截断，听起来像没有语音。
        // 0.85 是兼顾"温柔不催促"和"能完整播完"的经验值。
        engine.setSpeechRate(0.85f)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // 选择女声音色：很多系统会命名为 "female" / "Female"
            try {
                val femaleVoice = engine.voices?.firstOrNull {
                    it.locale == locale &&
                            (it.name.contains("female", ignoreCase = true) ||
                                    it.name.contains("xiaoxiao", ignoreCase = true) ||
                                    it.name.contains("yaoyao", ignoreCase = true))
                }
                if (femaleVoice != null) {
                    engine.voice = femaleVoice
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed to pick a female voice: ${e.message}")
            }
        }

        ttsReady = true
        onReady?.invoke(true)
    }

    /** 说出阶段指令；同时播放短暂的氛围提示音。 */
    fun speakPhase(text: String, fallbackChime: Boolean = true, flush: Boolean = false) {
        val message = text.trim()
        if (message.isEmpty()) return

        // TTS 通道（长句 TTS 推荐 flush，避免上一句尾音拖到下一段）。
        // 训练开始时 ttsReady 可能是 false：TTS 引擎初始化是异步的，
        // 用户可能在 onInit 回调完成之前就点了开始。这种情况下也要尝试 speak，
        // 让 TTS 引擎内部排队，等就绪后自动播出来，避免静默。
        val t = tts
        if (t != null) {
            try {
                val mode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                val result = t.speak(message, mode, null, "phase-${System.currentTimeMillis()}")
                if (result == TextToSpeech.ERROR) {
                    Log.w(tag, "TTS speak returned ERROR for: $message")
                } else if (!ttsReady) {
                    // speak 成功但 ttsReady 还是 false：等引擎就绪会自动播放。
                    Log.i(tag, "TTS queued (not ready yet): $message")
                }
            } catch (e: Exception) {
                Log.w(tag, "TTS speak failed, fallback to chime: ${e.message}")
            }
        } else {
            Log.w(tag, "TTS engine is null, cannot speak: $message")
        }

        // 同时播放柔和音效
        if (fallbackChime && soundsLoaded) {
            // 提示音小一些用于补充
            soundPool.play(chimePhaseId, 0.4f, 0.4f, 1, 0, 1.0f)
        }
    }

    fun playInhaleChime() {
        if (!soundsLoaded) return
        soundPool.play(chimeInhaleId, 0.6f, 0.6f, 2, 0, 1.0f)
    }

    fun playExhaleChime() {
        if (!soundsLoaded) return
        soundPool.play(chimeExhaleId, 0.65f, 0.65f, 2, 0, 1.0f)
    }

    /** 屏息阶段播报的轻钟声（吸气后屏息/呼气后屏息共用）。 */
    fun playHoldChime(variant: Int = 0) {
        if (!soundsLoaded) return
        // 盒式第二段屏息使用略不同的小钟声（用 rate 微调区分）
        val rate = if (variant == 1) 0.94f else 1.05f
        soundPool.play(chimePhaseId, 0.45f, 0.45f, 1, 0, rate)
    }

    /** 暂停并清空 TTS 队列。 */
    fun pause() {
        tts?.stop()
    }

    /** 完全释放资源。 */
    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.w(tag, "Error shutting down tts: ${e.message}")
        }
        tts = null
        ttsReady = false
        soundPool.release()
    }

    /** 当前是否使用了女声合成（用于在设置中提示）。 */
    fun isFemaleVoiceReady(): Boolean = ttsReady
}
