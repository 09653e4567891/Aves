package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.screens.AiStudioScreen
import com.example.ui.screens.GalleryGlassScreen
import com.example.ui.screens.LSPosedStatusScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.configureAppCheck

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: App Check configuration wrapped safely
        try {
            configureAppCheck(this, intent)
        } catch (_: Throwable) {}

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsState()
                var currentTab by remember { mutableIntStateOf(0) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Aves Liquid Glass",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp
                                )
                            },
                            actions = {
                                Box(
                                    modifier = Modifier.padding(end = 16.dp)
                                ) {
                                    Text(
                                        text = "API 102",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.testTag("tag_api_102")
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.testTag("main_navigation_bar")
                        ) {
                            NavigationBarItem(
                                selected = currentTab == 0,
                                onClick = { currentTab = 0 },
                                icon = { Icon(Icons.Default.WaterDrop, contentDescription = null) },
                                label = { Text("Glass Studio") },
                                modifier = Modifier.testTag("nav_tab_glass")
                            )
                            NavigationBarItem(
                                selected = currentTab == 1,
                                onClick = { currentTab = 1 },
                                icon = { Icon(Icons.Default.Extension, contentDescription = null) },
                                label = { Text("LSPosed & Aves") },
                                modifier = Modifier.testTag("nav_tab_lsposed")
                            )
                            NavigationBarItem(
                                selected = currentTab == 2,
                                onClick = { currentTab = 2 },
                                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                                label = { Text("AI Studio") },
                                modifier = Modifier.testTag("nav_tab_ai")
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            0 -> GalleryGlassScreen(
                                uiState = uiState,
                                onUpdateConfig = { viewModel.updateConfig(it) },
                                onApplyPreset = { viewModel.applyPreset(it) },
                                onToggleCompare = { viewModel.toggleCompareMode() },
                                onImagePicked = { viewModel.onImagePicked(it) }
                            )
                            1 -> LSPosedStatusScreen(
                                uiState = uiState,
                                onUpdateConfig = { viewModel.updateConfig(it) },
                                onRefreshStatus = { viewModel.loadInitialState() },
                                onLaunchAves = { viewModel.launchAves(this@MainActivity) }
                            )
                            2 -> AiStudioScreen(
                                uiState = uiState,
                                onGenerateImage = { viewModel.generateImage(it) },
                                onSelectResolution = { viewModel.setResolution(it) },
                                onSetPreviewImage = {
                                    viewModel.setGeneratedImageAsPreview(it)
                                    currentTab = 0
                                },
                                onSendMessage = { viewModel.sendChatMessage(it) },
                                onSelectChatRole = { viewModel.setChatModelRole(it) },
                                onClearChat = { viewModel.clearChat() }
                            )
                        }
                    }
                }
            }
        }
    }
}
