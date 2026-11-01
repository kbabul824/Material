package com.meditation.tracker.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meditation.tracker.data.local.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val soundEnabled: Flow<Boolean> = preferencesRepository.soundEnabled
    val vibrationEnabled: Flow<Boolean> = preferencesRepository.vibrationEnabled
    val dynamicTheme: Flow<Boolean> = preferencesRepository.useDynamicTheme

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setSoundEnabled(enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setVibrationEnabled(enabled) }
    }

    fun setDynamicTheme(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDynamicTheme(enabled) }
    }
}
