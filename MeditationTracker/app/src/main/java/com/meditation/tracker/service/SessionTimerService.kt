package com.meditation.tracker.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.meditation.tracker.MainActivity
import com.meditation.tracker.MeditationApp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Foreground Service that maintains reliable time tracking and bell alerts
 * even when the screen turns off or the app is put in the background.
 */
class SessionTimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var timerJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val totalSeconds = intent.getIntExtra(EXTRA_TOTAL_SECONDS, 600)
                val intervalStep = intent.getIntExtra(EXTRA_INTERVAL_STEP, 0)
                startSession(totalSeconds, intervalStep)
            }
            ACTION_PAUSE -> pauseSession()
            ACTION_RESUME -> resumeSession()
            ACTION_STOP -> stopSession()
        }
        return START_NOT_STICKY
    }

    private fun startSession(totalSeconds: Int, intervalStep: Int) {
        _sessionState.value = SessionState(
            totalDurationSeconds = totalSeconds,
            remainingSeconds = totalSeconds,
            intervalStepSeconds = intervalStep,
            isRunning = true
        )

        startForeground(NOTIFICATION_ID, buildNotification(_sessionState.value))
        runTimerLoop()
    }

    private fun runTimerLoop() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive && _sessionState.value.isRunning && _sessionState.value.remainingSeconds > 0) {
                delay(1000L)
                val current = _sessionState.value
                val newRemaining = current.remainingSeconds - 1
                val elapsed = current.totalDurationSeconds - newRemaining

                // Trigger Interval Bell Check
                if (current.intervalStepSeconds > 0 && elapsed > 0 && elapsed % current.intervalStepSeconds == 0) {
                    playBellAlert()
                }

                _sessionState.value = current.copy(
                    remainingSeconds = newRemaining,
                    isFinished = newRemaining <= 0
                )

                updateNotification(_sessionState.value)

                if (newRemaining <= 0) {
                    playBellAlert() // Completion chime
                    stopForeground(STOP_FOREGROUND_DETACH)
                    break
                }
            }
        }
    }

    private fun pauseSession() {
        _sessionState.value = _sessionState.value.copy(isRunning = false)
        timerJob?.cancel()
        updateNotification(_sessionState.value)
    }

    private fun resumeSession() {
        _sessionState.value = _sessionState.value.copy(isRunning = true)
        runTimerLoop()
    }

    private fun stopSession() {
        timerJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun playBellAlert() {
        // High-frequency mindful alert tone
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 80)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
        } catch (_: Exception) {}

        // Gentle dual pulse vibration
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 200), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(250)
        }
    }

    private fun buildNotification(state: SessionState): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val minutes = state.remainingSeconds / 60
        val seconds = state.remainingSeconds % 60
        val timeFormatted = String.format("%02d:%02d", minutes, seconds)
        val statusText = if (state.isRunning) "Meditation in progress • $timeFormatted left" else "Paused • $timeFormatted left"

        return NotificationCompat.Builder(this, MeditationApp.CHANNEL_ID)
            .setContentTitle("Meditation Session")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(state.isRunning)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(state: SessionState) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(state))
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_TOTAL_SECONDS = "EXTRA_TOTAL_SECONDS"
        const val EXTRA_INTERVAL_STEP = "EXTRA_INTERVAL_STEP"

        data class SessionState(
            val totalDurationSeconds: Int = 0,
            val remainingSeconds: Int = 0,
            val intervalStepSeconds: Int = 0,
            val isRunning: Boolean = false,
            val isFinished: Boolean = false
        )

        private val _sessionState = MutableStateFlow(SessionState())
        val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

        fun start(context: Context, totalSeconds: Int, intervalStep: Int = 0) {
            val intent = Intent(context, SessionTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TOTAL_SECONDS, totalSeconds)
                putExtra(EXTRA_INTERVAL_STEP, intervalStep)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pause(context: Context) {
            context.startService(Intent(context, SessionTimerService::class.java).apply { action = ACTION_PAUSE })
        }

        fun resume(context: Context) {
            context.startService(Intent(context, SessionTimerService::class.java).apply { action = ACTION_RESUME })
        }

        fun stop(context: Context) {
            context.startService(Intent(context, SessionTimerService::class.java).apply { action = ACTION_STOP })
        }
    }
}
