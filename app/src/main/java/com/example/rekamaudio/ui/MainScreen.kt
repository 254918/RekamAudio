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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.net.toUri

import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.rekamaudio.data.model.Recording
import com.example.rekamaudio.service.AudioCaptureService
import com.example.rekamaudio.ui.components.RecordingItem
import com.example.rekamaudio.ui.components.RenameDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onSettingsClick: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val recordings by viewModel.recordings.collectAsState()
    val selectedIds by viewModel.selectedRecordingIds.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val context = LocalContext.current
    val isSelectionMode = selectedIds.isNotEmpty()

    // Event Handling
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MainEvent.StartRecordingService -> {
                    val intent = Intent(context, AudioCaptureService::class.java).apply {
                        action = AudioCaptureService.ACTION_START
                        putExtra(AudioCaptureService.EXTRA_RESULT_CODE, event.resultCode)
                        putExtra(AudioCaptureService.EXTRA_RESULT_DATA, event.data)
                        putExtra(AudioCaptureService.EXTRA_AUDIO_QUALITY, event.quality.name)
                    }
                    context.startForegroundService(intent)
                }
                is MainEvent.StartOverlayService -> {
                    val intent = Intent(context, AudioCaptureService::class.java).apply {
                        action = AudioCaptureService.ACTION_SHOW_OVERLAY
                        putExtra(AudioCaptureService.EXTRA_RESULT_CODE, event.resultCode)
                        putExtra(AudioCaptureService.EXTRA_RESULT_DATA, event.data)
                        putExtra(AudioCaptureService.EXTRA_AUDIO_QUALITY, event.quality.name)
                    }
                    context.startForegroundService(intent)
                }
                is MainEvent.StopRecordingService -> {
                    val intent = Intent(context, AudioCaptureService::class.java).apply {
                        action = event.action
                    }
                    context.startService(intent)
                }
                is MainEvent.ShareRecording -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "audio/*"
                        putExtra(Intent.EXTRA_STREAM, event.uri.toUri())
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Recording"))
                }
                is MainEvent.ShareSelected -> {
                    val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        type = "audio/*"
                        putParcelableArrayListExtra(
                            Intent.EXTRA_STREAM,
                            ArrayList(event.uris.map { it.toUri() })
                        )
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Recordings"))
                }
                is MainEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    BackHandler(enabled = isSelectionMode) { viewModel.clearSelection() }

    val (showRenameDialog, setShowRenameDialog) = remember { mutableStateOf(false) }
    val (recordingToRename, setRecordingToRename) = remember { mutableStateOf<Recording?>(null) }

    if (showRenameDialog && recordingToRename != null) {
        RenameDialog(
            recording = recordingToRename,
            onDismiss = { setShowRenameDialog(false) },
            onRename = { 
                viewModel.renameRecording(recordingToRename, it)
                setShowRenameDialog(false)
            }
        )
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val msg = if (android.provider.Settings.canDrawOverlays(context)) "Overlay Permission Granted" else "Overlay Permission Required"
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    val mediaProjectionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            viewModel.startOverlayService(result.resultCode, result.data!!)
        } else {
            Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else true

        if (audioGranted && notificationGranted) {
             if (!android.provider.Settings.canDrawOverlays(context)) {
                 overlayPermissionLauncher.launch(Intent(
                     android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                     "package:${context.packageName}".toUri()
                 ))
                 return@rememberLauncherForActivityResult
             }
            val mpManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjectionLauncher.launch(mpManager.createScreenCaptureIntent())
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
                    title = { Text("Rekam Audio") },
                    actions = {
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = {
                        if (uiState is RecordingUiState.Recording) {
                            viewModel.stopRecordingService()
                        } else {
                            val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO).apply {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
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
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (recordings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No recordings yet")
                }
            } else {
                LazyColumn {
                    items(recordings) { recording ->
                        RecordingItem(
                            recording = recording,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedIds.contains(recording.id),
                            isPlaying = playbackState == recording.id,
                            onDelete = { viewModel.deleteRecording(it) },
                            onRename = { setRecordingToRename(it); setShowRenameDialog(true) },
                            onShare = { viewModel.shareRecording(it) },
                            onLongClick = { viewModel.toggleSelection(recording.id) },
                            onClick = {
                                if (isSelectionMode) viewModel.toggleSelection(recording.id) else viewModel.playRecording(recording)
                            }
                        )
                    }
                }
            }
        }
    }
}
