package com.example.ui.screens

import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GlassConfig
import com.example.ui.MainUiState
import com.example.ui.components.AppleGlassPillButton
import com.example.ui.components.LiquidGlassCard

@Composable
fun GalleryGlassScreen(
    uiState: MainUiState,
    onUpdateConfig: ((GlassConfig) -> GlassConfig) -> Unit,
    onApplyPreset: (GlassConfig) -> Unit,
    onToggleCompare: () -> Unit,
    onImagePicked: (android.net.Uri) -> Unit
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let { onImagePicked(it) } }
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("gallery_glass_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Preset selector chips
        Text(
            text = "Liquid Glass Presets",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassConfig.PRESETS.forEach { preset ->
                val isSelected = uiState.config.presetName == preset.presetName
                FilterChip(
                    selected = isSelected,
                    onClick = { onApplyPreset(preset) },
                    label = { Text(preset.presetName) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    },
                    modifier = Modifier.testTag("preset_chip_${preset.presetName.replace(" ", "_").lowercase()}")
                )
            }
        }

        // Preview Canvas Area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
                .clip(RoundedCornerShape(24.dp)),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background Image
                if (uiState.selectedImageBitmap != null) {
                    Image(
                        bitmap = uiState.selectedImageBitmap.asImageBitmap(),
                        contentDescription = "Gallery item preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Modern colorful mesh gradient placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF2E3192),
                                        Color(0xFF1BFFFF),
                                        Color(0xFF0F172A)
                                    )
                                )
                            )
                    )
                }

                // Split comparison or single preview
                if (uiState.isCompareSplitMode) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left: Standard Flat Blur
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.25f))
                                    .then(
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            Modifier.blur(uiState.config.blurRadius.dp)
                                        } else Modifier
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Standard Blur\n(Flat Android)",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Right: Prismal Liquid Glass
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LiquidGlassCard(
                                modifier = Modifier.fillMaxSize(),
                                config = uiState.config,
                                cornerRadius = 16.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Prismal Glass",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Refraction: ${String.format("%.2f", uiState.config.refraction)}\nDispersion: ${String.format("%.2f", uiState.config.chromaticAberration)}",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Full Apple Floating Glass Showcase
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Floating Glass Capsule in Top Corner
                        AppleGlassPillButton(
                            text = "🔮 Apple Glass Pill",
                            onClick = {},
                            modifier = Modifier.align(Alignment.TopEnd)
                        )

                        // Center Floating Glass Action Pills
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Apple Liquid Glass Floating Buttons",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppleGlassPillButton(
                                    text = "✨ iOS Glass",
                                    isActive = uiState.config.presetName == "Apple Liquid Glass",
                                    onClick = { onApplyPreset(GlassConfig.PRESETS[1]) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                AppleGlassPillButton(
                                    text = "VisionOS",
                                    isActive = uiState.config.presetName == "Prismal Quartz",
                                    onClick = { onApplyPreset(GlassConfig.PRESETS[0]) }
                                )
                            }
                            Text(
                                text = "Tap buttons for spring animation & real-time optical refraction",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Overlay control buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .align(Alignment.BottomEnd),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        ),
                        modifier = Modifier.testTag("pick_photo_button")
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Image", fontSize = 12.sp)
                    }

                    AssistChip(
                        onClick = onToggleCompare,
                        label = {
                            Text(if (uiState.isCompareSplitMode) "Full Glass" else "Compare Split")
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Compare, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        ),
                        modifier = Modifier.testTag("toggle_compare_button")
                    )
                }
            }
        }

        // Shader Tuning Sliders Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Shader Optical Parameters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = {
                            onApplyPreset(GlassConfig.PRESETS.first())
                        },
                        modifier = Modifier.testTag("reset_shader_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset shader")
                    }
                }

                // Refraction Index
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Index of Refraction (IOR)", fontSize = 13.sp)
                        Text(String.format("%.2f", uiState.config.refraction), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Slider(
                        value = uiState.config.refraction,
                        onValueChange = { newVal ->
                            onUpdateConfig { it.copy(refraction = newVal) }
                        },
                        valueRange = 1.0f..2.2f,
                        modifier = Modifier.testTag("slider_refraction")
                    )
                }

                // Chromatic Aberration Dispersion
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Chromatic Dispersion (Prism RGB Split)", fontSize = 13.sp)
                        Text(String.format("%.2f", uiState.config.chromaticAberration), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Slider(
                        value = uiState.config.chromaticAberration,
                        onValueChange = { newVal ->
                            onUpdateConfig { it.copy(chromaticAberration = newVal) }
                        },
                        valueRange = 0.0f..1.0f,
                        modifier = Modifier.testTag("slider_chromatic_aberration")
                    )
                }

                // Fluid Viscosity
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Fluid Viscosity & Wave Ripples", fontSize = 13.sp)
                        Text(String.format("%.2f", uiState.config.viscosity), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Slider(
                        value = uiState.config.viscosity,
                        onValueChange = { newVal ->
                            onUpdateConfig { it.copy(viscosity = newVal) }
                        },
                        valueRange = 0.0f..1.0f,
                        modifier = Modifier.testTag("slider_viscosity")
                    )
                }

                // Blur Radius
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Surface Blur & Frosting", fontSize = 13.sp)
                        Text("${uiState.config.blurRadius.toInt()} dp", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Slider(
                        value = uiState.config.blurRadius,
                        onValueChange = { newVal ->
                            onUpdateConfig { it.copy(blurRadius = newVal) }
                        },
                        valueRange = 4.0f..50.0f,
                        modifier = Modifier.testTag("slider_blur_radius")
                    )
                }

                // Fresnel Gloss Glow
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Fresnel Specular Rim Gloss", fontSize = 13.sp)
                        Text(String.format("%.1f", uiState.config.fresnelGlow), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Slider(
                        value = uiState.config.fresnelGlow,
                        onValueChange = { newVal ->
                            onUpdateConfig { it.copy(fresnelGlow = newVal) }
                        },
                        valueRange = 1.0f..5.0f,
                        modifier = Modifier.testTag("slider_fresnel_glow")
                    )
                }

                // Glass Tint Swatches
                Column {
                    Text("Glass Tint & Crystal Color", fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
                    val tints = listOf(
                        0x22A0D8EFL to "Cyan Ice",
                        0x18FFFFFFL to "Clear Glass",
                        0x28FF007FL to "Neon Rose",
                        0x2800E5FFL to "Prism Aqua",
                        0x2876FF03L to "Emerald",
                        0x4010141EL to "Smoked Obsidian"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        tints.forEach { (colorHex, name) ->
                            val isSelected = uiState.config.tintColorHex == colorHex
                            val color = Color(
                                ((colorHex shr 16) and 0xFF).toInt(),
                                ((colorHex shr 8) and 0xFF).toInt(),
                                (colorHex and 0xFF).toInt()
                            )
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        onUpdateConfig { it.copy(tintColorHex = colorHex) }
                                    }
                                    .testTag("tint_${name.replace(" ", "_").lowercase()}")
                            )
                        }
                    }
                }

                // Interactive Touch Ripple Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Touch Dynamic Ripple Reaction", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Tapping ripples the liquid glass optics", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = uiState.config.touchRippleEnabled,
                        onCheckedChange = { checked ->
                            onUpdateConfig { it.copy(touchRippleEnabled = checked) }
                        },
                        modifier = Modifier.testTag("switch_touch_ripple")
                    )
                }
            }
        }
    }
}
