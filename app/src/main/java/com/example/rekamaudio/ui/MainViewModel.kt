package com.example.rekamaudio.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rekamaudio.data.model.Recording
import com.example.rekamaudio.data.repository.AudioCaptureRepository
import com.example.rekamaudio.data.repository.SettingsRepository
import com.example.rekamaudio.player.AudioPlayer
import com.example.rekamaudio.service.AudioCaptureService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AudioCaptureRepository,
    private val settingsRepository: SettingsRepository,
    private val audioPlayer: AudioPlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecordingUiState>(RecordingUiState.Idle)
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()

    private val _recordings = MutableStateFlow<List<Recording>>(emptyList())
    val recordings = _recordings.asStateFlow()

    private val _playbackState = MutableStateFlow<Long?>(null)
    val playbackState: StateFlow<Long?> = _playbackState.asStateFlow()

    private val _events = MutableSharedFlow<MainEvent>()
    val events = _events.asSharedFlow()

    private val _selectedRecordingIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedRecordingIds = _selectedRecordingIds.asStateFlow()

    init {
        observeRecordings()
    }

    private fun observeRecordings() {
        viewModelScope.launch {
            repository.getRecordings()
                .catch { e -> _uiState.value = RecordingUiState.Error(e.message ?: "Unknown error") }
                .collect { list -> _recordings.value = list }
        }
    }

    fun playRecording(recording: Recording) {
        if (_playbackState.value == recording.id) {
            stopPlayback()
        } else {
            _playbackState.value = recording.id
            audioPlayer.playFile(recording.fileUri) {
                _playbackState.value = null
            }
        }
    }

    fun stopPlayback() {
        audioPlayer.stop()
        _playbackState.value = null
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }



    fun startOverlayService(resultCode: Int, data: Intent) {
        stopPlayback()
        viewModelScope.launch {
            val quality = settingsRepository.audioQuality.first()
            _events.emit(MainEvent.StartOverlayService(resultCode, data, quality))
        }
    }

    fun stopRecordingService() {
        viewModelScope.launch {
            _events.emit(MainEvent.StopRecordingService(AudioCaptureService.ACTION_STOP))
            _uiState.value = RecordingUiState.Idle
        }
    }

    fun deleteRecording(recording: Recording) {
        if (_playbackState.value == recording.id) stopPlayback()
        viewModelScope.launch {
            repository.deleteRecording(recording)
                .onFailure { _uiState.value = RecordingUiState.Error(it.message ?: "Failed to delete") }
        }
    }

    fun renameRecording(recording: Recording, newName: String) {
        viewModelScope.launch {
            repository.renameRecording(recording, newName)
                .onFailure { _uiState.value = RecordingUiState.Error(it.message ?: "Failed to rename") }
        }
    }

    fun shareRecording(recording: Recording) {
        viewModelScope.launch {
            _events.emit(MainEvent.ShareRecording(recording.fileUri))
        }
    }

    fun toggleSelection(recordingId: Long) {
        _selectedRecordingIds.update { current ->
            if (current.contains(recordingId)) current - recordingId else current + recordingId
        }
    }

    fun clearSelection() {
        _selectedRecordingIds.value = emptySet()
    }

    fun deleteSelectedRecordings() {
        val idsToDelete = _selectedRecordingIds.value
        viewModelScope.launch {
            _recordings.value.filter { it.id in idsToDelete }.forEach {
                if (_playbackState.value == it.id) stopPlayback()
                repository.deleteRecording(it)
            }
            clearSelection()
        }
    }

    fun shareSelectedRecordings() {
        val idsToShare = _selectedRecordingIds.value
        val uris = _recordings.value
            .filter { it.id in idsToShare }
            .map { it.fileUri }
        
        viewModelScope.launch {
            _events.emit(MainEvent.ShareSelected(uris))
        }
    }
}
