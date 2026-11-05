package com.iattend.app.ui.modifiers

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class BlurDirection { TOP, BOTTOM }

/**
 * Gradient scrim overlay for smoothly fading edges under bars or navigation containers.
 * Safe across all GPU architectures (no heavy AGSL runtime convolution loops).
 */
fun Modifier.progressiveBlur(
    blurRadius: Float = 0f,
    height: Float,
    direction: BlurDirection = BlurDirection.TOP,
    showGradientOverlay: Boolean = true
): Modifier = composed {
    val overlayColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.65f)

    if (showGradientOverlay && height > 0f) {
        Modifier.drawWithContent {
            drawContent()
            val brush = when (direction) {
                BlurDirection.TOP -> Brush.verticalGradient(
                    colors = listOf(overlayColor, Color.Transparent),
                    endY = height
                )
                BlurDirection.BOTTOM -> Brush.verticalGradient(
                    colors = listOf(Color.Transparent, overlayColor),
                    startY = (size.height - height).coerceAtLeast(0f)
                )
            }
            drawRect(brush = brush)
        }
    } else Modifier
}
