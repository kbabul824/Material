package com.meditation.tracker.ui.screens.session

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

    // Start timer foreground service on initial launch
    LaunchedEffect(Unit) {
        SessionTimerService.start(context, targetDurationSeconds, intervalStepSeconds)
    }

    // Auto-prompt save dialog when session finishes
    LaunchedEffect(serviceState.isFinished) {
        if (serviceState.isFinished && serviceState.totalDurationSeconds > 0) {
            showSaveDialog = true
        }
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

            // Visual breathing circle & countdown display
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(280.dp)
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

                // Timer numbers counting UP from 0:00 to target goal
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val elapsed = serviceState.elapsedSeconds
                    val minutes = elapsed / 60
                    val seconds = elapsed % 60
                    val targetMinutes = targetDurationSeconds / 60
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Goal: ${targetMinutes}:00",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                // Cancel
                FilledTonalIconButton(
                    onClick = {
                        SessionTimerService.stop(context)
                        onCancel()
                    },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel Session")
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

                // Complete early
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
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Complete Session") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val elapsed = targetDurationSeconds - serviceState.remainingSeconds
                    Text("Time practiced: ${elapsed / 60}m ${elapsed % 60}s")
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
                        SessionTimerService.stop(context)
                        val elapsed = (targetDurationSeconds - serviceState.remainingSeconds).coerceAtLeast(1)
                        viewModel.saveCompletedSession(
                            mode = modeString,
                            durationSeconds = elapsed,
                            intervalConfigSummary = if (intervalStepSeconds > 0) "Every ${intervalStepSeconds / 60}m bell" else "No intervals",
                            notes = notesText
                        )
                        showSaveDialog = false
                        onSessionFinished()
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        SessionTimerService.stop(context)
                        showSaveDialog = false
                        onCancel()
                    }
                ) {
                    Text("Discard")
                }
            }
        )
    }
}
