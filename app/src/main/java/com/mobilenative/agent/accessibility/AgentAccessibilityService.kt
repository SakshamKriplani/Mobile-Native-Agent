package com.mobilenative.agent.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AgentAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var bridge: AccessibilityBridge

    @Inject
    lateinit var viewTreeExtractor: ViewTreeExtractor

    @Inject
    lateinit var gestureDispatcher: GestureDispatcher

    override fun onServiceConnected() {
        super.onServiceConnected()
        Timber.i("AgentAccessibilityService Connected")
        bridge.activeService = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString()
                bridge.updateActivePackage(packageName)
                Timber.d("Active Package Changed: %s", packageName)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Content updated (e.g. streaming LLM text or WhatsApp chat loaded)
            }
        }
    }

    override fun onInterrupt() {
        Timber.w("AgentAccessibilityService Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("AgentAccessibilityService Destroyed")
        bridge.activeService = null
    }
}
