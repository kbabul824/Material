package com.meditation.tracker.ui.screens.longduration

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
fun LongDurationConfigScreen(
    onBack: () -> Unit,
    onStartSession: (durationSeconds: Int, milestoneStepSeconds: Int) -> Unit
) {
    var sessionMinutes by remember { mutableFloatStateOf(30f) }
    var enableMilestoneBells by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Long Duration Setup") },
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
                "Commit to sustained deep mindfulness with subtle progress milestones.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Duration Slider
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Target Goal: ${sessionMinutes.toInt()} minutes",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Slider(
                        value = sessionMinutes,
                        onValueChange = { sessionMinutes = it },
                        valueRange = 10f..120f,
                        steps = 21
                    )
                }
            }

            // Milestone Toggle
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Quarter-Way Milestone Bells", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Plays a gentle alert at 25%, 50%, and 75% marks.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = enableMilestoneBells,
                        onCheckedChange = { enableMilestoneBells = it }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val duration = (sessionMinutes * 60).toInt()
                    val step = if (enableMilestoneBells) duration / 4 else 0
                    onStartSession(duration, step)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Long Meditation")
            }
        }
    }
}
