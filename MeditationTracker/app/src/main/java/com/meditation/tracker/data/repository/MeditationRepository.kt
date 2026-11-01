package com.meditation.tracker.data.repository

import com.meditation.tracker.data.model.MeditationMode
import com.meditation.tracker.data.model.SessionHistory
import kotlinx.coroutines.flow.Flow

interface MeditationRepository {
    fun getAllSessions(): Flow<List<SessionHistory>>
    fun getSessionsByMode(mode: MeditationMode): Flow<List<SessionHistory>>
    fun getTotalSessionsCount(): Flow<Int>
    fun getTotalMeditationTimeSeconds(): Flow<Long>
    suspend fun saveSession(session: SessionHistory): Long
    suspend fun deleteSession(session: SessionHistory)
}
