package com.mobilenative.agent.nlu.models

import kotlinx.serialization.Serializable

@Serializable
data class ScrapedMessage(
    val sender: String,
    val text: String,
    val timestamp: String? = null,
    val isOutgoing: Boolean = false
)

@Serializable
data class TaskMetadata(
    val taskAssigner: String, // The friend/contact who requested the task (e.g. "Rahul" or "Sender")
    val taskReceiver: String = "Me", // The user who received the request
    val assignedTimestamp: String? = null, // e.g. "14:04" or "10:30 PM"
    val chatName: String? = null // Contact name or group title
)

@Serializable
data class ShoppingItem(
    val productName: String,
    val variant: String? = null,
    val quantity: Int = 1
)

@Serializable
data class DocumentDetails(
    val docType: String,
    val clientName: String? = null,
    val lineItems: String? = null,
    val totalAmount: String? = null,
    val format: String = "PDF"
)

@Serializable
data class ExtractedEntities(
    val shoppingItems: List<ShoppingItem> = emptyList(),
    val targetStore: String = "blinkit", // blinkit, zepto, instamart
    val documentRequest: DocumentDetails? = null,
    val locationRequested: Boolean = false,
    val contactRequested: Boolean = false,
    val recipient: String? = null
)

@Serializable
data class TaskStep(
    val stepId: Int,
    val targetApp: String,
    val action: String,
    val params: String? = null
)

@Serializable
data class TaskPlan(
    val hasActionableTask: Boolean,
    val intentType: String, // SHOPPING_CART_PREPARATION, AI_DOC_CREATION_SHARE, SHARE_LOCATION, UNKNOWN
    val confidence: Float = 1.0f,
    val humanReadableSummary: String,
    val taskMetadata: TaskMetadata? = null,
    val entities: ExtractedEntities = ExtractedEntities(),
    val requiresClarification: Boolean = false,
    val clarificationQuestion: String? = null,
    val clarificationOptions: List<String> = emptyList(),
    val executionSteps: List<TaskStep> = emptyList()
)
