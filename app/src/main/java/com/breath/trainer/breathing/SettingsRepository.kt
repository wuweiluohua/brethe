package com.breath.trainer.breathing

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.breath.trainer.audio.AmbientSounds
import com.breath.trainer.audio.VoiceStyle
import com.breath.trainer.breathing.pattern.BreathingPatterns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "breath_prefs")

/**
 * 用户偏好持久化（训练轮数、声音开关、背景音乐开关、保持屏幕常亮、当前节奏、当前环境音、播报方式）。
 */
class SettingsRepository(private val appContext: Context) {

    object Keys {
        val TotalRounds = intPreferencesKey("total_rounds")
        val SoundEnabled = booleanPreferencesKey("sound_enabled")
        val MusicEnabled = booleanPreferencesKey("music_enabled")
        val KeepScreenOn = booleanPreferencesKey("keep_screen_on")
        val PatternId = stringPreferencesKey("pattern_id")
        val AmbientId = stringPreferencesKey("ambient_id")
        val VoiceStyleId = stringPreferencesKey("voice_style_id")
        val ThemeMode = intPreferencesKey("theme_mode")
    }

    val totalRounds: Flow<Int> = appContext.dataStore.data.map {
        it[Keys.TotalRounds] ?: BreathingPatterns.FOUR_SEVEN_EIGHT.totalRounds
    }

    val soundEnabled: Flow<Boolean> = appContext.dataStore.data.map {
        it[Keys.SoundEnabled] ?: true
    }

    val musicEnabled: Flow<Boolean> = appContext.dataStore.data.map {
        it[Keys.MusicEnabled] ?: true
    }

    val keepScreenOn: Flow<Boolean> = appContext.dataStore.data.map {
        it[Keys.KeepScreenOn] ?: true
    }

    val patternId: Flow<String> = appContext.dataStore.data.map {
        it[Keys.PatternId] ?: BreathingPatterns.FOUR_SEVEN_EIGHT.id
    }

    val ambientId: Flow<String> = appContext.dataStore.data.map {
        it[Keys.AmbientId] ?: AmbientSounds.CALM.id
    }

    val voiceStyleId: Flow<String> = appContext.dataStore.data.map {
        it[Keys.VoiceStyleId] ?: VoiceStyle.GENTLE_LONG.id
    }

    /** 0=跟随系统 1=浅色 2=深色。 */
    val themeMode: Flow<Int> = appContext.dataStore.data.map {
        it[Keys.ThemeMode] ?: 0
    }

    suspend fun setTotalRounds(value: Int) = appContext.dataStore.edit {
        it[Keys.TotalRounds] = value.coerceIn(1, 12)
    }

    suspend fun setSoundEnabled(value: Boolean) = appContext.dataStore.edit {
        it[Keys.SoundEnabled] = value
    }

    suspend fun setMusicEnabled(value: Boolean) = appContext.dataStore.edit {
        it[Keys.MusicEnabled] = value
    }

    suspend fun setKeepScreenOn(value: Boolean) = appContext.dataStore.edit {
        it[Keys.KeepScreenOn] = value
    }

    suspend fun setPatternId(value: String) = appContext.dataStore.edit {
        it[Keys.PatternId] = value
    }

    suspend fun setAmbientId(value: String) = appContext.dataStore.edit {
        it[Keys.AmbientId] = value
    }

    suspend fun setVoiceStyleId(value: String) = appContext.dataStore.edit {
        it[Keys.VoiceStyleId] = value
    }

    suspend fun setThemeMode(value: Int) = appContext.dataStore.edit {
        it[Keys.ThemeMode] = value.coerceIn(0, 2)
    }
}
