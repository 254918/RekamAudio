package com.example.rekamaudio.player

import android.content.Context
import android.media.MediaPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

@Singleton
class AudioPlayer @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private var mediaPlayer: MediaPlayer? = null

    fun playFile(uri: String, onCompletion: () -> Unit) {
        stop()
        
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(context, uri.toUri())
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
