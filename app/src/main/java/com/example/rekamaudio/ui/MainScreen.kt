package com.example.rekamaudio.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.rekamaudio.data.model.Recording
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val recordings by viewModel.recordings.collectAsState()
    val selectedIds by viewModel.selectedRecordingIds.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val context = LocalContext.current
    val isSelectionMode = selectedIds.isNotEmpty()

    // Handle Back Press to clear selection
    BackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    var showRenameDialog by remember { mutableStateOf(false) }
    var recordingToRename by remember { mutableStateOf<Recording?>(null) }

    if (showRenameDialog && recordingToRename != null) {
        RenameDialog(
            recording = recordingToRename!!,
            onDismiss = { showRenameDialog = false },
            onRename = { newName ->
                viewModel.renameRecording(recordingToRename!!, newName)
                showRenameDialog = false
            }
        )
    }

    // Overlay Permission Launcher
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { 
        if (android.provider.Settings.canDrawOverlays(context)) {
            Toast.makeText(context, "Overlay Permission Granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Overlay Permission Required", Toast.LENGTH_SHORT).show()
        }
    }

    // Media Projection Launcher (Modified to handle Overlay mode)
    var isOverlayMode by remember { mutableStateOf(false) }
    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            if (isOverlayMode) {
                viewModel.startOverlayService(result.resultCode, result.data!!)
                // Minimize app?
                // (context as? Activity)?.moveTaskToBack(true)
            } else {
                viewModel.startRecordingService(result.resultCode, result.data!!)
            }
        } else {
            Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else true

        if (audioGranted && notificationGranted) {
             // Check Overlay Permission if needed
             if (isOverlayMode && !android.provider.Settings.canDrawOverlays(context)) {
                 val intent = Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${context.packageName}"))
                 overlayPermissionLauncher.launch(intent)
                 return@rememberLauncherForActivityResult
             }

            val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
        } else {
            Toast.makeText(context, "Permissions required", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedIds.size} Selected") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.shareSelectedRecordings() }) {
                            Icon(Icons.Default.Share, contentDescription = "Share Selected")
                        }
                        IconButton(onClick = { viewModel.deleteSelectedRecordings() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Rekam Audio") }
                )
            }
        },

        floatingActionButton = {
            if (!isSelectionMode) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Overlay Mode FAB
                    if (uiState !is RecordingUiState.Recording) {
                         SmallFloatingActionButton(
                            onClick = {
                                isOverlayMode = true
                                // Request permissions sequence for overlay
                                val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                permissionLauncher.launch(permissions.toTypedArray())
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Icon(Icons.Default.PictureInPicture, contentDescription = "Floating Mode")
                        }
                    }

                    // Main Recording FAB
                    FloatingActionButton(
                        onClick = {
                            isOverlayMode = false
                            if (uiState is RecordingUiState.Recording) {
                                viewModel.stopRecordingService()
                            } else {
                                val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                permissionLauncher.launch(permissions.toTypedArray())
                            }
                        },
                        containerColor = if (uiState is RecordingUiState.Recording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = if (uiState is RecordingUiState.Recording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (uiState is RecordingUiState.Recording) "Stop" else "Record"
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (recordings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No recordings yet")
                }
            } else {
                LazyColumn {
                    items(recordings) { recording ->
                        val isSelected = selectedIds.contains(recording.id)
                        val isPlaying = playbackState == recording.id
                        RecordingItem(
                            recording = recording,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            isPlaying = isPlaying, // Pass playing state
                            onDelete = { viewModel.deleteRecording(it) },
                            onRename = {
                                recordingToRename = it
                                showRenameDialog = true
                            },
                            onShare = { viewModel.shareRecording(it) },
                            onLongClick = { viewModel.toggleSelection(recording.id) },
                            onClick = {
                                if (isSelectionMode) {
                                    viewModel.toggleSelection(recording.id)
                                } else {
                                    viewModel.playRecording(recording) // Use new play action
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

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
                     if (isPlaying) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop")
                     } else {
                         Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                     }
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
            animation = tween(500, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val scale2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 100, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val scale3 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 50, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Bar(scale = scale1, color = MaterialTheme.colorScheme.primary)
        Bar(scale = scale2, color = Color.LightGray)
        Bar(scale = scale3, color = Color.LightGray)
    }
}

@Composable
fun Bar(scale: Float, color: Color) {
    Box(
        modifier = Modifier
            .width(6.dp)
            .fillMaxHeight(fraction = scale)
            .background(color, shape = androidx.compose.foundation.shape.CircleShape)
    )
}

@Composable
fun RenameDialog(recording: Recording, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var text by remember { mutableStateOf(recording.fileName.removeSuffix(".m4a")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Recording") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Name") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onRename(text) }) { Text("Rename") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
