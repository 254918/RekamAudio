package com.example.rekamaudio.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rekamaudio.data.repository.AudioCaptureRepository
import com.example.rekamaudio.service.AudioCaptureService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AudioCaptureRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecordingUiState>(RecordingUiState.Idle)
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()

    private val _recordings = MutableStateFlow<List<com.example.rekamaudio.data.model.Recording>>(emptyList())
    val recordings = _recordings.asStateFlow()

    init {
        loadRecordings()
    }

    private fun loadRecordings() {
        viewModelScope.launch {
            repository.getRecordings()
                .catch { e -> _uiState.value = RecordingUiState.Error(e.message ?: "Unknown error") }
                .collect { list ->
                    _recordings.value = list
                }
        }
    }

    fun startRecordingService(resultCode: Int, data: Intent) {
        val intent = Intent(context, AudioCaptureService::class.java).apply {
            action = AudioCaptureService.ACTION_START
            putExtra(AudioCaptureService.EXTRA_RESULT_CODE, resultCode)
            putExtra(AudioCaptureService.EXTRA_RESULT_DATA, data)
        }
        context.startForegroundService(intent)
        _uiState.value = RecordingUiState.Recording
    }

    fun stopRecordingService() {
        val intent = Intent(context, AudioCaptureService::class.java).apply {
            action = AudioCaptureService.ACTION_STOP
        }
        context.startService(intent)
        _uiState.value = RecordingUiState.Idle
        // Reload recordings after a short delay to allow file closing
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            loadRecordings()
        }
    }

    fun deleteRecording(recording: com.example.rekamaudio.data.model.Recording) {
        viewModelScope.launch {
            repository.deleteRecording(recording)
                .onSuccess { loadRecordings() }
                .onFailure { _uiState.value = RecordingUiState.Error(it.message ?: "Failed to delete") }
        }
    }

    fun renameRecording(recording: com.example.rekamaudio.data.model.Recording, newName: String) {
        viewModelScope.launch {
            repository.renameRecording(recording, newName)
                .onSuccess { loadRecordings() }
                .onFailure { _uiState.value = RecordingUiState.Error(it.message ?: "Failed to rename") }
        }
    }

    fun shareRecording(recording: com.example.rekamaudio.data.model.Recording) {
        val uri = android.net.Uri.parse(recording.fileUri)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Recording").apply {
             addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    // Selection Mode Logic
    private val _selectedRecordingIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedRecordingIds = _selectedRecordingIds.asStateFlow()

    fun toggleSelection(recordingId: Long) {
        val current = _selectedRecordingIds.value
        if (current.contains(recordingId)) {
            _selectedRecordingIds.value = current - recordingId
        } else {
            _selectedRecordingIds.value = current + recordingId
        }
    }

    fun clearSelection() {
        _selectedRecordingIds.value = emptySet()
    }

    fun deleteSelectedRecordings() {
        val idsToDelete = _selectedRecordingIds.value
        viewModelScope.launch {
            val recordingsToDelete = _recordings.value.filter { it.id in idsToDelete }
            recordingsToDelete.forEach { repository.deleteRecording(it) }
            clearSelection()
            loadRecordings()
        }
    }

    fun shareSelectedRecordings() {
        val idsToShare = _selectedRecordingIds.value
        val recordingsToShare = _recordings.value.filter { it.id in idsToShare }

        val uris = ArrayList<android.net.Uri>()
        recordingsToShare.forEach { uris.add(android.net.Uri.parse(it.fileUri)) }

        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "audio/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Recordings").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
