package com.example.rekamaudio.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.example.rekamaudio.data.model.Recording
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import androidx.core.net.toUri

class AudioCaptureRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : AudioCaptureRepository {

    override fun getRecordings(): Flow<List<Recording>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(queryRecordings())
            }
        }

        // Initial query
        trySend(queryRecordings())

        // Register observer
        val collection =
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

        context.contentResolver.registerContentObserver(collection, true, observer)

        awaitClose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }.flowOn(Dispatchers.IO)


    private fun queryRecordings(): List<Recording> {
        val recordings = mutableListOf<Recording>()
        
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.RELATIVE_PATH
        )

        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        val collection =
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val duration = cursor.getLong(durationColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)
                val relativePath = cursor.getString(pathColumn)

                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                // Only include files written by our app (Music/RekamAudio) with a supported extension
                val isSupportedFormat = name.endsWith(".m4a") ||
                    name.endsWith(".wav") ||
                    name.endsWith(".mp3")

                if (isSupportedFormat && relativePath.contains("RekamAudio")) {
                     recordings.add(
                        Recording(
                            id = id,
                            fileName = name,
                            fileUri = contentUri.toString(),
                            durationMs = duration,
                            createdAt = dateAdded * 1000 // DATE_ADDED is in seconds
                        )
                    )
                }
            }
        }
        return recordings
    }

    override suspend fun deleteRecording(recording: Recording): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val uri = recording.fileUri.toUri()
            val rowsDeleted = context.contentResolver.delete(uri, null, null)
            if (rowsDeleted > 0) {
                Result.success(true)
            } else {
                Result.failure(Exception("Could not delete file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun renameRecording(recording: Recording, newName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val uri = recording.fileUri.toUri()
            val extension = recording.fileName.substringAfterLast('.', "m4a")
            val finalName = if (newName.endsWith(".$extension")) newName else "$newName.$extension"
            
            val contentValues = android.content.ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, finalName)
            }

            val rowsUpdated = context.contentResolver.update(uri, contentValues, null, null)
            
            if (rowsUpdated > 0) {
                Result.success(true)
            } else {
                Result.failure(Exception("Could not rename file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
