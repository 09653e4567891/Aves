package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "GeminiAiManager"

enum class ModelRole(val modelId: String, val displayName: String, val description: String) {
    FAST_LITE("gemini-3.1-flash-lite", "Gemini 3.1 Flash Lite", "Ultra-fast, lowest latency responses"),
    BALANCED_FLASH("gemini-3.8-flash", "Gemini 3.8 Flash", "Balanced, general-purpose vision & optics assistant"),
    REASONING_ARGON("gemini-4-argon", "Gemini 4 Argon", "Deep reasoning for complex shaders & math")
}

enum class ImageResolution(val size: ImageSize, val label: String) {
    SIZE_1K(ImageSize.SIZE_1K, "1K (1024x1024)"),
    SIZE_2K(ImageSize.SIZE_2K, "2K (2048x2048)"),
    SIZE_4K(ImageSize.SIZE_4K, "4K (4096x4096)")
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER, AI, SYSTEM
}

class GeminiAiManager {

    private val conversationHistory = mutableListOf<Content>()

    /**
     * Generate high-quality images using gemini-3-pro-image-preview (with fallback to gemini-nano-banana-2.1)
     * Supports user affordance for 1K, 2K, and 4K resolutions.
     */
    suspend fun generateImage(
        prompt: String,
        resolution: ImageResolution = ImageResolution.SIZE_1K
    ): Bitmap {
        return withTimeout(60_000L) {
            val config = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
                imageConfig = ImageConfig.builder()
                    .setImageSize(resolution.size)
                    .build()
            }

            // Primary model: gemini-3-pro-image-preview
            val modelName = "gemini-3-pro-image-preview"
            try {
                val model = Firebase.ai.generativeModel(
                    modelName = modelName,
                    generationConfig = config
                )
                val response = model.generateContent(prompt)
                val imagePart = response.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()

                imagePart?.image ?: throw IllegalStateException("Model returned no image candidate.")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Image generation with $modelName failed, trying gemini-nano-banana-2.1: ${e.message}")
                // Fallback to gemini-nano-banana-2.1
                val fallbackModel = Firebase.ai.generativeModel(
                    modelName = "gemini-nano-banana-2.1",
                    generationConfig = config
                )
                val response = fallbackModel.generateContent(prompt)
                val imagePart = response.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()

                imagePart?.image ?: throw (e)
            }
        }
    }

    /**
     * Multi-turn chat session with role switching (fast lite, flash, argon)
     * and streaming output to minimize perceived latency.
     */
    fun streamChatResponse(
        userMessage: String,
        role: ModelRole = ModelRole.FAST_LITE,
        systemInstructionText: String = "You are an expert in Android AGSL shaders, fluid optics, liquid glass (Prismal), and Aves Gallery customization."
    ): Flow<String> = flow {
        val model = Firebase.ai.generativeModel(
            modelName = role.modelId,
            systemInstruction = content { text(systemInstructionText) }
        )

        // Append user content to history
        val userContent = content("user") { text(userMessage) }
        val currentHistory = conversationHistory.toList()

        val chat = model.startChat(history = currentHistory)
        val accumulatedText = StringBuilder()

        try {
            withTimeout(60_000L) {
                chat.sendMessageStream(userMessage).collect { chunk ->
                    val text = chunk.text.orEmpty()
                    accumulatedText.append(text)
                    emit(accumulatedText.toString())
                }
            }
            // Save to history on success
            conversationHistory.add(userContent)
            conversationHistory.add(content("model") { text(accumulatedText.toString()) })
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Chat response timed out", e)
            throw IllegalStateException("Response timed out after 60 seconds.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Chat failed on ${role.modelId}", e)
            throw e
        }
    }

    fun clearHistory() {
        conversationHistory.clear()
    }
}
