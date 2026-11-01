package com.meditation.tracker

import com.meditation.tracker.data.local.SessionDao
import com.meditation.tracker.data.model.MeditationMode
import com.meditation.tracker.data.model.SessionHistory
import com.meditation.tracker.data.repository.MeditationRepositoryImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SessionRepositoryTest {

    private val sessionDao: SessionDao = mockk(relaxed = true)
    private lateinit var repository: MeditationRepositoryImpl

    @Before
    fun setUp() {
        repository = MeditationRepositoryImpl(sessionDao)
    }

    @Test
    fun saveSession_calls_dao_insert() = runBlocking {
        val session = SessionHistory(
            id = 1L,
            startTimeEpochMs = 1000L,
            endTimeEpochMs = 2000L,
            durationSeconds = 600,
            mode = MeditationMode.INTERVAL,
            intervalConfigSummary = "Every 3m",
            notes = "Peaceful morning session"
        )

        coEvery { sessionDao.insertSession(session) } returns 1L

        val resultId = repository.saveSession(session)
        assertEquals(1L, resultId)
        coVerify(exactly = 1) { sessionDao.insertSession(session) }
    }

    @Test
    fun getSessionsByMode_returns_filtered_flow() = runBlocking {
        val sampleList = listOf(
            SessionHistory(
                id = 1L,
                startTimeEpochMs = 1000L,
                endTimeEpochMs = 2000L,
                durationSeconds = 1200,
                mode = MeditationMode.LONG_DURATION,
                intervalConfigSummary = "25% milestone bells",
                notes = "Deep focus"
            )
        )

        coEvery { sessionDao.getSessionsByMode(MeditationMode.LONG_DURATION) } returns flowOf(sampleList)

        repository.getSessionsByMode(MeditationMode.LONG_DURATION).collect { result ->
            assertEquals(1, result.size)
            assertEquals(MeditationMode.LONG_DURATION, result.first().mode)
            assertEquals("Deep focus", result.first().notes)
        }
    }
}
