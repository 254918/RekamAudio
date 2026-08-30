package com.example.rekamaudio.data.repository

import com.example.rekamaudio.data.model.AudioQuality
import com.example.rekamaudio.data.model.Mp3Bitrate
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val audioQuality: Flow<AudioQuality>
    suspend fun setAudioQuality(quality: AudioQuality)
    val mp3Bitrate: Flow<Mp3Bitrate>
    suspend fun setMp3Bitrate(bitrate: Mp3Bitrate)
}
