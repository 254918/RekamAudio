package com.example.rekamaudio.player

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var mediaPlayer: MediaPlayer? = null

    fun playFile(uri: String, onCompletion: () -> Unit) {
        stop()
        
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(context, Uri.parse(uri))
                prepare()
                start()
                setOnCompletionListener { 
                    onCompletion()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onCompletion()
            }
        }
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun release() {
        stop()
    }
}
