package com.example.rekamaudio.service

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.example.rekamaudio.R

class OverlayManager(private val context: Context) {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isOverlayShown = false

    private var overlayRecordButton: ImageView? = null
    private var overlayCloseButton: ImageView? = null

    fun showOverlay(onRecordClick: () -> Unit, onStopClick: () -> Unit, onCloseClick: () -> Unit, isRecording: Boolean) {
        if (isOverlayShown) {
            updateOverlayState(isRecording)
            return
        }

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 100

        overlayView = createOverlayView(onRecordClick, onStopClick, onCloseClick)
        updateOverlayState(isRecording)

        try {
            windowManager?.addView(overlayView, params)
            isOverlayShown = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeOverlay() {
        if (isOverlayShown && overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
                isOverlayShown = false
                overlayView = null
                overlayRecordButton = null
                overlayCloseButton = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createOverlayView(onRecordClick: () -> Unit, onStopClick: () -> Unit, onCloseClick: () -> Unit): View {
        val rootLayout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#80000000")) // Semi-transparent black
                cornerRadius = 32f
            }
            setPadding(16, 16, 16, 16)
            elevation = 10f
        }

        // Record/Stop Button
        val recordButton = ImageView(context).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(100, 100).apply {
                marginEnd = 16
            }
            setPadding(20, 20, 20, 20)
            background = ContextCompat.getDrawable(context, R.drawable.ic_launcher_background)?.constantState?.newDrawable()?.mutate()
            // Make circular
             background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(android.graphics.Color.WHITE)
            }
            elevation = 4f
        }
        
        recordButton.setOnClickListener {
            val tag = recordButton.tag as? Boolean ?: false
            if (tag) {
                onStopClick()
            } else {
                onRecordClick()
            }
        }
        overlayRecordButton = recordButton

        // Close Button
        val closeButton = ImageView(context).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(80, 80).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(android.graphics.Color.WHITE)
            setPadding(16, 16, 16, 16)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(android.graphics.Color.DKGRAY)
            }
        }
        closeButton.setOnClickListener {
            onCloseClick()
        }
        overlayCloseButton = closeButton

        rootLayout.addView(recordButton)
        rootLayout.addView(closeButton)
        
        return rootLayout
    }
    
    fun updateState(isRecording: Boolean) {
        overlayRecordButton?.tag = isRecording // Store state in tag
        updateOverlayState(isRecording)
    }

    private fun updateOverlayState(isRecording: Boolean) {
        if (isRecording) {
            overlayRecordButton?.setImageResource(android.R.drawable.ic_media_pause)
            overlayRecordButton?.background?.setTint(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            overlayCloseButton?.visibility = View.GONE // Hide close button while recording
        } else {
            overlayRecordButton?.setImageResource(android.R.drawable.ic_btn_speak_now)
            overlayRecordButton?.background?.setTint(ContextCompat.getColor(context, android.R.color.holo_green_dark))
            overlayCloseButton?.visibility = View.VISIBLE
        }
    }
}
