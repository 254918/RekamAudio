package com.example.rekamaudio.player

import android.content.Context
import android.media.MediaPlayer
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackProgress(
    val positionMs: Int = 0,
    val durationMs: Int = 0
) {
    val fraction: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

@Singleton
class AudioPlayer @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    private val _progress = MutableStateFlow(PlaybackProgress())
    val progress: StateFlow<PlaybackProgress> = _progress.asStateFlow()

    fun playFile(uri: String, onCompletion: () -> Unit) {
        stop()

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(context, uri.toUri())
                setOnPreparedListener { player ->
                    player.start()
                    startProgressUpdates()
                }
                setOnCompletionListener {
                    stopProgressUpdates()
                    onCompletion()
                }
                setOnErrorListener { _, _, _ ->
                    stopProgressUpdates()
                    onCompletion()
                    true
                }
                prepareAsync()
            } catch (e: Exception) {
                e.printStackTrace()
                onCompletion()
            }
        }
    }

    fun seekTo(positionMs: Int) {
        val player = mediaPlayer ?: return
        try {
            val duration = player.duration
            val clamped = positionMs.coerceIn(0, if (duration > 0) duration else positionMs)
            player.seekTo(clamped)
            _progress.value = _progress.value.copy(positionMs = clamped)
        } catch (_: IllegalStateException) {
            // Player not in a seekable state yet – ignore
        }
    }

    fun stop() {
        stopProgressUpdates()
        mediaPlayer?.run {
            try {
                if (isPlaying) stop()
            } catch (_: IllegalStateException) {
            }
            release()
        }
        mediaPlayer = null
        _progress.value = PlaybackProgress()
    }

    fun release() {
        stop()
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { player ->
                    try {
                        _progress.value = PlaybackProgress(player.currentPosition, player.duration)
                    } catch (_: IllegalStateException) {
                    }
                }
                delay(PROGRESS_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    companion object {
        private const val PROGRESS_UPDATE_INTERVAL_MS = 200L
    }
}
