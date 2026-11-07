package com.meditation.tracker

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MeditationApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Active Meditation Session",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows progress and provides controls for active meditation sessions"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Meditation Interval Bells",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts and gentle bells marking meditation intervals"
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            manager.createNotificationChannel(alertChannel)
        }
    }

    companion object {
        const val CHANNEL_ID = "meditation_session_channel"
        const val ALERT_CHANNEL_ID = "meditation_interval_alert_channel"
    }
}
