package com.meditation.tracker.ui.navigation

sealed class Screen(val route: String, val title: String) {
    data object Home : Screen("home", "Home")
    data object IntervalConfig : Screen("interval_config", "Interval Reminder")
    data object LongDurationConfig : Screen("long_duration_config", "Long Duration")
    data object Session : Screen(
        "session/{mode}/{durationSeconds}/{intervalStepSeconds}",
        "Active Session"
    ) {
        fun createRoute(mode: String, durationSeconds: Int, intervalStepSeconds: Int): String {
            return "session/$mode/$durationSeconds/$intervalStepSeconds"
        }
    }
    data object History : Screen("history", "History")
    data object Settings : Screen("settings", "Settings")
}
