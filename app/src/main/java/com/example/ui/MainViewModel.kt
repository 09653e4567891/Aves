package com.example.ui

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ChatMessage
import com.example.ai.GeminiAiManager
import com.example.ai.ImageResolution
import com.example.ai.MessageSender
import com.example.ai.ModelRole
import com.example.model.GlassConfig
import com.example.provider.GlassSettingsProvider
import com.example.util.ModuleStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ImageGenUiState {
    data object Idle : ImageGenUiState
    data object Loading : ImageGenUiState
    data class Success(val bitmap: Bitmap) : ImageGenUiState
    data class Error(val message: String) : ImageGenUiState
}

data class MainUiState(
    val config: GlassConfig = GlassConfig(),
    val isAvesInstalled: Boolean = false,
    val detectedAvesPackage: String? = null,
    val isLsposedModuleActive: Boolean = false,
    val isCompareSplitMode: Boolean = true,
    val selectedImageUri: Uri? = null,
    val selectedImageBitmap: Bitmap? = null,
    val imageGenState: ImageGenUiState = ImageGenUiState.Idle,
    val selectedResolution: ImageResolution = ImageResolution.SIZE_1K,
    val chatMessages: List<ChatMessage> = listOf(
        ChatMessage(
            sender = MessageSender.AI,
            text = "Welcome to Aves Liquid Glass! I can help you customize AGSL refraction shaders, explain chromatic dispersion, or generate high-res wallpapers for your gallery."
        )
    ),
    val isChatLoading: Boolean = false,
    val currentStreamingResponse: String = "",
    val selectedChatModel: ModelRole = ModelRole.FAST_LITE,
    val chatError: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val aiManager = GeminiAiManager()

    init {
        loadInitialState()
    }

    fun loadInitialState() {
        val context = getApplication<Application>()
        val savedConfig = GlassSettingsProvider.getSavedConfig(context)

        val detectedPkg = detectAvesPackage(context)
        val isHookActive = ModuleStatus.isModuleActive()

        _uiState.update {
            it.copy(
                config = savedConfig,
                isAvesInstalled = detectedPkg != null,
                detectedAvesPackage = detectedPkg,
                isLsposedModuleActive = isHookActive
            )
        }
    }

    private fun detectAvesPackage(context: Context): String? {
        val candidates = listOf(
            "deckers.thibault.aves",
            "deckers.thibault.aves.libre",
            "deckers.thibault.aves.debug"
        )
        for (pkg in candidates) {
            try {
                context.packageManager.getPackageInfo(pkg, 0)
                return pkg
            } catch (_: Exception) {}
        }

        // Fallback: search for any installed gallery activity with 'aves' in package name
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                type = "image/*"
            }
            val resolveList = context.packageManager.queryIntentActivities(intent, 0)
            for (res in resolveList) {
                val pkg = res.activityInfo?.packageName.orEmpty()
                if (pkg.contains("aves", ignoreCase = true)) {
                    return pkg
                }
            }
        } catch (_: Exception) {}

        return null
    }

    fun launchAves(context: Context) {
        val pkg = _uiState.value.detectedAvesPackage ?: "deckers.thibault.aves"
        val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
        if (launchIntent != null) {
            context.startActivity(launchIntent)
        }
    }

    fun updateConfig(modifier: (GlassConfig) -> GlassConfig) {
        val newConfig = modifier(_uiState.value.config)
        _uiState.update { it.copy(config = newConfig) }
        GlassSettingsProvider.saveConfig(getApplication(), newConfig)
    }

    fun applyPreset(preset: GlassConfig) {
        val newConfig = preset.copy(
            replaceSystemBlur = _uiState.value.config.replaceSystemBlur,
            injectAvesHeaders = _uiState.value.config.injectAvesHeaders,
            injectAvesNav = _uiState.value.config.injectAvesNav,
            touchRippleEnabled = _uiState.value.config.touchRippleEnabled
        )
        _uiState.update { it.copy(config = newConfig) }
        GlassSettingsProvider.saveConfig(getApplication(), newConfig)
    }

    fun toggleCompareMode() {
        _uiState.update { it.copy(isCompareSplitMode = !it.isCompareSplitMode) }
    }

    fun onImagePicked(uri: Uri) {
        val context = getApplication<Application>()
        try {
            val stream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(stream)
            _uiState.update {
                it.copy(selectedImageUri = uri, selectedImageBitmap = bitmap)
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to load picked image", e)
        }
    }

    fun setGeneratedImageAsPreview(bitmap: Bitmap) {
        _uiState.update { it.copy(selectedImageBitmap = bitmap) }
    }

    fun setResolution(res: ImageResolution) {
        _uiState.update { it.copy(selectedResolution = res) }
    }

    fun setChatModelRole(role: ModelRole) {
        _uiState.update { it.copy(selectedChatModel = role) }
    }

    fun generateImage(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(imageGenState = ImageGenUiState.Loading) }
            try {
                val bitmap = aiManager.generateImage(prompt, _uiState.value.selectedResolution)
                _uiState.update {
                    it.copy(
                        imageGenState = ImageGenUiState.Success(bitmap),
                        selectedImageBitmap = bitmap
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MainViewModel", "Image generation error", e)
                _uiState.update {
                    it.copy(imageGenState = ImageGenUiState.Error(e.localizedMessage ?: "Image generation failed."))
                }
            }
        }
    }

    fun sendChatMessage(message: String) {
        if (message.isBlank() || _uiState.value.isChatLoading) return

        val userMsg = ChatMessage(sender = MessageSender.USER, text = message)
        _uiState.update {
            it.copy(
                chatMessages = it.chatMessages + userMsg,
                isChatLoading = true,
                currentStreamingResponse = "",
                chatError = null
            )
        }

        viewModelScope.launch {
            try {
                aiManager.streamChatResponse(
                    userMessage = message,
                    role = _uiState.value.selectedChatModel
                ).collect { chunk ->
                    _uiState.update { it.copy(currentStreamingResponse = chunk) }
                }

                val fullResponse = _uiState.value.currentStreamingResponse
                if (fullResponse.isNotBlank()) {
                    val aiMsg = ChatMessage(sender = MessageSender.AI, text = fullResponse)
                    _uiState.update {
                        it.copy(
                            chatMessages = it.chatMessages + aiMsg,
                            isChatLoading = false,
                            currentStreamingResponse = ""
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isChatLoading = false,
                            chatError = "Empty response returned from model."
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MainViewModel", "Chat streaming error", e)
                _uiState.update {
                    it.copy(
                        isChatLoading = false,
                        chatError = e.localizedMessage ?: "Failed to generate chat response."
                    )
                }
            }
        }
    }

    fun clearChat() {
        aiManager.clearHistory()
        _uiState.update {
            it.copy(
                chatMessages = listOf(
                    ChatMessage(
                        sender = MessageSender.AI,
                        text = "Chat cleared. Ask me anything about AGSL liquid glass or Aves Gallery!"
                    )
                ),
                chatError = null
            )
        }
    }
}
