package com.meditation.tracker.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meditation.tracker.data.model.MeditationMode
import com.meditation.tracker.data.model.SessionHistory
import com.meditation.tracker.data.repository.MeditationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: MeditationRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow<MeditationMode?>(null)
    val selectedFilter: StateFlow<MeditationMode?> = _selectedFilter.asStateFlow()

    val filteredSessions = _selectedFilter.flatMapLatest { filter ->
        if (filter == null) {
            repository.getAllSessions()
        } else {
            repository.getSessionsByMode(filter)
        }
    }

    fun setFilter(mode: MeditationMode?) {
        _selectedFilter.value = mode
    }

    fun deleteSession(session: SessionHistory) {
        viewModelScope.launch {
            repository.deleteSession(session)
        }
    }
}
