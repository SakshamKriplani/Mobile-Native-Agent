package com.mobilenative.agent.ui.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.mobilenative.agent.R
import com.mobilenative.agent.accessibility.AccessibilityBridge
import com.mobilenative.agent.accessibility.ViewTreeExtractor
import com.mobilenative.agent.ui.theme.MobileNativeAgentTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class FloatingOverlayService : LifecycleService() {

    @Inject
    lateinit var accessibilityBridge: AccessibilityBridge

    @Inject
    lateinit var viewTreeExtractor: ViewTreeExtractor

    private lateinit var windowManager: WindowManager
    private lateinit var overlayParams: WindowManager.LayoutParams
    private var composeView: ComposeView? = null

    private var isProcessingState by mutableStateOf(false)

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "agent_overlay_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        startAsForegroundService()
        initOverlayView()
        Timber.i("FloatingOverlayService started")
    }

    private fun startAsForegroundService() {
        val channelName = "Mobile Native Agent Overlay"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the agent floating pill active on screen"
            }
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_service_notification_title))
            .setContentText(getString(R.string.overlay_service_notification_desc))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun initOverlayView() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 300
        }

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingOverlayService)
            setViewTreeViewModelStoreOwner(null)
            setViewTreeSavedStateRegistryOwner(null)

            setContent {
                MobileNativeAgentTheme {
                    val activePackage by accessibilityBridge.activePackageName.collectAsState()

                    FloatingPillView(
                        activePackage = activePackage,
                        isProcessing = isProcessingState,
                        onPillClicked = {
                            handlePillTrigger()
                        },
                        onDragDelta = { dx, dy ->
                            overlayParams.x += dx.toInt()
                            overlayParams.y += dy.toInt()
                            windowManager.updateViewLayout(this@apply, overlayParams)
                        }
                    )
                }
            }
        }

        windowManager.addView(composeView, overlayParams)
    }

    private fun handlePillTrigger() {
        Timber.d("Floating Pill Clicked by User")
        val service = accessibilityBridge.activeService
        if (service == null) {
            Timber.w("Accessibility Service is not connected. User must enable it in Settings.")
            return
        }

        val rootNode = service.rootInActiveWindow
        val visibleNodes = viewTreeExtractor.extractNodes(rootNode)
        Timber.i("Scraped %d nodes from active window (%s)", visibleNodes.size, accessibilityBridge.activePackageName.value)

        // Flash processing indicator
        isProcessingState = true
        composeView?.postDelayed({
            isProcessingState = false
        }, 1200)
    }

    override fun onDestroy() {
        super.onDestroy()
        composeView?.let {
            windowManager.removeView(it)
        }
        Timber.i("FloatingOverlayService destroyed")
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}
