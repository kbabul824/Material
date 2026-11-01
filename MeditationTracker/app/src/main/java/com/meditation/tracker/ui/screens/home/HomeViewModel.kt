package com.meditation.tracker.ui.screens.home

import androidx.lifecycle.ViewModel
import com.meditation.tracker.data.repository.MeditationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: MeditationRepository
) : ViewModel() {
    val totalSessionsCount: Flow<Int> = repository.getTotalSessionsCount()
    val totalMeditationTime: Flow<Long> = repository.getTotalMeditationTimeSeconds()
}
