package com.example.rekamaudio.ui

sealed interface RecordingUiState {
    data object Idle : RecordingUiState
    data object Recording : RecordingUiState
    data class Error(val message: String) : RecordingUiState
}
