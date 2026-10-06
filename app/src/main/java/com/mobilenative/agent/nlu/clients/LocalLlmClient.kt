package com.mobilenative.agent.nlu.clients

import com.mobilenative.agent.nlu.models.ExtractedEntities
import com.mobilenative.agent.nlu.models.ShoppingItem
import com.mobilenative.agent.nlu.models.TaskPlan
import com.mobilenative.agent.nlu.models.TaskStep
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalLlmClient @Inject constructor() {

    /**
     * Local deterministic fallback rule-based / on-device SLM parser.
     * Guarantees zero-failure operation even when offline or cloud rate-limits hit.
     */
    fun generateOfflineFallbackPlan(rawText: String): TaskPlan {
        Timber.i("Running Local Offline Fallback Parser on: %s", rawText)
        val lower = rawText.lowercase()

        // 1. Check for location request
        if (lower.contains("location") || lower.contains("kaha hai") || lower.contains("kahan ho")) {
            return TaskPlan(
                hasActionableTask = true,
                intentType = "SHARE_LOCATION",
                confidence = 0.9f,
                humanReadableSummary = "Share your current real-time GPS location to this WhatsApp chat.",
                entities = ExtractedEntities(locationRequested = true),
                executionSteps = listOf(
                    TaskStep(1, "com.whatsapp", "OPEN_ATTACHMENT", null),
                    TaskStep(2, "com.whatsapp", "SELECT_LOCATION", null),
                    TaskStep(3, "com.whatsapp", "SEND_CURRENT_LOCATION", null)
                )
            )
        }

        // 2. Check for shopping / snack items
        if (lower.contains("lays") || lower.contains("coke") || lower.contains("order") || lower.contains("daal de") || lower.contains("bhejna")) {
            val items = mutableListOf<ShoppingItem>()
            if (lower.contains("lays")) {
                val variant = if (lower.contains("blue")) "Blue (Classic Salted)" else "Default"
                items.add(ShoppingItem("Lays Classic Salted", variant, 2))
            }
            if (lower.contains("coke") || lower.contains("coca")) {
                items.add(ShoppingItem("Coca-Cola", "Can/Bottle", 1))
            }

            return TaskPlan(
                hasActionableTask = true,
                intentType = "SHOPPING_CART_PREPARATION",
                confidence = 0.85f,
                humanReadableSummary = "Add ${items.joinToString { "${it.quantity}x ${it.productName}" }} to cart on Blinkit (stops before checkout).",
                entities = ExtractedEntities(
                    shoppingItems = items,
                    targetStore = "blinkit"
                ),
                executionSteps = listOf(
                    TaskStep(1, "com.grofers.customerapp", "SEARCH_AND_ADD", "Lays Classic Salted"),
                    TaskStep(2, "com.grofers.customerapp", "SEARCH_AND_ADD", "Coca-Cola"),
                    TaskStep(3, "com.grofers.customerapp", "OPEN_CART", null)
                )
            )
        }

        return TaskPlan(
            hasActionableTask = false,
            intentType = "UNKNOWN",
            confidence = 0.5f,
            humanReadableSummary = "No actionable cross-app task detected in recent messages."
        )
    }
}
