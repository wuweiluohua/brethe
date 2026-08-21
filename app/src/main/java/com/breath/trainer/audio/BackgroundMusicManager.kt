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
 * 后台循环播放器：根据 [ambient] 在不同 [AmbientSound] 之间切换。
 *
 * - 默认播放 [AmbientSounds.OCEAN] 的 30 秒循环氛围音。
 * - 通过 [AudioAttributes] 标记为 [USAGE_MEDIA]，音量跟随系统媒体音量，避免与 TTS 抢流。
 * - 切音源时先 stop 再 start，状态在 enabled=false 时静默。
 */
class BackgroundMusicManager(private val appContext: Context) {

    private var player: MediaPlayer? = null
    private val tag = "BackgroundMusic"

    /** 是否启用循环播放。 */
    @Volatile
    var enabled: Boolean = true
        set(value) {
            field = value
            if (!value) stop()
        }

    /** 当前激活的环境音。 */
    @Volatile
    var ambient: AmbientSound = AmbientSounds.OCEAN
        set(value) {
            val same = value.id == field.id
            field = value
            if (!enabled) return
            if (!same && player?.isPlaying == true) restartInternal()
        }

    /** 音量 0..1。默认调低，避免盖过温柔女声 / 提示音（提示音约 0.4–0.65）。 */
    @Volatile
    var volume: Float = 0.2f
        set(value) {
            field = value.coerceIn(0f, 1f)
            player?.setVolume(field, field)
        }

    fun start() {
        if (!enabled) return
        // 已有 player 且正在播放：no-op；如果处于准备中（isPlaying==false），
        // 也要 stop 释放旧实例，否则连续点击 / 切音源会泄漏 file descriptor 直到闪退。
        if (player?.isPlaying == true) return
        // 先把可能还活着的旧 player 释放干净
        stop()
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
                Log.e(tag, "MediaPlayer error what=$what extra=$extra (ambient=${ambient.id})")
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
        val afd: AssetFileDescriptor = appContext.resources.openRawResourceFd(resId)
        try {
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            mp.prepareAsync()
        } finally {
            try {
                afd.close()
            } catch (e: IOException) {
                Log.w(tag, "afd close: ${e.message}")
            }
        }
    }

    private fun restartInternal() {
        val wasPlaying = player?.isPlaying == true
        stop()
        if (wasPlaying || enabled) start()
    }

    fun pause() {
        runCatching { player?.pause() }.onFailure { Log.w(tag, "pause failed: ${it.message}") }
    }

    fun stop() {
        try {
            player?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (e: Exception) {
            Log.w(tag, "stop failed: ${e.message}")
        } finally {
            player = null
        }
    }
}
