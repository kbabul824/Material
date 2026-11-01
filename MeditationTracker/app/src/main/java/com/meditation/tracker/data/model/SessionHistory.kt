package com.meditation.tracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persistent Room Entity for a recorded meditation session.
 */
@Entity(tableName = "meditation_sessions")
data class SessionHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTimeEpochMs: Long,
    val endTimeEpochMs: Long,
    val durationSeconds: Int,
    val mode: MeditationMode,
    val intervalConfigSummary: String,
    val notes: String = "",
    val completedSuccessfully: Boolean = true
)
