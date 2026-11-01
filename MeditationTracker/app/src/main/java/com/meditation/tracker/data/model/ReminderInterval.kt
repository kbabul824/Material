package com.meditation.tracker.data.model

/**
 * Represents a reminder checkpoint within a session.
 * @param offsetSeconds seconds elapsed since session start when this alert triggers.
 * @param label descriptive title (e.g. "Warmup complete", "Midpoint check-in", "Deep breathing").
 * @param soundTone resource or tone name identifier.
 */
data class ReminderInterval(
    val offsetSeconds: Int,
    val label: String,
    val soundTone: String = "tibetan_singing_bowl"
)
