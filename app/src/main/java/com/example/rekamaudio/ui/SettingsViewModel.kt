package com.example.rekamaudio.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rekamaudio.data.model.AudioQuality
import com.example.rekamaudio.data.model.Mp3Bitrate
import com.example.rekamaudio.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val audioQuality: StateFlow<AudioQuality> = settingsRepository.audioQuality
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AudioQuality.MEDIUM_QUALITY_M4A
        )

    fun setAudioQuality(quality: AudioQuality) {
        viewModelScope.launch {
            settingsRepository.setAudioQuality(quality)
        }
    }

    val mp3Bitrate: StateFlow<Mp3Bitrate> = settingsRepository.mp3Bitrate
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Mp3Bitrate.BITRATE_192
        )

    fun setMp3Bitrate(bitrate: Mp3Bitrate) {
        viewModelScope.launch {
            settingsRepository.setMp3Bitrate(bitrate)
        }
    }

    val streamingEncoding: StateFlow<Boolean> = settingsRepository.streamingEncoding
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun setStreamingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setStreamingEncoding(enabled)
            // When streaming is enabled, auto-downgrade 320k to 192k (not supported in streaming)
            if (enabled) {
                val currentBitrate = settingsRepository.mp3Bitrate.first()
                if (!currentBitrate.streamingSupported) {
                    settingsRepository.setMp3Bitrate(Mp3Bitrate.BITRATE_192)
                }
            }
        }
    }
}
