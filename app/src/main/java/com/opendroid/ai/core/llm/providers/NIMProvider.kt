package com.opendroid.ai.core.llm.providers

import android.util.Log
import com.aetheria.jarvis.llm.NimTripleClient
import com.opendroid.ai.core.llm.*
import com.opendroid.ai.data.models.ChatMessage
import com.opendroid.ai.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NIM Provider - wraps NimTripleClient to implement LLMProvider interface.
 * Routes requests to appropriate NIM model based on request type:
 * - Requests with images -> VisionDescriber (LLaVA)
 * - Planning requests (JSON format, system prompt contains planning) -> MainReasoner (DeepSeek)
 * - Other requests -> FastRouter (Qwen)
 */
@Singleton
class NIMProvider @Inject constructor(
    private val nimClient: NimTripleClient,
    private val settingsRepository: SettingsRepository
) : LLMProvider {

    override val name: String = "NIM Triple"
    override val availableModels: List<String> = listOf("nim-triple")

    private val TAG = "NIMProvider"

    override suspend fun complete(request: LLMRequest): LLMResponse {
        // Determine which NIM model to use based on request characteristics
        val role = determineRole(request)
        Log.d(TAG, "Routing request to NIM ${role.name}")

        return try {
            nimClient.complete(role, request)
        } catch (e: IOException) {
            Log.e(TAG, "NIM request failed: ${e.message}")
            // Graceful degradation: return error response that caller can handle
            throw com.opendroid.ai.core.llm.error.LLMErrorMapper.fromThrowable(name, request.model.orEmpty(), e)
        }
    }

    override fun streamComplete(request: LLMRequest): Flow<String> = flow {
        val role = determineRole(request)
        val response = nimClient.complete(role, request)
        val words = response.content.split(" ")
        for (word in words) {
            emit("$word ")
        }
    }

    override suspend fun isAvailable(): Boolean {
        return nimClient.isAvailable()
    }

    /**
     * Determine which NIM model role to use based on the request.
     */
    private fun determineRole(request: LLMRequest): NimTripleClient.ModelRole {
        // If request has image, use VisionDescriber
        val hasImage = request.messages.any { it.imageBase64 != null && it.imageBase64.isNotBlank() }
        if (hasImage) {
            return NimTripleClient.ModelRole.VISION_DESCRIBER
        }

        // If system prompt contains planning keywords, use MainReasoner
        val systemPrompt = request.systemPrompt.lowercase()
        val isPlanning = systemPrompt.contains("planning") ||
                         systemPrompt.contains("plan") ||
                         request.responseFormat == ResponseFormat.JSON && request.maxTokens > 500

        if (isPlanning) {
            return NimTripleClient.ModelRole.MAIN_REASONER
        }

        // Default to FastRouter for quick responses
        return NimTripleClient.ModelRole.FAST_ROUTER
    }
}