package com.example.rekamaudio.data.repository

import com.example.rekamaudio.data.model.Recording
import kotlinx.coroutines.flow.Flow
interface AudioCaptureRepository {
    fun getRecordings(): Flow<List<Recording>>
    suspend fun deleteRecording(recording: Recording): Result<Boolean>
    suspend fun renameRecording(recording: Recording, newName: String): Result<Boolean>
}
