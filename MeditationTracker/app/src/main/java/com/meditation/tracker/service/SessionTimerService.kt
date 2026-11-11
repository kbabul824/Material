package com.meditation.tracker.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
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
 * Resilient Media Playback Foreground Service.
 * Counts UP indefinitely until manually stopped by the user.
 * Even after a goal or target is reached, it announces the milestone
 * and continues counting without stopping.
 */
class SessionTimerService : Service(), TextToSpeech.OnInitListener {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var timerJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private var sessionStartTimeMs: Long = 0L
    private var elapsedSecondsBeforePause: Int = 0
    private var totalSecondsConfigured: Int = 0
    private var intervalStepConfigured: Int = 0
    private var lastTriggeredIntervalIndex: Int = 0
    private var goalMilestoneAnnounced: Boolean = false

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
            tts?.setSpeechRate(0.85f)
            tts?.setPitch(0.95f)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
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
            wakeLock?.acquire(4 * 60 * 60 * 1000L) // 4 hours timeout
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

        // If session is ALREADY running, do not reset elapsed time
        if (_sessionState.value.isRunning) {
            return
        }

        totalSecondsConfigured = totalSeconds
        intervalStepConfigured = intervalStep
        lastTriggeredIntervalIndex = 0
        goalMilestoneAnnounced = false
        elapsedSecondsBeforePause = 0

        val now = SystemClock.elapsedRealtime()
        sessionStartTimeMs = now

        _sessionState.value = SessionState(
            totalDurationSeconds = totalSeconds,
            remainingSeconds = totalSeconds,
            elapsedSeconds = 0,
            intervalStepSeconds = intervalStep,
            isRunning = true,
            isFinished = false
        )

        promoteToForeground()
        runTimerLoop()
    }

    private fun promoteToForeground() {
        val notification = buildNotification(_sessionState.value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun runTimerLoop() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive && _sessionState.value.isRunning) {
                val now = SystemClock.elapsedRealtime()
                val elapsedMs = now - sessionStartTimeMs
                val elapsedSec = (elapsedMs / 1000L).toInt()

                // 1. Periodic Interval Check (chimes & announces minutes completed)
                if (intervalStepConfigured > 0 && elapsedSec > 0) {
                    val currentIntervalIndex = elapsedSec / intervalStepConfigured
                    if (currentIntervalIndex > lastTriggeredIntervalIndex) {
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

                // 2. Target Goal Milestone Check (announced when target is first reached, but timer continues!)
                if (totalSecondsConfigured in 1..86399 && elapsedSec >= totalSecondsConfigured && !goalMilestoneAnnounced) {
                    goalMilestoneAnnounced = true
                    val goalMinutes = totalSecondsConfigured / 60
                    val announcement = "Goal of $goalMinutes minutes reached"
                    playBellAndAnnounce(announcement)
                }

                val remainingSec = (totalSecondsConfigured - elapsedSec).coerceAtLeast(0)

                _sessionState.value = _sessionState.value.copy(
                    remainingSeconds = remainingSec,
                    elapsedSeconds = elapsedSec,
                    isFinished = false // Always continue until user manually stops
                )

                updateNotification(_sessionState.value)

                delay(500L)
            }
        }
    }

    private fun pauseSession() {
        val now = SystemClock.elapsedRealtime()
        elapsedSecondsBeforePause = ((now - sessionStartTimeMs) / 1000L).toInt()
        _sessionState.value = _sessionState.value.copy(
            elapsedSeconds = elapsedSecondsBeforePause,
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
        sessionStartTimeMs = now - (elapsedSecondsBeforePause * 1000L)
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
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
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

        // 3. Spoken voice announcement
        serviceScope.launch {
            delay(550L)
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
        val statusText = if (state.totalDurationSeconds >= 86400) {
            // Open-ended / Long Duration
            if (state.isRunning) "Meditation active • $timeFormatted" else "Paused • $timeFormatted"
        } else {
            val targetMinutes = state.totalDurationSeconds / 60
            if (state.elapsedSeconds >= state.totalDurationSeconds) {
                if (state.isRunning) "Meditation active • $timeFormatted (Goal reached)" else "Paused • $timeFormatted (Goal reached)"
            } else {
                if (state.isRunning) "Meditation active • $timeFormatted / ${targetMinutes}:00" else "Paused • $timeFormatted / ${targetMinutes}:00"
            }
        }

        return NotificationCompat.Builder(this, MeditationApp.CHANNEL_ID)
            .setContentTitle("Meditation Session")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(state.isRunning)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
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
