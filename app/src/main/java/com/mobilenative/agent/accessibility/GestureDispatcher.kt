package com.mobilenative.agent.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GestureDispatcher @Inject constructor() {

    /**
     * Standard Accessibility Node Click
     */
    fun performClick(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }
        return false
    }

    /**
     * Programmatic Direct Text Injection
     */
    fun setText(node: AccessibilityNodeInfo?, text: String): Boolean {
        if (node == null) return false
        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    /**
     * Synthetic Screen Coordinate Tap via Bezier Stroke
     */
    fun tapCoordinates(
        service: AccessibilityService?,
        x: Float,
        y: Float,
        durationMs: Long = 50L,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (service == null) {
            Timber.e("Cannot tap coordinate: AccessibilityService is not connected")
            onComplete(false)
            return
        }

        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Timber.d("Coordinate tap succeeded at ($x, $y)")
                onComplete(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                Timber.w("Coordinate tap cancelled at ($x, $y)")
                onComplete(false)
            }
        }, null)
    }

    /**
     * Synthetic Drag/Scroll Gesture
     */
    fun scrollUp(
        service: AccessibilityService?,
        startX: Float,
        startY: Float,
        distanceY: Float = 500f,
        durationMs: Long = 300L,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (service == null) {
            onComplete(false)
            return
        }

        val endY = startY + distanceY // Downward drag scrolls content upward
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(startX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                onComplete(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                onComplete(false)
            }
        }, null)
    }
}
