package com.meditation.tracker.ui.screens.session

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meditation.tracker.service.SessionTimerService

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun SessionScreen(
    modeString: String,
    targetDurationSeconds: Int,
    intervalStepSeconds: Int,
    onSessionFinished: () -> Unit,
    onCancel: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val serviceState by SessionTimerService.sessionState.collectAsState()
    var showSaveDialog by remember { mutableStateOf(false) }
    var notesText by remember { mutableStateOf("") }

    // Helper function to guarantee session is saved to history whenever manually stopped
    val saveCurrentSessionToHistory: (notes: String) -> Unit = { userNotes ->
        val elapsed = serviceState.elapsedSeconds
        if (elapsed >= 3) {
            val configSummary = if (intervalStepSeconds > 0) {
                "Chime every ${intervalStepSeconds / 60}m"
            } else {
                "Open duration"
            }
            viewModel.saveCompletedSession(
                mode = modeString,
                durationSeconds = elapsed,
                intervalConfigSummary = configSummary,
                notes = userNotes
            )
        }
    }

    // Keep the screen awake during active meditation so screen timeout does not lock it
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Start timer foreground service on initial launch
    LaunchedEffect(Unit) {
        SessionTimerService.start(context, targetDurationSeconds, intervalStepSeconds)
    }

    // Handle physical or gesture back button: save completed time automatically
    BackHandler {
        saveCurrentSessionToHistory(notesText)
        SessionTimerService.stop(context)
        onSessionFinished()
    }

    // Mindful Breathing Animation
    val infiniteTransition = rememberInfiniteTransition(label = "breath")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Mode Header
            Text(
                text = if (modeString == "INTERVAL") "Interval Meditation" else "Long Duration Meditation",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Visual breathing circle & count-up display
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(280.dp)
            ) {
                // Outer pulsing aura
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(breathScale)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            shape = CircleShape
                        )
                )

                // Timer numbers counting UP from 0:00 (continues indefinitely even after goal!)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val elapsed = serviceState.elapsedSeconds
                    val minutes = elapsed / 60
                    val seconds = elapsed % 60
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Goal status indicator for Interval mode
                    if (modeString == "INTERVAL" && targetDurationSeconds in 1..86399) {
                        Spacer(modifier = Modifier.height(2.dp))
                        val goalMinutes = targetDurationSeconds / 60
                        if (elapsed >= targetDurationSeconds) {
                            Text(
                                text = "Goal reached ($goalMinutes:00) • Continuing",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "Goal: $goalMinutes:00",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (serviceState.isRunning) "Inhale • Exhale" else "Paused",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Controls row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stop button: pauses and opens save dialog
                FilledTonalIconButton(
                    onClick = {
                        SessionTimerService.pause(context)
                        showSaveDialog = true
                    },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Stop Session")
                }

                // Pause / Play
                FloatingActionButton(
                    onClick = {
                        if (serviceState.isRunning) {
                            SessionTimerService.pause(context)
                        } else {
                            SessionTimerService.resume(context)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = if (serviceState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (serviceState.isRunning) "Pause" else "Resume",
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Complete and Save button
                FilledTonalIconButton(
                    onClick = {
                        SessionTimerService.pause(context)
                        showSaveDialog = true
                    },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Finish and Save")
                }
            }
        }
    }

    // Save Reflection Dialog
    if (showSaveDialog) {
        val elapsed = serviceState.elapsedSeconds
        AlertDialog(
            onDismissRequest = {
                // Even if dismissed, completed time is preserved and saved
                saveCurrentSessionToHistory(notesText)
                SessionTimerService.stop(context)
                showSaveDialog = false
                onSessionFinished()
            },
            title = { Text("Session Ended") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Time completed: ${elapsed / 60}m ${elapsed % 60}s")
                    Text(
                        text = "Your completed time has been saved to your history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Reflection notes (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        saveCurrentSessionToHistory(notesText)
                        SessionTimerService.stop(context)
                        showSaveDialog = false
                        onSessionFinished()
                    }
                ) {
                    Text("Save & View History")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        saveCurrentSessionToHistory("")
                        SessionTimerService.stop(context)
                        showSaveDialog = false
                        onSessionFinished()
                    }
                ) {
                    Text("Done")
                }
            }
        )
    }
}
