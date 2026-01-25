package com.example.rekamaudio.ui

import com.example.rekamaudio.data.model.Recording

sealed interface RecordingUiState {
    data object Idle : RecordingUiState
    data object Recording : RecordingUiState
    data class Success(val recordings: List<Recording>) : RecordingUiState
    data class Error(val message: String) : RecordingUiState
}
