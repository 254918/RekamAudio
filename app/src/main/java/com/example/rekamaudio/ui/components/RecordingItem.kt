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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.rekamaudio.R
import com.example.rekamaudio.data.model.Recording
import com.example.rekamaudio.player.PlaybackProgress
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecordingItem(
    recording: Recording,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isPlaying: Boolean,
    playbackProgress: PlaybackProgress,
    onSeek: (Int) -> Unit,
    onDelete: (Recording) -> Unit,
    onRename: (Recording) -> Unit,
    onShare: (Recording) -> Unit,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val dateText = remember(recording.createdAt) {
        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date(recording.createdAt))
    }

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

            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, shape = androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = stringResource(R.string.recording),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(modifier = Modifier.weight(1f).padding(8.dp)) {
                Text(text = recording.fileName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(R.string.date_label, dateText),
                    style = MaterialTheme.typography.labelSmall
                )
                if (isPlaying) {
                    if (playbackProgress.durationMs > 0) {
                        PlaybackProgressBar(
                            progress = playbackProgress,
                            onSeek = onSeek,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    } else {
                        PlaybackVisualizer(modifier = Modifier.height(24.dp).width(48.dp))
                    }
                }
            }

            if (!isSelectionMode) {
                IconButton(onClick = onClick) {
                     Icon(
                         if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                         contentDescription = stringResource(if (isPlaying) R.string.stop else R.string.play)
                     )
                 }

                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more))
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share)) },
                        onClick = { expanded = false; onShare(recording) },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.rename)) },
                        onClick = { expanded = false; onRename(recording) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = { expanded = false; onDelete(recording) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                    )
                }
            }
        }
    }
}

@Composable
fun PlaybackProgressBar(
    progress: PlaybackProgress,
    onSeek: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // While dragging we show the finger position; otherwise follow playback
    var dragFraction by remember { mutableStateOf<Float?>(null) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatDuration(if (dragFraction != null) {
                (dragFraction!! * progress.durationMs).toInt()
            } else progress.positionMs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = dragFraction ?: progress.fraction,
            onValueChange = { dragFraction = it },
            onValueChangeFinished = {
                dragFraction?.let { fraction ->
                    onSeek((fraction * progress.durationMs).toInt())
                }
                dragFraction = null
            },
            modifier = Modifier
                .weight(1f)
                .height(28.dp)
                .padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        Text(
            text = formatDuration(progress.durationMs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDuration(ms: Int): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
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
