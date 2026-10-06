package com.mobilenative.agent.nlu.clients

import com.mobilenative.agent.nlu.PromptTemplates
import com.mobilenative.agent.nlu.models.TaskPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiClient @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun generatePlan(userPrompt: String, apiKey: String): TaskPlan = withContext(Dispatchers.IO) {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

        val payload = buildJsonObject {
            putJsonObject("generationConfig") {
                put("responseMimeType", "application/json")
                put("temperature", 0.1)
            }
            putJsonObject("systemInstruction") {
                putJsonArray("parts") {
                    addJsonObject {
                        put("text", PromptTemplates.SYSTEM_PROMPT)
                    }
                }
            }
            putJsonArray("contents") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("parts") {
                        addJsonObject {
                            put("text", userPrompt)
                        }
                    }
                }
            }
        }.toString()

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                throw ApiException(response.code, "Gemini API error HTTP ${response.code}: $errorBody")
            }

            val responseBody = response.body?.string() ?: throw ApiException(500, "Empty Gemini response body")
            val rootObj = json.parseToJsonElement(responseBody).jsonObject
            val contentStr = rootObj["candidates"]?.jsonArray?.get(0)?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("parts")?.jsonArray?.get(0)?.jsonObject
                ?.get("text")?.jsonPrimitive?.content
                ?: throw ApiException(500, "Failed to parse Gemini candidate content")

            Timber.d("Gemini raw JSON content: %s", contentStr)
            return@withContext json.decodeFromString<TaskPlan>(contentStr)
        }
    }
}
