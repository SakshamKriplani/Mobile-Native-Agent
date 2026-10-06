package com.mobilenative.agent.drivers

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.mobilenative.agent.accessibility.GestureDispatcher
import com.mobilenative.agent.accessibility.ViewTreeExtractor
import com.mobilenative.agent.nlu.models.ScrapedMessage
import kotlinx.coroutines.delay
import timber.log.Timber
import java.util.ArrayDeque
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhatsAppDriver @Inject constructor(
    private val viewTreeExtractor: ViewTreeExtractor,
    private val gestureDispatcher: GestureDispatcher
) {

    companion object {
        const val WHATSAPP_PKG = "com.whatsapp"
        const val WHATSAPP_BIZ_PKG = "com.whatsapp.w4b"
        private val TIME_PATTERN = Pattern.compile("(\\b\\d{1,2}:\\d{2}\\s*(?:[apAP][mM])?\\b)")
    }

    fun isWhatsAppForeground(packageName: String?): Boolean {
        return packageName == WHATSAPP_PKG || packageName == WHATSAPP_BIZ_PKG
    }

    /**
     * Extracts the active chat/contact title from the WhatsApp top toolbar.
     */
    fun extractChatHeaderTitle(root: AccessibilityNodeInfo?): String? {
        if (root == null) return null
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val viewId = node.viewIdResourceName
            if (viewId?.contains("conversation_contact_name", ignoreCase = true) == true) {
                return node.text?.toString()?.trim()
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    /**
     * Extracts recent conversation bubbles with sender classification and timestamps.
     */
    suspend fun scrapeChatHistory(service: AccessibilityService?): Pair<String?, List<ScrapedMessage>> {
        if (service == null) {
            Timber.w("Cannot scrape WhatsApp: AccessibilityService is null")
            return Pair(null, emptyList())
        }

        val messages = mutableListOf<ScrapedMessage>()
        val seenTexts = mutableSetOf<String>()
        val headerTitle = extractChatHeaderTitle(service.rootInActiveWindow)

        // 1. Initial snapshot of visible messages
        val initialNodes = extractChatBubbles(service.rootInActiveWindow, headerTitle)
        for (msg in initialNodes) {
            if (seenTexts.add("${msg.sender}:${msg.text}:${msg.timestamp}")) {
                messages.add(msg)
            }
        }

        // 2. Scroll up to capture multi-message context
        val screenBounds = Rect()
        service.rootInActiveWindow?.getBoundsInScreen(screenBounds)
        val centerX = if (screenBounds.width() > 0) screenBounds.exactCenterX() else 540f
        val centerY = if (screenBounds.height() > 0) screenBounds.exactCenterY() else 1100f

        gestureDispatcher.scrollUp(service, centerX, centerY, distanceY = 600f, durationMs = 300L)
        delay(400) // Wait for UI to settle

        // 3. Second snapshot after scroll
        val scrolledNodes = extractChatBubbles(service.rootInActiveWindow, headerTitle)
        for (msg in scrolledNodes) {
            if (seenTexts.add("${msg.sender}:${msg.text}:${msg.timestamp}")) {
                messages.add(0, msg)
            }
        }

        Timber.i("Scraped %d unique WhatsApp messages from chat '%s'", messages.size, headerTitle)
        return Pair(headerTitle, messages.takeLast(10))
    }

    private fun extractChatBubbles(root: AccessibilityNodeInfo?, fallbackContactName: String?): List<ScrapedMessage> {
        if (root == null) return emptyList()

        val results = mutableListOf<ScrapedMessage>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        val screenBounds = Rect()
        root.getBoundsInScreen(screenBounds)
        val screenWidth = if (screenBounds.width() > 0) screenBounds.width() else 1080

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = node.text?.toString()?.trim()

            if (!text.isNullOrBlank() && text.length > 1) {
                if (!isExcludedSystemText(text)) {
                    val bounds = Rect()
                    node.getBoundsInScreen(bounds)

                    // Direction determination: Right-aligned (> 30% from left) = Outgoing ('Me')
                    val isOutgoing = bounds.left > (screenWidth * 0.30f)
                    val sender = if (isOutgoing) "Me" else (fallbackContactName ?: "Sender")

                    // Extract embedded or neighboring timestamp
                    val timestamp = extractTimestamp(text, node)

                    // Clean text if timestamp was appended
                    val cleanedText = cleanMessageText(text, timestamp)

                    results.add(
                        ScrapedMessage(
                            sender = sender,
                            text = cleanedText,
                            timestamp = timestamp,
                            isOutgoing = isOutgoing
                        )
                    )
                }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return results
    }

    private fun extractTimestamp(text: String, node: AccessibilityNodeInfo): String? {
        val matcher = TIME_PATTERN.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)
        }
        // Check parent container for timestamp node
        val parent = node.parent
        if (parent != null) {
            for (i in 0 until parent.childCount) {
                val sibling = parent.getChild(i)
                val siblingText = sibling?.text?.toString()?.trim() ?: ""
                val m = TIME_PATTERN.matcher(siblingText)
                if (m.find()) {
                    return m.group(1)
                }
            }
        }
        return null
    }

    private fun cleanMessageText(text: String, timestamp: String?): String {
        if (timestamp != null && text.endsWith(timestamp)) {
            return text.removeSuffix(timestamp).trim()
        }
        return text
    }

    private fun isExcludedSystemText(text: String): Boolean {
        val excluded = listOf(
            "type a message", "message", "online", "typing...", "search",
            "calls", "chats", "updates", "status", "camera", "whatsapp"
        )
        return excluded.any { text.equals(it, ignoreCase = true) }
    }
}
