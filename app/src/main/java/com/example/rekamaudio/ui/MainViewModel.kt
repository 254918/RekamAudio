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
}
