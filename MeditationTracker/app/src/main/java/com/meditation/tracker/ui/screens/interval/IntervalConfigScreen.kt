package com.meditation.tracker.ui.screens.interval

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntervalConfigScreen(
    onBack: () -> Unit,
    onStartSession: (durationSeconds: Int, intervalStepSeconds: Int) -> Unit
) {
    var sessionMinutes by remember { mutableFloatStateOf(15f) }
    var intervalMinutes by remember { mutableFloatStateOf(3f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Interval Reminder Setup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                "Customize interval chimes to bring awareness back during meditation.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Total Duration Slider
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Total Session Duration: ${sessionMinutes.toInt()} minutes",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Slider(
                        value = sessionMinutes,
                        onValueChange = { sessionMinutes = it },
                        valueRange = 5f..60f,
                        steps = 10
                    )
                }
            }

            // Interval Bell Frequency Slider
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Bell Interval: Every ${intervalMinutes.toInt()} minutes",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Slider(
                        value = intervalMinutes,
                        onValueChange = { intervalMinutes = it },
                        valueRange = 1f..15f,
                        steps = 13
                    )
                    Text(
                        text = "You will hear a gentle bell every ${intervalMinutes.toInt()} min until the session ends.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val duration = (sessionMinutes * 60).toInt()
                    val step = (intervalMinutes * 60).toInt()
                    onStartSession(duration, step)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Begin Interval Practice")
            }
        }
    }
}
