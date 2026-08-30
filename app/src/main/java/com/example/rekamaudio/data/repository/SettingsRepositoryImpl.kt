package com.example.rekamaudio.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.rekamaudio.data.model.AudioQuality
import com.example.rekamaudio.data.model.Mp3Bitrate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private val AUDIO_QUALITY_KEY = stringPreferencesKey("audio_quality")
    private val MP3_BITRATE_KEY = stringPreferencesKey("mp3_bitrate")

    override val audioQuality: Flow<AudioQuality> = context.dataStore.data
        .map { preferences ->
            val qualityName = preferences[AUDIO_QUALITY_KEY] ?: AudioQuality.MEDIUM_QUALITY_M4A.name
            try {
                AudioQuality.valueOf(qualityName)
            } catch (e: IllegalArgumentException) {
                AudioQuality.MEDIUM_QUALITY_M4A
            }
        }

    override suspend fun setAudioQuality(quality: AudioQuality) {
        context.dataStore.edit { preferences ->
            preferences[AUDIO_QUALITY_KEY] = quality.name
        }
    }

    override val mp3Bitrate: Flow<Mp3Bitrate> = context.dataStore.data
        .map { preferences ->
            val bitrateName = preferences[MP3_BITRATE_KEY] ?: Mp3Bitrate.BITRATE_192.name
            try {
                Mp3Bitrate.valueOf(bitrateName)
            } catch (e: IllegalArgumentException) {
                Mp3Bitrate.BITRATE_192
            }
        }

    override suspend fun setMp3Bitrate(bitrate: Mp3Bitrate) {
        context.dataStore.edit { preferences ->
            preferences[MP3_BITRATE_KEY] = bitrate.name
        }
    }
}
