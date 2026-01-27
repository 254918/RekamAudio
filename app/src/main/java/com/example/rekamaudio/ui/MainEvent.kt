package com.example.rekamaudio.ui

import android.content.Intent
import com.example.rekamaudio.data.model.AudioQuality

sealed interface MainEvent {
    data class StartRecordingService(val resultCode: Int, val data: Intent, val quality: AudioQuality) : MainEvent
    data class StartOverlayService(val resultCode: Int, val data: Intent, val quality: AudioQuality) : MainEvent
    data class StopRecordingService(val action: String) : MainEvent
    data class ShareRecording(val uri: String) : MainEvent
    data class ShareSelected(val uris: List<String>) : MainEvent
    data class Error(val message: String) : MainEvent
}
