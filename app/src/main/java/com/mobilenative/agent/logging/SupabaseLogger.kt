package com.mobilenative.agent.logging

import com.mobilenative.agent.nlu.models.ScrapedMessage
import com.mobilenative.agent.nlu.models.TaskPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseLogger @Inject constructor() {

    var supabaseUrl: String = "https://offrflwttiagsbftpuzs.supabase.co"
    var supabaseAnonKey: String = ""

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun logExtractedTask(
        plan: TaskPlan,
        rawMessages: List<ScrapedMessage>,
        status: String = "EXTRACTED"
    ) = withContext(Dispatchers.IO) {
        if (supabaseUrl.isBlank() || supabaseAnonKey.isBlank()) {
            Timber.d("Supabase logging skipped: URL or Anon Key not configured")
            return@withContext
        }

        try {
            val normalizedUrl = supabaseUrl.trimEnd('/') + "/rest/v1/extracted_tasks"

            val payload = buildJsonObject {
                put("chat_name", plan.taskMetadata?.chatName ?: "WhatsApp Chat")
                put("task_assigner", plan.taskMetadata?.taskAssigner ?: "Unknown")
                put("task_receiver", plan.taskMetadata?.taskReceiver ?: "Me")
                put("assigned_timestamp", plan.taskMetadata?.assignedTimestamp ?: "")
                put("intent_type", plan.intentType)
                put("summary", plan.humanReadableSummary)
                put("status", status)

                // If task exists, log action steps; otherwise keep it null
                if (plan.hasActionableTask && plan.executionSteps.isNotEmpty()) {
                    put("action_steps", json.encodeToJsonElement(plan.executionSteps))
                    put("entities", json.encodeToJsonElement(plan.entities))
                } else {
                    put("action_steps", JsonNull)
                    put("entities", JsonNull)
                }

                put("raw_messages", buildJsonArray {
                    rawMessages.forEach { msg ->
                        addJsonObject {
                            put("sender", msg.sender)
                            put("text", msg.text)
                            put("timestamp", msg.timestamp ?: "")
                            put("isOutgoing", msg.isOutgoing)
                        }
                    }
                })
            }.toString()

            val request = Request.Builder()
                .url(normalizedUrl)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Timber.i("Successfully logged task to Supabase: %s (action_steps=%s)", plan.intentType, plan.hasActionableTask)
                } else {
                    val err = response.body?.string() ?: ""
                    Timber.w("Failed to log task to Supabase (HTTP %d): %s", response.code, err)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while logging task to Supabase")
        }
    }
}
