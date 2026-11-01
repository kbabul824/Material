package com.meditation.tracker

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatterTest {

    private fun formatSeconds(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format("%02d:%02d", m, s)
    }

    @Test
    fun formatSeconds_zero_returns_zero_zero() {
        assertEquals("00:00", formatSeconds(0))
    }

    @Test
    fun formatSeconds_standard_minutes() {
        assertEquals("15:30", formatSeconds(930))
    }

    @Test
    fun formatSeconds_full_hour() {
        assertEquals("60:00", formatSeconds(3600))
    }
}
