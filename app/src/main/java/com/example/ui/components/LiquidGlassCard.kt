package com.example.ui.components

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.GlassConfig
import com.example.shader.PrismalShader

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig(),
    cornerRadius: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit
) {
    var size by remember { mutableStateOf(Offset.Zero) }
    var touchPos by remember { mutableStateOf(Offset.Zero) }
    var touchRadius by remember { mutableFloatStateOf(0f) }

    val timeAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        timeAnim.animateTo(
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(100000, easing = LinearEasing)
            )
        )
    }

    // Touch ripple animation when tapped
    LaunchedEffect(touchPos) {
        if (touchPos != Offset.Zero && config.touchRippleEnabled) {
            val rippleAnim = Animatable(10f)
            touchRadius = 10f
            rippleAnim.animateTo(
                targetValue = 500f,
                animationSpec = tween(700, easing = LinearEasing)
            ) {
                touchRadius = value
            }
            touchRadius = 0f
        }
    }

    val shape = RoundedCornerShape(cornerRadius)

    val tintColor = remember(config.tintColorHex) {
        val hex = config.tintColorHex
        val a = ((hex shr 24) and 0xFF).toInt()
        val r = ((hex shr 16) and 0xFF).toInt()
        val g = ((hex shr 8) and 0xFF).toInt()
        val b = (hex and 0xFF).toInt()
        Color(r, g, b, a)
    }

    Box(
        modifier = modifier
            .onSizeChanged { size = Offset(it.width.toFloat(), it.height.toFloat()) }
            .clip(shape)
            .pointerInput(config.touchRippleEnabled) {
                detectTapGestures { tapOffset ->
                    touchPos = tapOffset
                }
            }
            .graphicsLayer {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && config.isEnabled && size.x > 0 && size.y > 0) {
                    try {
                        val effect = PrismalShader.createRenderEffect(
                            config = config,
                            width = size.x,
                            height = size.y,
                            timeSeconds = timeAnim.value,
                            touchX = touchPos.x,
                            touchY = touchPos.y,
                            touchRadius = touchRadius
                        )
                        if (effect != null) {
                            renderEffect = effect.asComposeRenderEffect()
                        }
                    } catch (_: Throwable) {
                        // Fallback gracefully on virtual emulator GPUs
                    }
                }
            }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        tintColor.copy(alpha = (tintColor.alpha + 0.15f).coerceAtMost(0.9f)),
                        tintColor.copy(alpha = tintColor.alpha.coerceAtLeast(0.1f))
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.5f),
                        Color.White.copy(alpha = 0.1f),
                        tintColor.copy(alpha = 0.4f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.x, size.y)
                ),
                shape = shape
            ),
        content = content
    )
}
