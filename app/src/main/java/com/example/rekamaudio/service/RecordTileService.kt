package com.example.rekamaudio.service

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.rekamaudio.R
import com.example.rekamaudio.ui.PermissionActivity

@RequiresApi(Build.VERSION_CODES.N)
class RecordTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val isRecording = getSharedPreferences(AudioCaptureService.PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(AudioCaptureService.KEY_IS_RECORDING, false)

        if (isRecording) {
            // Stop Recording
            val intent = Intent(this, AudioCaptureService::class.java).apply {
                action = AudioCaptureService.ACTION_STOP
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            // Optimistically update state
            updateTileState(false)
        } else {
            // Start Recording -> Check Permission via Activity
            val intent = Intent(this, PermissionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val pendingIntent = android.app.PendingIntent.getActivity(
                this,
                0,
                intent,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(pendingIntent)
            } else {
                 // Fallback for older versions if necessary, though PendingIntent works for all
                 // But strictly speaking, the Intent version was the standard before.
                 // However, to be safe and clean, let's try the PendingIntent one first
                 // If the API level is < 34, un-deprecated method takes Intent.
                 // Actually, let's check SDK version to decide.
                 startActivityAndCollapse(intent)
            }
        }
    }

    private fun updateTileState(forceState: Boolean? = null) {
        val qsTile = qsTile ?: return

        val isRecording = forceState ?: getSharedPreferences(AudioCaptureService.PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(AudioCaptureService.KEY_IS_RECORDING, false)

        if (isRecording) {
            qsTile.state = Tile.STATE_ACTIVE
            qsTile.icon = Icon.createWithResource(this, R.drawable.ic_stop_recording)
            qsTile.label = getString(R.string.stop_recording_label)
        } else {
            qsTile.state = Tile.STATE_INACTIVE
            qsTile.icon = Icon.createWithResource(this, R.drawable.ic_mic)
            qsTile.label = getString(R.string.start_recording_label)
        }

        qsTile.updateTile()
    }
}
