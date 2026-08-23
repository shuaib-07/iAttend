package com.iattend.app.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iattend.app.ui.theme.Motion

/**
 * Two-layer animated wavy ring (Implementation Plan §7's "Material3 Expressive progress-ring"):
 * the arc eases toward [percentage] and the color eases between the on/below-threshold tones,
 * both using the shared [Motion] spring vocabulary instead of an instant snap. Replays its fill
 * from 0 every time it enters composition (e.g. navigating to a fresh screen instance), then
 * tracks live value changes normally while mounted.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProgressRing(
    percentage: Float,
    aboveThreshold: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    strokeWidth: Dp = 8.dp,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium
) {
    var targetFraction by remember { mutableFloatStateOf(0f) }
    var targetPercentage by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(percentage) {
        targetFraction = (percentage / 100f).coerceIn(0f, 1f)
        targetPercentage = percentage
    }

    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = Motion.gentle(),
        label = "progressRingFraction"
    )
    val animatedPercentage by animateFloatAsState(
        targetValue = targetPercentage,
        animationSpec = Motion.gentle(),
        label = "progressRingCountUp"
    )
    val color by animateColorAsState(
        targetValue = if (aboveThreshold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        animationSpec = Motion.snappy(),
        label = "progressRingColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "progressRingBreathe")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "progressRingBreatheScale"
    )

    val strokePx = with(LocalDensity.current) { strokeWidth.toPx() }
    val stroke = remember(strokePx) { Stroke(width = strokePx, cap = StrokeCap.Round) }

    Box(
        modifier = modifier.size(size).graphicsLayer { scaleX = breatheScale; scaleY = breatheScale },
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = strokeWidth,
            strokeCap = StrokeCap.Round
        )
        CircularWavyProgressIndicator(
            progress = { animatedFraction },
            modifier = Modifier.fillMaxSize(),
            color = color,
            trackColor = Color.Transparent,
            stroke = stroke,
            trackStroke = stroke
        )
        Text("${animatedPercentage.toInt()}%", style = textStyle, color = color)
    }
}
