package com.breath.trainer

import android.app.Application
import android.util.Log
import com.breath.trainer.audio.BackgroundMusicManager
import com.breath.trainer.audio.TtsManager
import com.breath.trainer.breathing.SettingsRepository

/**
 * 全局持有的资源，避免 Activity 重建时丢失。
 */
class BreathApplication : Application() {

    lateinit var ttsManager: TtsManager
        private set
    lateinit var backgroundMusic: BackgroundMusicManager
        private set
    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        ttsManager = TtsManager(applicationContext)
        backgroundMusic = BackgroundMusicManager(applicationContext)
        settingsRepository = SettingsRepository(applicationContext)

        ttsManager.initialize()
        Log.i(TAG, "BreathApplication initialized")
    }

    override fun onTerminate() {
        ttsManager.release()
        backgroundMusic.stop()
        super.onTerminate()
    }

    companion object {
        private const val TAG = "BreathApplication"
    }
}
