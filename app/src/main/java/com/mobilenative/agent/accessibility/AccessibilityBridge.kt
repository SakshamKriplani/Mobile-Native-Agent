package com.mobilenative.agent.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Normalized representation of an on-screen interactive UI node.
 */
data class UiNode(
    val viewId: String?,
    val className: String,
    val text: String?,
    val contentDescription: String?,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val isScrollable: Boolean,
    val bounds: Rect,
    val rawNode: AccessibilityNodeInfo?
)

/**
 * Singleton state bridge between the Android AccessibilityService and UI/Orchestrator layers.
 */
@Singleton
class AccessibilityBridge @Inject constructor() {

    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    private val _activePackageName = MutableStateFlow<String?>(null)
    val activePackageName: StateFlow<String?> = _activePackageName.asStateFlow()

    var activeService: AgentAccessibilityService? = null
        set(value) {
            field = value
            _isServiceConnected.value = (value != null)
        }

    fun updateActivePackage(packageName: String?) {
        _activePackageName.value = packageName
    }
}
