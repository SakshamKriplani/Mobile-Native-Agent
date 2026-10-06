package com.mobilenative.agent.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ViewTreeExtractor @Inject constructor() {

    /**
     * Traverses the active window using Breadth-First Search (BFS) and extracts
     * a sanitized list of actionable or readable UI nodes.
     */
    fun extractNodes(root: AccessibilityNodeInfo?): List<UiNode> {
        if (root == null) return emptyList()

        val results = mutableListOf<UiNode>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()
            val viewId = node.viewIdResourceName
            val isClickable = node.isClickable
            val isEditable = node.isEditable
            val isScrollable = node.isScrollable

            // Filter for meaningful interactive or text-bearing nodes
            if (!text.isNullOrEmpty() || !desc.isNullOrEmpty() || isClickable || isEditable || isScrollable) {
                results.add(
                    UiNode(
                        viewId = viewId,
                        className = node.className?.toString() ?: "",
                        text = text,
                        contentDescription = desc,
                        isClickable = isClickable,
                        isEditable = isEditable,
                        isScrollable = isScrollable,
                        bounds = bounds,
                        rawNode = node
                    )
                )
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { child ->
                    queue.add(child)
                }
            }
        }
        return results
    }
}
