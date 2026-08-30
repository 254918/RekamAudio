package com.example.rekamaudio.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.example.rekamaudio.R
import com.example.rekamaudio.data.model.Mp3Bitrate
import com.example.rekamaudio.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import com.example.rekamaudio.data.model.AudioQuality
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import android.media.AudioFormat.CHANNEL_IN_STEREO
import android.media.AudioFormat.ENCODING_PCM_16BIT

@AndroidEntryPoint
class AudioCaptureService : Service() {

    @Inject lateinit var settingsRepository: SettingsRepository

    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var recordingJob: Job? = null
    private var isRecording = false
    private var currentFileUri: Uri? = null
    private var audioQuality: AudioQuality = AudioQuality.MEDIUM_QUALITY_M4A

    private lateinit var overlayManager: OverlayManager
    private var savedResultCode: Int = 0
    private var savedResultData: Intent? = null
    private var isOverlayMode = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        overlayManager = OverlayManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        when (intent.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }

                if (resultCode != 0 && resultData != null) {
                    savedResultCode = resultCode
                    savedResultData = resultData
                    
                    startForegroundServiceNotification()
                    startRecording(resultCode, resultData)
                    overlayManager.updateState(true) // Update overlay if active
                }
            }
            ACTION_STOP -> {
                stopRecording()
                overlayManager.updateState(false) // Update overlay
                if (!isOverlayMode) {
                    stopSelf()
                }
            }
            ACTION_SHOW_OVERLAY -> {
                 isOverlayMode = true
                 val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                 val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }
                
                if (resultCode != 0 && resultData != null) {
                    savedResultCode = resultCode
                    savedResultData = resultData
                }
                
                val qualityName = intent.getStringExtra(EXTRA_AUDIO_QUALITY)
                if (qualityName != null) {
                    try {
                        audioQuality = AudioQuality.valueOf(qualityName)
                    } catch (e: Exception) {
                        Log.e("AudioCaptureService", "Invalid AudioQuality: $qualityName")
                    }
                }
                
                startForegroundServiceNotification()
                
                overlayManager.showOverlay(
                    onRecordClick = {
                        if (savedResultCode != 0 && savedResultData != null) {
                            startRecording(savedResultCode, savedResultData!!)
                        } else {
                            Log.e("AudioCaptureService", "Missing MediaProjection Permission Data")
                        }
                    },
                    onStopClick = {
                        stopRecording()
                    },
                    onCloseClick = {
                        isOverlayMode = false
                        stopRecording()
                        overlayManager.removeOverlay()
                        stopSelf()
                    },
                    isRecording = isRecording
                )
            }
            ACTION_DISMISS_OVERLAY -> {
                isOverlayMode = false
                stopRecording()
                overlayManager.removeOverlay()
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }
    
    private fun startForegroundServiceNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
             startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
             startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        }
    }

    private fun startRecording(resultCode: Int, resultData: Intent) {
        if (isRecording) {
            // Already recording
            return
        }

        mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, resultData)
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                stopRecording()
                overlayManager.updateState(false)
            }
        }, null)

        // Launch into background to allow delay and offload setup
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // CRITICAL FIX: Give the system time to register the projection
                // before asking for AudioPlaybackCapture. This prevents the "silent audio" race condition.
                kotlinx.coroutines.delay(500)

                val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
                    .addMatchingUsage(AudioAttributes.USAGE_GAME)
                    .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    // REMOVED .excludeUid() to allow self-recording for testing or internal audio general testing
                    .build()

                val sampleRate = 48000 // Native android sample rate
                val channelConfig = AudioFormat.CHANNEL_IN_STEREO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2

                Log.d("AudioCaptureService", "Creating AudioRecord with bufferSize: $bufferSize")

                if (ContextCompat.checkSelfPermission(this@AudioCaptureService, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.e("AudioCaptureService", "Recording permission not granted")
                    stopRecording() // This is running on IO thread
                    withContext(Dispatchers.Main) {
                        overlayManager.updateState(false)
                    }
                    if (!isOverlayMode) stopSelf()
                    return@launch
                }

                audioRecord = AudioRecord.Builder()
                    .setAudioPlaybackCaptureConfig(config)
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(audioFormat)
                            .setSampleRate(sampleRate)
                            .setChannelMask(channelConfig)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .build()

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e("AudioCaptureService", "AudioRecord failed to initialize")
                    stopRecording()
                    withContext(Dispatchers.Main) {
                        overlayManager.updateState(false)
                    }
                    if (!isOverlayMode) stopSelf()
                    return@launch
                }

                audioRecord?.startRecording()
                isRecording = true
                getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_IS_RECORDING, true)
                    .apply()

                Log.d("AudioCaptureService", "AudioRecord started successfully")
                
                withContext(Dispatchers.Main) {
                    overlayManager.updateState(true)
                }

                val mp3Bitrate = settingsRepository.mp3Bitrate.first()

                recordingJob = launch {
                    when (audioQuality) {
                        AudioQuality.HIGH_QUALITY_WAV -> recordWavAudio(bufferSize)
                        AudioQuality.COMPATIBLE_QUALITY_MP3 -> recordMp3Audio(bufferSize, mp3Bitrate)
                        AudioQuality.MEDIUM_QUALITY_M4A -> recordAacAudio(bufferSize)
                    }
                }
            } catch (e: Exception) {
                Log.e("AudioCaptureService", "Error starting recording", e)
                stopRecording()
                withContext(Dispatchers.Main) {
                    overlayManager.updateState(false)
                }
                if (!isOverlayMode) stopSelf()
            }
        }
    }

    private fun recordAacAudio(bufferSize: Int) {
        val uri = createMediaStoreEntry(isWav = false) ?: return
        currentFileUri = uri
        
        var pfd: ParcelFileDescriptor? = null
        
        try {
             pfd = contentResolver.openFileDescriptor(uri, "w")
             val fd = pfd?.fileDescriptor ?: return

            // Setup MediaCodec setup (AAC Encoder)
            val mimeType = MediaFormat.MIMETYPE_AUDIO_AAC
            val format = MediaFormat.createAudioFormat(mimeType, SAMPLE_RATE, CHANNEL_COUNT)
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            format.setInteger(MediaFormat.KEY_BIT_RATE, 192000) // 192kbps
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            
            mediaCodec = MediaCodec.createEncoderByType(mimeType)
            mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            mediaCodec?.start()

            // Setup MediaMuxer with FileDescriptor
            mediaMuxer = MediaMuxer(fd, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            
            val bufferInfo = MediaCodec.BufferInfo()
            var trackIndex = -1
            var isMuxerStarted = false
            
            val inputBuffer = ByteArray(bufferSize)
            var totalBytesRead = 0L
            var lastLogTime = 0L

            while (isRecording) {
                // Read PCM data
                val readResult = audioRecord?.read(inputBuffer, 0, bufferSize) ?: 0
                
                if (readResult > 0) {
                    // Diagnostic Logging (every 2 seconds)
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastLogTime > 2000) {
                         var maxAmp = 0
                         for (i in 0 until readResult step 2) {
                             if (i + 1 < readResult) {
                                  // Convert 2 bytes to 16-bit sample (Little Endian)
                                  val sample = ((inputBuffer[i+1].toInt() shl 8) or (inputBuffer[i].toInt() and 0xFF)).toShort()
                                  val absSample = abs(sample.toInt())
                                  if (absSample > maxAmp) maxAmp = absSample
                             }
                         }
                         Log.d("AudioCheck", "Recorded $readResult bytes, Max Amplitude: $maxAmp")
                         lastLogTime = currentTime
                    }

                    val inputBufferIndex = mediaCodec?.dequeueInputBuffer(10000) ?: -1
                    if (inputBufferIndex >= 0) {
                        val codecInputBuffer = mediaCodec?.getInputBuffer(inputBufferIndex)
                        codecInputBuffer?.clear()
                        codecInputBuffer?.put(inputBuffer, 0, readResult)
                        
                        // Calculate PTS based on samples processed
                        // PTS (us) = (TotalSamples / SampleRate) * 1,000,000
                        // TotalSamples = TotalBytes / (ChannelCount * 2 bytes/sample)
                        val presentationTimeUs = (totalBytesRead * 1_000_000) / (SAMPLE_RATE * CHANNEL_COUNT * 2)
                        
                        mediaCodec?.queueInputBuffer(
                            inputBufferIndex,
                            0,
                            readResult,
                            presentationTimeUs,
                            0
                        )
                        
                        totalBytesRead += readResult
                    }
                }

                // Retrieve encoded data
                var outputBufferIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, 0) ?: -1
                while (outputBufferIndex >= 0) {
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }

                    if (bufferInfo.size != 0) {
                         if (!isMuxerStarted) {
                            val newFormat = mediaCodec?.outputFormat
                            trackIndex = mediaMuxer?.addTrack(newFormat!!) ?: -1
                            mediaMuxer?.start()
                            isMuxerStarted = true
                        }
                        
                        val encodedData = mediaCodec?.getOutputBuffer(outputBufferIndex)
                        if (encodedData != null) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            mediaMuxer?.writeSampleData(trackIndex, encodedData, bufferInfo)
                        }
                    }

                    mediaCodec?.releaseOutputBuffer(outputBufferIndex, false)
                    outputBufferIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, 0) ?: -1
                }
            }
        } catch (e: Exception) {
            Log.e("AudioCaptureService", "Error encoding audio", e)
        } finally {
             try {
                mediaCodec?.stop()
                mediaCodec?.release()
                mediaMuxer?.stop()
                mediaMuxer?.release()
                pfd?.close()
                if (isRecording) { // If stopped properly, otherwise maybe error
                    finalizeMediaStoreEntry(uri)
                } else {
                     finalizeMediaStoreEntry(uri)
                }
            } catch (e: Exception) {
                Log.e("AudioCaptureService", "Error releasing resources", e)
            }
            mediaCodec = null
            mediaMuxer = null
            currentFileUri = null
        }
    }

    private fun recordWavAudio(bufferSize: Int) {
        val uri = createMediaStoreEntry(isWav = true) ?: return
        currentFileUri = uri
        
        var pfd: ParcelFileDescriptor? = null
        var outputStream: FileOutputStream? = null
        
        try {
            pfd = contentResolver.openFileDescriptor(uri, "w")
            val fd = pfd?.fileDescriptor ?: return
            outputStream = FileOutputStream(fd)
            val channel = outputStream.channel

            // Write 44 bytes placeholder header
            val header = ByteArray(44)
            outputStream.write(header)

            val buffer = ByteArray(bufferSize)
            var totalBytesRead = 0L
            
            while (isRecording) {
                val readResult = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                if (readResult > 0) {
                    outputStream.write(buffer, 0, readResult)
                    totalBytesRead += readResult.toLong()
                }
            }
            
            // Go back and write real header
            channel.position(0)
            writeWavHeader(channel, totalBytesRead, SAMPLE_RATE, CHANNEL_COUNT)
            
        } catch (e: Exception) {
             Log.e("AudioCaptureService", "Error recording WAV", e)
        } finally {
            try {
                outputStream?.close()
                pfd?.close()
                finalizeMediaStoreEntry(uri)
            } catch (e: Exception) {
                Log.e("AudioCaptureService", "Error closing resources", e)
            }
        }
    }

    /**
     * Records raw PCM to a temp file while recording, then encodes it to MP3
     * with ffmpeg (libmp3lame) after stopping and imports the result into MediaStore.
     */
    private fun recordMp3Audio(bufferSize: Int, bitrate: Mp3Bitrate) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val pcmFile = File(cacheDir, "rekam_tmp_$timestamp.pcm")
        val mp3File = File(cacheDir, "rekam_tmp_$timestamp.mp3")

        try {
            FileOutputStream(pcmFile).use { outputStream ->
                val buffer = ByteArray(bufferSize)
                while (isRecording) {
                    val readResult = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                    if (readResult > 0) {
                        outputStream.write(buffer, 0, readResult)
                    }
                }
            }

            // Encode PCM -> MP3. Blocking call, immune to coroutine cancellation,
            // so the encode always finishes even though stopRecording() cancels the job.
            val command = "-y -f s16le -ar $SAMPLE_RATE -ac $CHANNEL_COUNT " +
                "-i ${pcmFile.absolutePath} -codec:a libmp3lame -b:a ${bitrate.bitsPerSecond} ${mp3File.absolutePath}"
            val session = FFmpegKit.execute(command)

            if (!ReturnCode.isSuccess(session.returnCode)) {
                Log.e("AudioCaptureService", "MP3 encode failed: rc=${session.returnCode} ${session.getAllLogsAsString()}")
                return
            }

            copyFileToMediaStore(mp3File, "recording_$timestamp.mp3", "audio/mpeg")
        } catch (e: Exception) {
            Log.e("AudioCaptureService", "Error recording MP3", e)
        } finally {
            pcmFile.delete()
            mp3File.delete()
        }
    }

    private fun copyFileToMediaStore(file: File, fileName: String, mimeType: String) {
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/RekamAudio")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }

        val collection =
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val uri = contentResolver.insert(collection, values) ?: run {
            Log.e("AudioCaptureService", "Failed to create MediaStore entry for $fileName")
            return
        }

        try {
            contentResolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            }
            val doneValues = ContentValues().apply {
                put(MediaStore.Audio.Media.IS_PENDING, 0)
            }
            contentResolver.update(uri, doneValues, null, null)
        } catch (e: Exception) {
            Log.e("AudioCaptureService", "Failed to write $fileName to MediaStore", e)
            contentResolver.delete(uri, null, null)
        }
    }

    private fun writeWavHeader(channel: FileChannel, totalAudioLen: Long, sampleRate: Int, channels: Int) {
        val totalDataLen = totalAudioLen + 36
        val byteRate = (sampleRate * channels * 16 / 8).toLong()
        
        val header = ByteArray(44)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        buffer.put("RIFF".toByteArray())
        buffer.putInt(totalDataLen.toInt())
        buffer.put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16) // Subchunk1Size
        buffer.putShort(1) // AudioFormat 1 = PCM
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate.toInt())
        buffer.putShort((channels * 16 / 8).toShort()) // BlockAlign
        buffer.putShort(16) // BitsPerSample
        buffer.put("data".toByteArray())
        buffer.putInt(totalAudioLen.toInt())
        
        channel.write(ByteBuffer.wrap(header))
    }

    private fun createMediaStoreEntry(isWav: Boolean): Uri? {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = if (isWav) "recording_$timestamp.wav" else "recording_$timestamp.m4a"
        val mimeType = if (isWav) "audio/wav" else "audio/mp4a-latm"
        
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/RekamAudio")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        
        val collection =
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        return contentResolver.insert(collection, values)
    }

    private fun finalizeMediaStoreEntry(uri: Uri) {
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.IS_PENDING, 0)
        }
        contentResolver.update(uri, values, null, null)
    }

    private fun stopRecording() {
        isRecording = false
        getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IS_RECORDING, false)
            .apply()

        recordingJob?.cancel()
        
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
             Log.e("AudioCaptureService", "Error stopping audioRecord", e)
        }
        audioRecord = null
        
        mediaProjection?.stop()
        mediaProjection = null
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayManager.removeOverlay()
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_recording_title))
            .setContentText(getString(R.string.notification_recording_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "AudioCaptureChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        const val ACTION_SHOW_OVERLAY = "SHOW_OVERLAY"
        const val ACTION_DISMISS_OVERLAY = "DISMISS_OVERLAY" // New Action
        const val EXTRA_RESULT_CODE = "RESULT_CODE"
        const val EXTRA_RESULT_DATA = "RESULT_DATA"
        const val SAMPLE_RATE = 48000
        const val CHANNEL_COUNT = 2
        const val EXTRA_AUDIO_QUALITY = "AUDIO_QUALITY"
        
        const val PREFS_NAME = "rekam_audio_prefs"
        const val KEY_IS_RECORDING = "is_recording"
    }
}
