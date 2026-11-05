package com.meditation.tracker.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.*
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import com.meditation.tracker.MainActivity
import com.meditation.tracker.MeditationApp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Robust Foreground Service that uses a Partial WakeLock, SystemClock.elapsedRealtime(),
 * and Android Text-to-Speech (TTS) to chime and announce completed minutes at each interval.
 */
class SessionTimerService : Service(), TextToSpeech.OnInitListener {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var timerJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private var targetEndTimeMs: Long = 0L
    private var totalSecondsConfigured: Int = 0
    private var intervalStepConfigured: Int = 0
    private var lastTriggeredIntervalIndex: Int = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
        initTextToSpeech()
    }

    private fun initTextToSpeech() {
        try {
            tts = TextToSpeech(applicationContext, this)
        } catch (_: Exception) {}
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
            // Use gentle, calm speech rate for meditation
            tts?.setSpeechRate(0.85f)
            tts?.setPitch(0.95f)
            
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts?.setAudioAttributes(audioAttributes)
            isTtsReady = true
        }
    }

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
        return START_STICKY
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "MeditationTracker::SessionTimerWakeLock"
            ).apply {
                setReferenceCounted(false)
            }
        }
        if (wakeLock?.isHeld != true) {
            wakeLock?.acquire(4 * 60 * 60 * 1000L) // 4 hours max timeout
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
    }

    private fun startSession(totalSeconds: Int, intervalStep: Int) {
        acquireWakeLock()
        totalSecondsConfigured = totalSeconds
        intervalStepConfigured = intervalStep
        lastTriggeredIntervalIndex = 0

        val now = SystemClock.elapsedRealtime()
        targetEndTimeMs = now + (totalSeconds * 1000L)

        _sessionState.value = SessionState(
            totalDurationSeconds = totalSeconds,
            remainingSeconds = totalSeconds,
            intervalStepSeconds = intervalStep,
            isRunning = true,
            isFinished = false
        )

        startForeground(NOTIFICATION_ID, buildNotification(_sessionState.value))
        runTimerLoop()
    }

    private fun runTimerLoop() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive && _sessionState.value.isRunning && targetEndTimeMs > 0) {
                val now = SystemClock.elapsedRealtime()
                val remainingMs = (targetEndTimeMs - now).coerceAtLeast(0L)
                val remainingSec = (remainingMs / 1000L).toInt()
                val elapsedSec = totalSecondsConfigured - remainingSec

                // Interval bell & voice announcement
                if (intervalStepConfigured > 0 && elapsedSec > 0) {
                    val currentIntervalIndex = elapsedSec / intervalStepConfigured
                    if (currentIntervalIndex > lastTriggeredIntervalIndex && remainingSec > 0) {
                        lastTriggeredIntervalIndex = currentIntervalIndex
                        val minutesCompleted = elapsedSec / 60
                        val announcement = if (minutesCompleted == 1) {
                            "1 minute completed"
                        } else {
                            "$minutesCompleted minutes completed"
                        }
                        playBellAndAnnounce(announcement)
                    }
                }

                _sessionState.value = _sessionState.value.copy(
                    remainingSeconds = remainingSec,
                    elapsedSeconds = elapsedSec,
                    isFinished = remainingSec <= 0
                )

                updateNotification(_sessionState.value)

                if (remainingSec <= 0) {
                    val totalMinutes = totalSecondsConfigured / 60
                    val finalMessage = if (totalMinutes == 1) {
                        "Session complete. 1 minute completed."
                    } else {
                        "Session complete. $totalMinutes minutes completed."
                    }
                    playBellAndAnnounce(finalMessage)
                    releaseWakeLock()
                    stopForeground(STOP_FOREGROUND_DETACH)
                    break
                }

                delay(500L)
            }
        }
    }

    private fun pauseSession() {
        val now = SystemClock.elapsedRealtime()
        val remainingMs = (targetEndTimeMs - now).coerceAtLeast(0L)
        _sessionState.value = _sessionState.value.copy(
            remainingSeconds = (remainingMs / 1000L).toInt(),
            isRunning = false
        )
        timerJob?.cancel()
        releaseWakeLock()
        tts?.stop()
        updateNotification(_sessionState.value)
    }

    private fun resumeSession() {
        acquireWakeLock()
        val now = SystemClock.elapsedRealtime()
        targetEndTimeMs = now + (_sessionState.value.remainingSeconds * 1000L)
        _sessionState.value = _sessionState.value.copy(isRunning = true)
        runTimerLoop()
    }

    private fun stopSession() {
        timerJob?.cancel()
        releaseWakeLock()
        tts?.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun playBellAndAnnounce(voiceMessage: String) {
        // 1. Play gentle bell tone
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 85)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 450)
        } catch (_: Exception) {}

        // 2. Gentle haptic vibration
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

        // 3. Spoken voice announcement through Text-to-Speech
        serviceScope.launch {
            delay(550L) // Wait half a second after the bell sound finishes
            if (isTtsReady && tts != null) {
                tts?.speak(voiceMessage, TextToSpeech.QUEUE_FLUSH, null, "meditation_minute_announcement")
            }
        }
    }

    private fun buildNotification(state: SessionState): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val minutes = state.elapsedSeconds / 60
        val seconds = state.elapsedSeconds % 60
        val timeFormatted = String.format("%02d:%02d", minutes, seconds)
        val targetMinutes = state.totalDurationSeconds / 60
        val statusText = if (state.isRunning) "Meditation active • $timeFormatted / ${targetMinutes}:00" else "Paused • $timeFormatted / ${targetMinutes}:00"

        return NotificationCompat.Builder(this, MeditationApp.CHANNEL_ID)
            .setContentTitle("Meditation Session")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(state.isRunning)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(state: SessionState) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(state))
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        tts?.stop()
        tts?.shutdown()
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
            val elapsedSeconds: Int = 0,
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
