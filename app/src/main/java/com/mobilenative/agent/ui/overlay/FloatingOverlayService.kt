package com.mobilenative.agent.ui.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.mobilenative.agent.R
import com.mobilenative.agent.accessibility.AccessibilityBridge
import com.mobilenative.agent.drivers.WhatsAppDriver
import com.mobilenative.agent.drivers.WhatsAppLocationAutomation
import com.mobilenative.agent.logging.SupabaseLogger
import com.mobilenative.agent.nlu.LLMGateway
import com.mobilenative.agent.nlu.models.ScrapedMessage
import com.mobilenative.agent.nlu.models.TaskPlan
import com.mobilenative.agent.ui.theme.MobileNativeAgentTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class FloatingOverlayService : LifecycleService() {

    @Inject
    lateinit var accessibilityBridge: AccessibilityBridge

    @Inject
    lateinit var whatsAppDriver: WhatsAppDriver

    @Inject
    lateinit var whatsAppLocationAutomation: WhatsAppLocationAutomation

    @Inject
    lateinit var llmGateway: LLMGateway

    @Inject
    lateinit var supabaseLogger: SupabaseLogger

    private lateinit var windowManager: WindowManager
    private lateinit var overlayParams: WindowManager.LayoutParams
    private var composeView: ComposeView? = null

    // UI States
    private var isProcessingState by mutableStateOf(false)
    private var activeTaskPlan by mutableStateOf<TaskPlan?>(null)
    private var lastScrapedMessages: List<ScrapedMessage> = emptyList()

    // Execution HUD States
    private var isExecutingAutomation by mutableStateOf(false)
    private var currentExecutionStep by mutableIntStateOf(1)
    private var totalExecutionSteps by mutableIntStateOf(3)
    private var executionStatusText by mutableStateOf("Starting...")
    private var isExecutionComplete by mutableStateOf(false)
    private var executionJob: Job? = null

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

                    Box(contentAlignment = Alignment.Center) {
                        // 1. Live Execution HUD (Active during automated screen control)
                        if (isExecutingAutomation) {
                            ExecutionHud(
                                stepStatusText = executionStatusText,
                                currentStep = currentExecutionStep,
                                totalSteps = totalExecutionSteps,
                                isComplete = isExecutionComplete,
                                onCancel = {
                                    cancelActiveExecution()
                                }
                            )
                        }
                        // 2. Confirmation Modal Overlay
                        else if (activeTaskPlan != null) {
                            activeTaskPlan?.let { plan ->
                                ConfirmationModal(
                                    plan = plan,
                                    onApprove = {
                                        Timber.i("User approved task plan: %s", plan.intentType)
                                        val approvedPlan = plan
                                        activeTaskPlan = null
                                        launchWorkflowExecution(approvedPlan)
                                    },
                                    onReject = {
                                        Timber.i("User rejected task plan")
                                        lifecycleScope.launch {
                                            supabaseLogger.logExtractedTask(plan, lastScrapedMessages, status = "REJECTED")
                                        }
                                        activeTaskPlan = null
                                    }
                                )
                            }
                        }
                        // 3. Floating Pill View (Idle State)
                        else {
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
            }
        }

        windowManager.addView(composeView, overlayParams)
    }

    private fun handlePillTrigger() {
        Timber.d("Floating Pill Clicked by User")
        val service = accessibilityBridge.activeService
        if (service == null) {
            Timber.w("Accessibility Service is not connected.")
            return
        }

        val activePkg = accessibilityBridge.activePackageName.value
        isProcessingState = true

        lifecycleScope.launch {
            try {
                if (whatsAppDriver.isWhatsAppForeground(activePkg)) {
                    val (chatHeader, messages) = whatsAppDriver.scrapeChatHistory(service)
                    lastScrapedMessages = messages
                    Timber.i("Captured %d WhatsApp messages from chat '%s' for NLU", messages.size, chatHeader)

                    if (messages.isNotEmpty()) {
                        val taskPlan = llmGateway.parseConversation(messages, chatHeader)
                        Timber.i("Generated Task Plan from %s: %s", taskPlan.taskMetadata?.taskAssigner, taskPlan.humanReadableSummary)

                        if (taskPlan.hasActionableTask) {
                            activeTaskPlan = taskPlan
                            supabaseLogger.logExtractedTask(taskPlan, messages, status = "EXTRACTED")
                        } else {
                            // Casual chat with no task -> Log to Supabase with action_steps=null
                            Timber.i("No actionable task detected. Logging to Supabase with action_steps=null")
                            supabaseLogger.logExtractedTask(taskPlan, messages, status = "NO_TASK_DETECTED")
                        }
                    }
                } else {
                    Timber.i("Pill tapped outside WhatsApp (%s)", activePkg)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing screen trigger")
            } finally {
                isProcessingState = false
            }
        }
    }

    private fun launchWorkflowExecution(plan: TaskPlan) {
        val service = accessibilityBridge.activeService ?: return
        isExecutingAutomation = true
        isExecutionComplete = false
        currentExecutionStep = 1
        totalExecutionSteps = 3
        executionStatusText = "Initializing automation..."

        executionJob = lifecycleScope.launch {
            try {
                // Log approval to Supabase
                supabaseLogger.logExtractedTask(plan, lastScrapedMessages, status = "APPROVED")

                when (plan.intentType) {
                    "SHARE_LOCATION" -> {
                        val success = whatsAppLocationAutomation.executeLocationShare(
                            service = service,
                            onStepProgress = { step, total, status ->
                                currentExecutionStep = step
                                totalExecutionSteps = total
                                executionStatusText = status
                            }
                        )

                        if (success) {
                            isExecutionComplete = true
                            executionStatusText = "Location sent successfully!"
                            supabaseLogger.logExtractedTask(plan, lastScrapedMessages, status = "COMPLETED")
                            delay(1600)
                        } else {
                            executionStatusText = "Action could not be completed"
                            delay(1600)
                        }
                    }
                    else -> {
                        // Level 4 (Shopping) & Level 5 (AI Doc) placeholders
                        executionStatusText = "Workflow ready for Level 4/5 integration"
                        delay(1500)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Workflow execution interrupted")
            } finally {
                isExecutingAutomation = false
                isExecutionComplete = false
            }
        }
    }

    private fun cancelActiveExecution() {
        Timber.w("Emergency cancel triggered by user")
        executionJob?.cancel()
        isExecutingAutomation = false
        isExecutionComplete = false
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
