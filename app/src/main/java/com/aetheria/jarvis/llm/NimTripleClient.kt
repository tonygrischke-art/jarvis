package com.aetheria.jarvis.llm

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.opendroid.ai.core.llm.LLMProvider
import com.opendroid.ai.core.llm.LLMRequest
import com.opendroid.ai.core.llm.LLMResponse
import com.opendroid.ai.core.llm.ResponseFormat
import com.opendroid.ai.data.models.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NIM Triple Client - routes requests to three specialized NIM endpoints:
 * - FastRouter (port 8080): Qwen2.5-0.5B-Instruct - for intent classification, quick decisions
 * - MainReasoner (port 8081): DeepSeek-Coder-1.3B-Instruct - for task planning, reasoning
 * - VisionDescriber (port 8082): LLaVA-1.6-Mistral-7B-Instruct - for screenshot analysis
 * 
 * All endpoints: https://integrate.api.nvidia.com/v1/chat/completions
 * Auth: Bearer token from NIM_API_KEY (SecurePrefs)
 * Timeout: 60s, graceful degradation on failure
 */
@Singleton
class NimTripleClient @Inject constructor(
    private val client: OkHttpClient,
    private val settingsRepository: com.opendroid.ai.data.repository.SettingsRepository
) {

    private val gson = Gson()
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    private val TAG = "NimTripleClient"

    private val NIM_BASE_URL = "https://integrate.api.nvidia.com/v1/chat/completions"
    private val TIMEOUT_SECONDS = 60L

    enum class ModelRole {
        FAST_ROUTER,    // Qwen2.5-0.5B-Instruct - intent classification, quick decisions
        MAIN_REASONER,  // DeepSeek-Coder-1.3B-Instruct - task planning, reasoning
        VISION_DESCRIBER // LLaVA-1.6-Mistral-7B-Instruct - screenshot analysis
    }

    private data class ModelConfig(
        val role: ModelRole,
        val modelName: String,
        val systemPrompt: String,
        val temperature: Float = 0.3f,
        val maxTokens: Int = 1500
    )

    private val modelConfigs = mapOf(
        ModelRole.FAST_ROUTER to ModelConfig(
            role = ModelRole.FAST_ROUTER,
            modelName = "qwen2.5-0.5b-instruct",
            systemPrompt = """You are a fast intent classifier for Android automation.
Analyze the user's request and classify it into ONE of these categories:
- NAVIGATE: Open app, go to website, navigate UI
- FORM_FILL: Fill out forms, input text, select options
- ACTION: Tap, swipe, click, toggle settings
- QUERY: Ask question, get info, read screen
- SYSTEM: Settings, permissions, device control

Return ONLY the category name.""",
            temperature = 0.1f,
            maxTokens = 50
        ),
        ModelRole.MAIN_REASONER to ModelConfig(
            role = ModelRole.MAIN_REASONER,
            modelName = "deepseek-coder-1.3b-instruct",
            systemPrompt = """You are JARVIS's Main Reasoner - an expert Android automation planner.
Given a user goal, create a step-by-step JSON plan for autonomous execution.

Available actions: OPEN_APP, OPEN_URL, TAP, SWIPE, TYPE_TEXT, SCROLL, SCREENSHOT, GET_SCREEN_TEXT, WAIT, BACK, HOME

Plan format (JSON):
{
  "goal": "user's original request",
  "steps": [
    {"action": "OPEN_APP", "params": {"packageName": "com.chrome.android"}, "description": "Open Chrome"},
    {"action": "OPEN_URL", "params": {"url": "https://indeed.com"}, "description": "Navigate to Indeed"},
    {"action": "TAP", "params": {"text": "Apply Now"}, "description": "Tap Apply button"},
    {"action": "TYPE_TEXT", "params": {"text": "John Doe"}, "description": "Enter name"},
    {"action": "SCREENSHOT", "params": {}, "description": "Capture confirmation"}
  ],
  "requiresVision": true
}

Be concise. Max 10 steps. Only output valid JSON.""",
            temperature = 0.2f,
            maxTokens = 2000
        ),
        ModelRole.VISION_DESCRIBER to ModelConfig(
            role = ModelRole.VISION_DESCRIBER,
            modelName = "llava-1.6-mistral-7b",
            systemPrompt = """You are JARVIS's Vision Describer - analyze Android screenshots for automation.
Describe what you see: app name, UI elements, buttons, forms, text fields, errors.
Focus on actionable information for form filling and navigation.
Be concise and structured.""",
            temperature = 0.3f,
            maxTokens = 800
        )
    )

    /**
     * Get the NIM API key from secure storage
     */
    private suspend fun getApiKey(): String? = withContext(Dispatchers.IO) {
        val config = settingsRepository.llmConfig.first()
        return@withContext config.apiKeys["NIM"]?.takeIf { it.isNotBlank() }
    }

    /**
     * Execute a request against a specific NIM model role
     */
    suspend fun complete(role: ModelRole, request: LLMRequest): LLMResponse {
        val apiKey = getApiKey() ?: throw IllegalStateException("NIM API Key not configured. Set it in Settings.")
        val config = modelConfigs[role]!!
        val startTime = System.currentTimeMillis()

        val messages = request.messages.map { msg ->
            val roleStr = if (msg.sender == ChatMessage.Sender.USER) "user" else "assistant"
            val content: Any = if (msg.imageBase64 != null && roleStr == "user") {
                val parts = mutableListOf<Map<String, Any>>()
                parts.add(mapOf("type" to "text", "text" to msg.text))
                parts.add(mapOf(
                    "type" to "image_url",
                    "image_url" to mapOf("url" to "data:image/jpeg;base64,${msg.imageBase64}")
                ))
                parts
            } else {
                msg.text
            }
            mapOf("role" to roleStr, "content" to content)
        }

        val requestBody = JsonObject().apply {
            addProperty("model", config.modelName)
            add("messages", gson.toJsonTree(messages))
            addProperty("temperature", config.temperature)
            addProperty("max_tokens", config.maxTokens)
            if (request.responseFormat == ResponseFormat.JSON) {
                addProperty("response_format", "json_object")
            }
            addProperty("system_prompt", config.systemPrompt)
        }

        val httpRequest = Request.Builder()
            .url(NIM_BASE_URL)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody(mediaType))
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(httpRequest).execute().use { response ->
                    val responseBody = response.body?.string() ?: throw IOException("Empty response")
                    if (!response.isSuccessful) {
                        throw IOException("NIM API error ${response.code}: $responseBody")
                    }
                    val json = gson.fromJson(responseBody, JsonObject::class.java)
                    val choices = json.getAsJsonArray("choices")
                    val firstChoice = choices[0].asJsonObject
                    val message = firstChoice.getAsJsonObject("message")
                    val content = message.get("content").asString

                    val usage = json.getAsJsonObject("usage")
                    val tokensUsed = usage?.get("total_tokens")?.asInt ?: 0

                    LLMResponse(
                        content = content,
                        tokensUsed = tokensUsed,
                        model = config.modelName,
                        provider = "NIM-${role.name}",
                        latencyMs = System.currentTimeMillis() - startTime
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "${role.name} request failed: ${e.message}")
                throw IOException("${role.name} failed: ${e.message}", e)
            }
        }
    }

    /**
     * Streaming version for real-time responses
     */
    fun streamComplete(role: ModelRole, request: LLMRequest): Flow<String> = flow {
        val response = complete(role, request)
        val words = response.content.split(" ")
        for (word in words) {
            emit("$word ")
        }
    }

    /**
     * Quick intent classification using FastRouter
     */
    suspend fun classifyIntent(userQuery: String): String {
        val request = LLMRequest(
            systemPrompt = modelConfigs[ModelRole.FAST_ROUTER]!!.systemPrompt,
            messages = listOf(ChatMessage(
                id = java.util.UUID.randomUUID().toString(),
                text = userQuery,
                sender = ChatMessage.Sender.USER
            )),
            temperature = 0.1f,
            maxTokens = 50,
            responseFormat = ResponseFormat.TEXT
        )
        return complete(ModelRole.FAST_ROUTER, request).content.trim().uppercase()
    }

    /**
     * Generate a task plan using MainReasoner
     */
    suspend fun generatePlan(userGoal: String, screenContext: String? = null): String {
        var prompt = "User goal: $userGoal"
        if (screenContext != null) {
            prompt += "\n\nCurrent screen context: $screenContext"
        }
        val request = LLMRequest(
            systemPrompt = modelConfigs[ModelRole.MAIN_REASONER]!!.systemPrompt,
            messages = listOf(ChatMessage(
                id = java.util.UUID.randomUUID().toString(),
                text = prompt,
                sender = ChatMessage.Sender.USER
            )),
            temperature = 0.2f,
            maxTokens = 2000,
            responseFormat = ResponseFormat.JSON
        )
        return complete(ModelRole.MAIN_REASONER, request).content.trim()
    }

    /**
     * Analyze screenshot using VisionDescriber
     */
    suspend fun analyzeScreenshot(base64Image: String, question: String = "What do you see on this screen?"): String {
        val prompt = "Analyze this Android screenshot. User question: $question\n\nDescribe: 1) What app is open 2) What content is visible 3) Answer the user's question 4) Any important UI elements for automation"
        val request = LLMRequest(
            systemPrompt = modelConfigs[ModelRole.VISION_DESCRIBER]!!.systemPrompt,
            messages = listOf(ChatMessage(
                id = java.util.UUID.randomUUID().toString(),
                text = prompt,
                sender = ChatMessage.Sender.USER,
                imageBase64 = base64Image
            )),
            temperature = 0.3f,
            maxTokens = 800,
            responseFormat = ResponseFormat.TEXT
        )
        return complete(ModelRole.VISION_DESCRIBER, request).content.trim()
    }

    /**
     * Check if NIM API is available
     */
    suspend fun isAvailable(): Boolean {
        return try {
            getApiKey() != null
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        const val PROVIDER_NAME = "NIM Triple"
    }
}