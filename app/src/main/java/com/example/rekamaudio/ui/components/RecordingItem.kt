package com.example.rekamaudio.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.rekamaudio.data.model.Recording
import java.util.Date

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecordingItem(
    recording: Recording,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isPlaying: Boolean,
    onDelete: (Recording) -> Unit,
    onRename: (Recording) -> Unit,
    onShare: (Recording) -> Unit,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = if (isSelected) 
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else 
            CardDefaults.cardColors()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() }
                )
            }
            
            Column(modifier = Modifier.weight(1f).padding(8.dp)) {
                Text(text = recording.fileName, style = MaterialTheme.typography.bodyLarge)
                Text(text = "Date: ${Date(recording.createdAt)}", style = MaterialTheme.typography.labelSmall)
                if (isPlaying) {
                    PlaybackVisualizer(modifier = Modifier.height(24.dp).width(48.dp))
                }
            }
            
            if (!isSelectionMode) {
                IconButton(onClick = onClick) {
                     Icon(
                         if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                         contentDescription = if (isPlaying) "Stop" else "Play"
                     )
                 }

                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = { expanded = false; onShare(recording) }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = { expanded = false; onRename(recording) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { expanded = false; onDelete(recording) }
                    )
                }
            }
        }
    }
}

@Composable
fun PlaybackVisualizer(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")
    
    // Animate 3 bars with different offsets
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val scale2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val scale3 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VisualizerBar(scale = scale1, color = MaterialTheme.colorScheme.primary)
        VisualizerBar(scale = scale2, color = MaterialTheme.colorScheme.primary)
        VisualizerBar(scale = scale3, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun VisualizerBar(scale: Float, color: Color) {
    Box(
        modifier = Modifier
            .width(6.dp)
            .fillMaxHeight(fraction = scale)
            .background(color, shape = androidx.compose.foundation.shape.CircleShape)
    )
}
