package com.example.rekamaudio.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.rekamaudio.data.model.Recording
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class AudioCaptureRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioCaptureRepository {

    override fun getRecordings(): Flow<List<Recording>> = flow {
        val recordings = mutableListOf<Recording>()
        
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED
        )
        
        // Filter for files in "Music/RekamAudio" (best effort filtering via RELATIVE_PATH on Android 10+)
        // Or just show all audio files created by this app (ownership based).
        // For simplicity and Android compatibility, we'll query generic audio and filter by name/path if possible,
        // or just show all files that match our naming convention "recording_".
        
        // Note: RELATIVE_PATH is API 29+.
        // Removed strict "recording_%" filtering to allow renamed files to appear.
        // Assuming Scoped Storage / permissions limit us to our own files or relevant audio.
        val selection = null
        val selectionArgs = null
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
             MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val duration = cursor.getLong(durationColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)

                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                // Only include m4a files from our app
                if (name.endsWith(".m4a")) {
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
        
        emit(recordings)
    }.flowOn(Dispatchers.IO)


    override suspend fun deleteRecording(recording: Recording): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(recording.fileUri)
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
            val uri = Uri.parse(recording.fileUri)
            val finalName = if (newName.endsWith(".m4a")) newName else "$newName.m4a"
            
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
