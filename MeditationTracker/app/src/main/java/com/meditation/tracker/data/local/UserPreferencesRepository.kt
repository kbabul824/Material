package com.meditation.tracker.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "meditation_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val USE_DYNAMIC_THEME = booleanPreferencesKey("use_dynamic_theme")
        val CHIME_TONE = stringPreferencesKey("chime_tone")
    }

    val soundEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.SOUND_ENABLED] ?: true }
    val vibrationEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.VIBRATION_ENABLED] ?: true }
    val useDynamicTheme: Flow<Boolean> = context.dataStore.data.map { it[Keys.USE_DYNAMIC_THEME] ?: true }
    val chimeTone: Flow<String> = context.dataStore.data.map { it[Keys.CHIME_TONE] ?: "Singing Bowl" }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun setDynamicTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.USE_DYNAMIC_THEME] = enabled }
    }

    suspend fun setChimeTone(tone: String) {
        context.dataStore.edit { it[Keys.CHIME_TONE] = tone }
    }
}
