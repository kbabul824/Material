package com.meditation.tracker.data.repository

import com.meditation.tracker.data.local.SessionDao
import com.meditation.tracker.data.model.MeditationMode
import com.meditation.tracker.data.model.SessionHistory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeditationRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao
) : MeditationRepository {
    override fun getAllSessions(): Flow<List<SessionHistory>> = sessionDao.getAllSessions()
    override fun getSessionsByMode(mode: MeditationMode): Flow<List<SessionHistory>> = sessionDao.getSessionsByMode(mode)
    override fun getTotalSessionsCount(): Flow<Int> = sessionDao.getTotalSessionsCount()
    override fun getTotalMeditationTimeSeconds(): Flow<Long> = sessionDao.getTotalMeditationTimeSeconds()
    override suspend fun saveSession(session: SessionHistory): Long = sessionDao.insertSession(session)
    override suspend fun deleteSession(session: SessionHistory) = sessionDao.deleteSession(session)
}
