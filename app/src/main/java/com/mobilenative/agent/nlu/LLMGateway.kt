package com.mobilenative.agent.nlu

import com.mobilenative.agent.nlu.clients.GeminiClient
import com.mobilenative.agent.nlu.clients.GroqClient
import com.mobilenative.agent.nlu.clients.LocalLlmClient
import com.mobilenative.agent.nlu.models.ScrapedMessage
import com.mobilenative.agent.nlu.models.TaskPlan
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LLMGateway @Inject constructor(
    private val groqClient: GroqClient,
    private val geminiClient: GeminiClient,
    private val localLlmClient: LocalLlmClient
) {
    var groqApiKey: String = ""
    var geminiApiKey: String = ""

    suspend fun parseConversation(messages: List<ScrapedMessage>, chatHeaderTitle: String? = null): TaskPlan {
        val userPrompt = PromptTemplates.buildUserPrompt(messages, chatHeaderTitle)
        val rawSummaryText = messages.joinToString(" ") { it.text }

        // 1. Primary Attempt: Groq (Llama-3.3-70b - Ultra Fast LPU)
        if (groqApiKey.isNotBlank()) {
            try {
                Timber.i("Attempting Primary NLU via Groq with participant recognition...")
                return groqClient.generatePlan(userPrompt, groqApiKey)
            } catch (e: Exception) {
                Timber.w(e, "Groq failed. Falling back to Secondary (Gemini)...")
            }
        }

        // 2. Secondary Attempt: Google Gemini Flash
        if (geminiApiKey.isNotBlank()) {
            try {
                Timber.i("Attempting Secondary NLU via Google Gemini...")
                return geminiClient.generatePlan(userPrompt, geminiApiKey)
            } catch (e: Exception) {
                Timber.w(e, "Gemini failed. Falling back to Tertiary Local Engine...")
            }
        }

        // 3. Tertiary Offline Fallback (Local Engine)
        Timber.i("Running Tertiary Offline Local NLU Engine...")
        return localLlmClient.generateOfflineFallbackPlan(rawSummaryText)
    }
}
