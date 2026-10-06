package com.mobilenative.agent.orchestrator

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.mobilenative.agent.accessibility.GestureDispatcher
import com.mobilenative.agent.accessibility.UiNode
import com.mobilenative.agent.accessibility.ViewTreeExtractor
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskStepRunner @Inject constructor(
    private val viewTreeExtractor: ViewTreeExtractor,
    private val gestureDispatcher: GestureDispatcher
) {

    /**
     * Attempts to find and click a node matching either a resource ID, text pattern, or content description.
     */
    suspend fun findAndClick(
        service: AccessibilityService?,
        searchCriteria: (UiNode) -> Boolean,
        maxRetries: Int = 4,
        retryDelayMs: Long = 400L
    ): Boolean {
        if (service == null) return false

        repeat(maxRetries) { attempt ->
            val root = service.rootInActiveWindow
            val nodes = viewTreeExtractor.extractNodes(root)
            val match = nodes.firstOrNull(searchCriteria)

            if (match != null) {
                Timber.d("Found matching node for click: %s (text: '%s')", match.viewId, match.text)
                val clicked = gestureDispatcher.performClick(match.rawNode)
                if (!clicked) {
                    // Fallback to synthetic coordinate tap if accessibility action fails
                    gestureDispatcher.tapCoordinates(
                        service,
                        match.bounds.exactCenterX(),
                        match.bounds.exactCenterY()
                    )
                }
                delay(300) // Settle delay
                return true
            }

            Timber.d("Node not found on attempt %d/%d. Waiting...", attempt + 1, maxRetries)
            delay(retryDelayMs)
        }

        return false
    }

    /**
     * Polls the window until a specific condition appears (e.g. screen transition).
     */
    suspend fun waitForCondition(
        service: AccessibilityService?,
        condition: (List<UiNode>) -> Boolean,
        timeoutMs: Long = 4000L
    ): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val root = service?.rootInActiveWindow
            val nodes = viewTreeExtractor.extractNodes(root)
            if (condition(nodes)) {
                return true
            }
            delay(250)
        }
        return false
    }
}
