package com.iattend.app.core.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeEffect

private const val SUBTITLE_TRANSITION_DURATION = 450

/**
 * RvSystem-style header — clean-room reimplementation of
 * RvSystem-Monitor/ui/components/AppBars.kt:SimpleTopAppBar idea:
 * TopAppBar with title+AnimatedContent subtitle+actions, container Transparent,
 * wrapped in existing Haze blur (10dp + progressive) over a slightly lighter
 * tint (surfaceContainerLow @ 0.80) as requested. No 28dp gradient, no
 * isScrolled alpha — exact Rv visual + haze.
 * Keeps iAttend headlineMedium Bold and notifications bell.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandHeader(
    currentTabLabel: String = "",
    onNotificationClick: (() -> Unit)? = null,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val headerBg = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.80f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .hazeEffect(state = hazeState) {
                style = HazeDefaults.style(
                    backgroundColor = headerBg,
                    tint = HazeDefaults.tint(headerBg),
                    blurRadius = 10.dp,
                    noiseFactor = 0f
                )
                progressive = HazeProgressive.verticalGradient(
                    startIntensity = 1f,
                    endIntensity = 0f
                )
            }
    ) {
        TopAppBar(
            title = {
                androidx.compose.foundation.layout.Column {
                    Text(
                        "iAttend",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    AnimatedContent(
                        targetState = currentTabLabel,
                        transitionSpec = {
                            (
                                slideInHorizontally(animationSpec = tween(SUBTITLE_TRANSITION_DURATION, easing = FastOutSlowInEasing)) +
                                    scaleIn(animationSpec = tween(SUBTITLE_TRANSITION_DURATION, easing = FastOutSlowInEasing))
                                ).togetherWith(
                                slideOutHorizontally(animationSpec = tween(SUBTITLE_TRANSITION_DURATION, easing = FastOutSlowInEasing)) +
                                    scaleOut(animationSpec = tween(SUBTITLE_TRANSITION_DURATION, easing = FastOutSlowInEasing))
                            )
                        },
                        label = "brandHeaderSubtitle"
                    ) { label ->
                        if (label.isNotEmpty()) {
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            actions = {
                if (onNotificationClick != null) {
                    IconButton(onClick = onNotificationClick) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
    }
}

// Back-compat without haze — same TopAppBar but with solid slightly-lighter background
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandHeader(
    currentTabLabel: String = "",
    onNotificationClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val headerBg = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.80f)
    TopAppBar(
        modifier = modifier,
        title = {
            androidx.compose.foundation.layout.Column {
                Text(
                    "iAttend",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (currentTabLabel.isNotEmpty()) {
                    Text(currentTabLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        },
        actions = {
            if (onNotificationClick != null) {
                IconButton(onClick = onNotificationClick) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = headerBg
        )
    )
}