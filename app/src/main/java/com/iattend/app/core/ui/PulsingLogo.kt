package com.iattend.app.core.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * Breathing logo + expanding ripple ring, matching Cashiro's onboarding WelcomeStep treatment:
 * a 130dp circle scaling 0.95<->1.05 over 2s (reverse), with an independent 120dp ripple ring
 * scaling 0.8->1.6 and fading 0.5->0 over 3s (restart), both via rememberInfiniteTransition.
 */
@Composable
fun PulsingLogo(logoRes: Int, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "logoAnimation")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 2000), RepeatMode.Reverse),
        label = "breathingScale"
    )
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 3000), RepeatMode.Restart),
        label = "rippleScale"
    )
    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 3000), RepeatMode.Restart),
        label = "rippleAlpha"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(rippleScale)
                .graphicsLayer { alpha = rippleAlpha }
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(130.dp)
                .scale(scale)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(logoRes),
                contentDescription = null,
                modifier = Modifier.size(90.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}
