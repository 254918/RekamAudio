package com.example.rekamaudio.data.repository

import com.example.rekamaudio.data.model.AudioQuality
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val audioQuality: Flow<AudioQuality>
    suspend fun setAudioQuality(quality: AudioQuality)
}
