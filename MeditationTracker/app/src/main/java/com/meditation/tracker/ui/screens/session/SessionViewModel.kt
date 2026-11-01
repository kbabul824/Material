package com.meditation.tracker.ui.screens.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meditation.tracker.data.model.MeditationMode
import com.meditation.tracker.data.model.SessionHistory
import com.meditation.tracker.data.repository.MeditationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val repository: MeditationRepository
) : ViewModel() {

    fun saveCompletedSession(
        mode: String,
        durationSeconds: Int,
        intervalConfigSummary: String,
        notes: String
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val session = SessionHistory(
                startTimeEpochMs = now - (durationSeconds * 1000L),
                endTimeEpochMs = now,
                durationSeconds = durationSeconds,
                mode = if (mode == "INTERVAL") MeditationMode.INTERVAL else MeditationMode.LONG_DURATION,
                intervalConfigSummary = intervalConfigSummary,
                notes = notes,
                completedSuccessfully = true
            )
            repository.saveSession(session)
        }
    }
}
