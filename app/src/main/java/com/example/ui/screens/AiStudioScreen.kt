package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.ChatMessage
import com.example.ai.ImageResolution
import com.example.ai.MessageSender
import com.example.ai.ModelRole
import com.example.ui.ImageGenUiState
import com.example.ui.MainUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiStudioScreen(
    uiState: MainUiState,
    onGenerateImage: (String) -> Unit,
    onSelectResolution: (ImageResolution) -> Unit,
    onSetPreviewImage: (Bitmap) -> Unit,
    onSendMessage: (String) -> Unit,
    onSelectChatRole: (ModelRole) -> Unit,
    onClearChat: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ai_studio_screen")
    ) {
        SecondaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("AI Image Studio", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_ai_image")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Gemini AI Chat", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_ai_chat")
            )
        }

        if (selectedTab == 0) {
            AiImageGeneratorTab(
                uiState = uiState,
                onGenerateImage = onGenerateImage,
                onSelectResolution = onSelectResolution,
                onSetPreviewImage = onSetPreviewImage
            )
        } else {
            AiChatTab(
                uiState = uiState,
                onSendMessage = onSendMessage,
                onSelectChatRole = onSelectChatRole,
                onClearChat = onClearChat
            )
        }
    }
}

@Composable
private fun AiImageGeneratorTab(
    uiState: MainUiState,
    onGenerateImage: (String) -> Unit,
    onSelectResolution: (ImageResolution) -> Unit,
    onSetPreviewImage: (Bitmap) -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "High-Quality Image Generator",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Powered by gemini-3-pro-image-preview. Generate custom crystal & liquid glass wallpapers for your gallery.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        // Resolution Affordance (1K, 2K, 4K)
        Column {
            Text(
                text = "Output Image Resolution Affordance:",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ImageResolution.entries.forEach { res ->
                    val isSelected = uiState.selectedResolution == res
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectResolution(res) },
                        label = { Text(res.label, fontSize = 12.sp) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("res_chip_${res.name.lowercase()}")
                    )
                }
            }
        }

        // Prompt input
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Prompt for Image Generation") },
            placeholder = { Text("e.g. Glowing liquid glass prism refraction, fluid caustics...") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_image_prompt"),
            shape = RoundedCornerShape(16.dp),
            maxLines = 4
        )

        // Suggestion Chips
        Text("Quick inspiration prompts:", fontSize = 12.sp, color = Color.Gray)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val suggestions = listOf(
                "Liquid glass crystal prism reflecting neon sunset caustics, cinematic 8k",
                "Abstract fluid glass refractive sphere floating in water, rainbow dispersion",
                "Translucent frosted glass typography with droplets and studio lighting"
            )
            suggestions.forEach { suggestion ->
                AssistChip(
                    onClick = { prompt = suggestion },
                    label = { Text(suggestion.take(34) + "...", fontSize = 11.sp) }
                )
            }
        }

        Button(
            onClick = { onGenerateImage(prompt) },
            enabled = prompt.isNotBlank() && uiState.imageGenState !is ImageGenUiState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("button_generate_image"),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (uiState.imageGenState is ImageGenUiState.Loading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Synthesizing Image (30-60s)...")
            } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Image (${uiState.selectedResolution.label})")
            }
        }

        // Output Preview Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when (val state = uiState.imageGenState) {
                    is ImageGenUiState.Idle -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Your generated high-res image will appear here", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                    is ImageGenUiState.Loading -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Generating ${uiState.selectedResolution.label} image with Gemini...",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Rendering realistic optical refraction & dispersion. Please wait 30-60 seconds.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    is ImageGenUiState.Success -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Image(
                                bitmap = state.bitmap.asImageBitmap(),
                                contentDescription = "Generated image output",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentScale = ContentScale.Crop
                            )
                            FilledTonalButton(
                                onClick = { onSetPreviewImage(state.bitmap) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .testTag("button_use_generated_in_gallery")
                            ) {
                                Text("Apply as Active Gallery Background")
                            }
                        }
                    }
                    is ImageGenUiState.Error -> {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Generation Error", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(state.message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiChatTab(
    uiState: MainUiState,
    onSendMessage: (String) -> Unit,
    onSelectChatRole: (ModelRole) -> Unit,
    onClearChat: () -> Unit
) {
    var messageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.chatMessages.size, uiState.currentStreamingResponse) {
        if (uiState.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.chatMessages.size)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Model Switcher Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Gemini Model Engine:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    IconButton(onClick = onClearChat, modifier = Modifier.size(28.dp).testTag("button_clear_chat")) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear chat", modifier = Modifier.size(16.dp))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModelRole.entries.forEach { role ->
                        val isSelected = uiState.selectedChatModel == role
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectChatRole(role) },
                            label = { Text(role.displayName, fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (role) {
                                        ModelRole.FAST_LITE -> Icons.Default.Bolt
                                        ModelRole.BALANCED_FLASH -> Icons.Default.Speed
                                        ModelRole.REASONING_ARGON -> Icons.Default.Psychology
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            modifier = Modifier.testTag("chip_model_${role.name.lowercase()}")
                        )
                    }
                }

                Text(
                    text = uiState.selectedChatModel.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(uiState.chatMessages) { message ->
                ChatBubble(message = message)
            }

            if (uiState.isChatLoading && uiState.currentStreamingResponse.isNotBlank()) {
                item {
                    ChatBubble(
                        message = ChatMessage(
                            sender = MessageSender.AI,
                            text = uiState.currentStreamingResponse
                        )
                    )
                }
            } else if (uiState.isChatLoading) {
                item {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Gemini is thinking...",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            if (uiState.chatError != null) {
                item {
                    Text(
                        text = "Error: ${uiState.chatError}",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        // Chat Input Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = messageInput,
                onValueChange = { messageInput = it },
                placeholder = { Text("Ask about AGSL shaders, Aves blur...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_chat_message"),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )

            IconButton(
                onClick = {
                    val msg = messageInput
                    messageInput = ""
                    onSendMessage(msg)
                },
                enabled = messageInput.isNotBlank() && !uiState.isChatLoading,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (messageInput.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f))
                    .testTag("button_send_chat")
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send message",
                    tint = if (messageInput.isNotBlank()) MaterialTheme.colorScheme.onPrimary else Color.Gray
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.sender == MessageSender.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text,
                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}
