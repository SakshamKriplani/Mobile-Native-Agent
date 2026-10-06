package com.mobilenative.agent.drivers

import android.accessibilityservice.AccessibilityService
import com.mobilenative.agent.orchestrator.TaskStepRunner
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhatsAppLocationAutomation @Inject constructor(
    private val stepRunner: TaskStepRunner
) {

    suspend fun executeLocationShare(
        service: AccessibilityService?,
        onStepProgress: (stepNumber: Int, totalSteps: Int, statusText: String) -> Unit
    ): Boolean {
        if (service == null) {
            Timber.w("Cannot execute location share: AccessibilityService is null")
            return false
        }

        try {
            // STEP 1: Tap the Attachment (+) / Paperclip Button
            onStepProgress(1, 3, "Tapping WhatsApp attachment menu...")
            val attachmentClicked = stepRunner.findAndClick(
                service = service,
                searchCriteria = { node ->
                    node.viewId?.contains("input_attach_button", ignoreCase = true) == true ||
                            node.contentDescription?.contains("Attach", ignoreCase = true) == true ||
                            node.viewId?.contains("attach_button", ignoreCase = true) == true
                },
                maxRetries = 4,
                retryDelayMs = 300L
            )

            if (!attachmentClicked) {
                Timber.w("Failed to click WhatsApp attachment button")
                return false
            }

            delay(400) // Wait for popup menu to render

            // STEP 2: Tap "Location" in the Attachment Grid
            onStepProgress(2, 3, "Selecting Location from menu...")
            val locationItemClicked = stepRunner.findAndClick(
                service = service,
                searchCriteria = { node ->
                    node.text?.equals("Location", ignoreCase = true) == true ||
                            node.contentDescription?.contains("Location", ignoreCase = true) == true ||
                            node.viewId?.contains("pick_location", ignoreCase = true) == true
                },
                maxRetries = 4,
                retryDelayMs = 350L
            )

            if (!locationItemClicked) {
                Timber.w("Failed to select 'Location' from attachment menu")
                return false
            }

            // STEP 3: Wait for Location Picker & Tap "Send your current location"
            onStepProgress(3, 3, "Waiting for GPS settlement & sending location...")
            delay(800) // Wait for WhatsApp map & nearby places to initialize

            val sendLocationClicked = stepRunner.findAndClick(
                service = service,
                searchCriteria = { node ->
                    node.text?.contains("Send your current location", ignoreCase = true) == true ||
                            node.contentDescription?.contains("Send your current location", ignoreCase = true) == true ||
                            node.viewId?.contains("send_my_location", ignoreCase = true) == true
                },
                maxRetries = 6,
                retryDelayMs = 500L
            )

            if (sendLocationClicked) {
                Timber.i("Successfully sent current location via WhatsApp UI automation!")
                delay(500)
                return true
            } else {
                Timber.w("Could not find 'Send your current location' button on location picker")
                return false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error during WhatsApp location automation")
            return false
        }
    }
}
