package com.meditation.tracker.data.local

import androidx.room.*
import com.meditation.tracker.data.model.MeditationMode
import com.meditation.tracker.data.model.SessionHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM meditation_sessions ORDER BY startTimeEpochMs DESC")
    fun getAllSessions(): Flow<List<SessionHistory>>

    @Query("SELECT * FROM meditation_sessions WHERE mode = :mode ORDER BY startTimeEpochMs DESC")
    fun getSessionsByMode(mode: MeditationMode): Flow<List<SessionHistory>>

    @Query("SELECT * FROM meditation_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): SessionHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionHistory): Long

    @Delete
    suspend fun deleteSession(session: SessionHistory)

    @Query("SELECT COUNT(*) FROM meditation_sessions")
    fun getTotalSessionsCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM meditation_sessions")
    fun getTotalMeditationTimeSeconds(): Flow<Long>
}
