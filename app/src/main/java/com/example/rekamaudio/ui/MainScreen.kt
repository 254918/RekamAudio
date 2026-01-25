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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
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

    // Media Projection Launcher
    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            viewModel.startRecordingService(result.resultCode, result.data!!)
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
                TopAppBar(title = { Text("Rekam Audio") })
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = {
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
                        RecordingItem(
                            recording = recording,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
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
                                    // TODO: Play audio
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
            }
            
            if (!isSelectionMode) {
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
