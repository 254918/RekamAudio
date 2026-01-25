package com.example.rekamaudio.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.rekamaudio.ui.theme.RekamAudioTheme
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.CompositionContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class OverlayManager(private val context: Context) {

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var isOverlayShown = false
    
    // State to drive the Compose UI
    private var isRecordingState by mutableStateOf(false)
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    fun showOverlay(onRecordClick: () -> Unit, onStopClick: () -> Unit, onCloseClick: () -> Unit, isRecording: Boolean) {
        if (isOverlayShown) {
            updateState(isRecording)
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

        // Initialize Lifecycle Owner
        lifecycleOwner = OverlayLifecycleOwner()
        lifecycleOwner?.performRestore(null)
        lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        overlayView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            
            setContent {
                RekamAudioTheme(
                    dynamicColor = true // Ensure dynamic colors matching the app
                ) {
                    OverlayContent(
                        isRecording = isRecordingState,
                        onRecordClick = onRecordClick,
                        onStopClick = onStopClick,
                        onCloseClick = onCloseClick
                    )
                }
            }
        }

        // Start Lifecycle
        lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        
        updateState(isRecording)

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
                // End Lifecycle
                lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
                lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                
                windowManager?.removeView(overlayView)
                overlayView = null
                lifecycleOwner = null
                isOverlayShown = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateState(isRecording: Boolean) {
        isRecordingState = isRecording
    }
}

@Composable
fun OverlayContent(
    isRecording: Boolean,
    onRecordClick: () -> Unit,
    onStopClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50), // Pill shape
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f), // Premium glass-like feel
        shadowElevation = 8.dp,
        modifier = Modifier.padding(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Record / Stop Button
            if (isRecording) {
                IconButton(
                    onClick = onStopClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop"
                    )
                }
            } else {
                IconButton(
                    onClick = onRecordClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Record"
                    )
                }
            }

            // Close Button (Only show when not recording to prevent accidents)
            if (!isRecording) {
                IconButton(
                    onClick = onCloseClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }
        }
    }
}

// Custom Lifecycle Owner for WindowManager-attached ComposeViews
private class OverlayLifecycleOwner : LifecycleRegistryOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    
    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    fun performRestore(savedState: Bundle?) {
        savedStateRegistryController.performRestore(savedState)
    }
}

// Simple Interface helper if LifecycleOwner clashes
private interface LifecycleRegistryOwner : androidx.lifecycle.LifecycleOwner {
    override val lifecycle: Lifecycle
}
