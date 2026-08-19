package com.breath.trainer.audio

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import java.io.IOException

/**
 * 后台循环播放器：根据 [setAmbient] 在不同 [AmbientSound] 之间切换。
 *
 * - 默认播放 [AmbientSounds.CALM] 的 30 秒循环氛围音。
 * - 通过 [AudioAttributes] 标记为 [USAGE_MEDIA]，音量跟随系统媒体音量，避免与 TTS 抢流。
 * - 切音源时先 stop 再 start，状态在 enabled=false 时静默。
 */
class BackgroundMusicManager(private val appContext: Context) {

    private var player: MediaPlayer? = null
    private val tag = "BackgroundMusic"

    /** 是否启用循环播放。 */
    @Volatile
    private var enabled: Boolean = true

    /** 当前激活的环境音。 */
    @Volatile
    private var ambient: AmbientSound = AmbientSounds.CALM

    /** 音量 0..1。 */
    @Volatile
    private var volume: Float = 0.45f


    fun isEnabled(): Boolean = enabled

    fun getAmbient(): AmbientSound = ambient

    fun getVolume(): Float = volume


    fun start() {
        if (!enabled) return
        if (player != null && player?.isPlaying == true) return

        try {
            val mp = MediaPlayer()
            applyAudioAttributes(mp)

            mp.setOnPreparedListener { p ->
                p.isLooping = true
                p.setVolume(volume, volume)
                p.start()
                Log.i(tag, "Background music started: ${ambient.id}")
            }

            mp.setOnErrorListener { _, what, extra ->
                Log.e(
                    tag,
                    "MediaPlayer error what=$what extra=$extra (ambient=${ambient.id})"
                )
                stop()
                true
            }

            openResource(mp, ambient.rawResId)
            player = mp

        } catch (e: Exception) {
            Log.e(tag, "start failed: ${e.message}")
            stop()
        }
    }


    private fun applyAudioAttributes(mp: MediaPlayer) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
        } else {
            @Suppress("DEPRECATION")
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC)
        }
    }


    private fun openResource(mp: MediaPlayer, resId: Int) {
        val afd: AssetFileDescriptor =
            appContext.resources.openRawResourceFd(resId)

        try {
            mp.setDataSource(
                afd.fileDescriptor,
                afd.startOffset,
                afd.length
            )
            mp.prepareAsync()

        } finally {
            try {
                afd.close()
            } catch (e: IOException) {
                Log.w(tag, "afd close: ${e.message}")
            }
        }
    }


    fun setEnabled(value: Boolean) {
        enabled = value

        if (!value) {
            stop()
        } else {
            start()
        }
    }


    fun setAmbient(value: AmbientSound) {
        val same = value.id == ambient.id

        ambient = value

        if (!enabled) return

        if (same && player?.isPlaying == true) {
            return
        }

        restartInternal()
    }


    private fun restartInternal() {
        val wasPlaying = player?.isPlaying == true

        stop()

        if (wasPlaying || enabled) {
            start()
        }
    }


    fun setVolume(value: Float) {
        volume = value.coerceIn(0f, 1f)
        player?.setVolume(volume, volume)
    }


    fun pause() {
        runCatching {
            player?.pause()
        }.onFailure {
            Log.w(tag, "pause failed: ${it.message}")
        }
    }


    fun stop() {
        try {
            player?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }

        } catch (e: Exception) {
            Log.w(tag, "stop failed: ${e.message}")

        } finally {
            player = null
        }
    }
}
