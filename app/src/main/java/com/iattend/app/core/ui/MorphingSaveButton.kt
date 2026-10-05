package com.iattend.app.core.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

enum class MorphingButtonState {
    IDLE,
    PENDING,
    SUCCESS
}

/**
 * Animated hand-drawn checkmark that writes itself from the short stroke to the long stroke.
 */
@Composable
fun DrawnCheck(
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.5.dp
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    Canvas(modifier = modifier.size(18.dp)) {
        val w = size.width
        val h = size.height

        val p1 = Offset(w * 0.18f, h * 0.52f)
        val p2 = Offset(w * 0.40f, h * 0.74f)
        val p3 = Offset(w * 0.84f, h * 0.26f)

        val stroke = Stroke(
            width = strokeWidth.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        val t = progress.value
        val seg1Fraction = 0.32f

        if (t <= seg1Fraction) {
            val localT = t / seg1Fraction
            val currentEnd = Offset(
                p1.x + (p2.x - p1.x) * localT,
                p1.y + (p2.y - p1.y) * localT
            )
            drawLine(color, p1, currentEnd, strokeWidth = stroke.width, cap = StrokeCap.Round)
        } else {
            drawLine(color, p1, p2, strokeWidth = stroke.width, cap = StrokeCap.Round)
            val localT = (t - seg1Fraction) / (1f - seg1Fraction)
            val currentEnd = Offset(
                p2.x + (p3.x - p2.x) * localT,
                p2.y + (p3.y - p2.y) * localT
            )
            drawLine(color, p2, currentEnd, strokeWidth = stroke.width, cap = StrokeCap.Round)
        }
    }
}

/**
 * Self-managing Morphing Save Button with suspend [onAction].
 * Transitions IDLE -> PENDING -> SUCCESS with spring physics, drawn checkmark, and haptic feedback.
 */
@Composable
fun MorphingSaveButton(
    onAction: suspend () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Save",
    pendingLabel: String = "Saving...",
    successLabel: String = "Saved",
    enabled: Boolean = true,
    showArrow: Boolean = true,
    onSuccess: (() -> Unit)? = null,
    resetAfterMs: Long = 0L,
    shape: Shape = MaterialTheme.shapes.medium
) {
    var state by remember { mutableStateOf(MorphingButtonState.IDLE) }
    var saveFailed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    MorphingSaveButton(
        onClick = {
            if (state == MorphingButtonState.PENDING) return@MorphingSaveButton
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            state = MorphingButtonState.PENDING
            saveFailed = false
            scope.launch {
                try {
                    onAction()
                    state = MorphingButtonState.SUCCESS
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (onSuccess != null) {
                        delay(600)
                        onSuccess()
                    } else if (resetAfterMs > 0) {
                        delay(resetAfterMs)
                        state = MorphingButtonState.IDLE
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    state = MorphingButtonState.IDLE
                    saveFailed = true
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        label = label,
        pendingLabel = pendingLabel,
        successLabel = successLabel,
        enabled = enabled && state != MorphingButtonState.PENDING,
        state = state,
        showArrow = showArrow,
        shape = shape
    )
    if (saveFailed) Text("Couldn't save. Please try again.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * State-driven Morphing Save Button.
 */
@Composable
fun MorphingSaveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Save",
    pendingLabel: String = "Saving...",
    successLabel: String = "Saved",
    enabled: Boolean = true,
    state: MorphingButtonState = MorphingButtonState.IDLE,
    showArrow: Boolean = true,
    shape: Shape = MaterialTheme.shapes.medium
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && state == MorphingButtonState.IDLE) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "saveBtnScale"
    )

    val containerColor by animateColorAsState(
        targetValue = when (state) {
            MorphingButtonState.SUCCESS -> Color(0xFF2E7D32) // Emerald green
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(220),
        label = "saveBtnColor"
    )

    val contentColor by animateColorAsState(
        targetValue = when (state) {
            MorphingButtonState.SUCCESS -> Color.White
            else -> MaterialTheme.colorScheme.onPrimary
        },
        animationSpec = tween(220),
        label = "saveBtnContentColor"
    )

    Button(
        onClick = {
            if (state == MorphingButtonState.PENDING) return@Button
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        enabled = enabled && state != MorphingButtonState.PENDING,
        shape = shape,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = 380f
                )
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.height(24.dp)
        ) {
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    (slideInVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                        initialOffsetY = { it / 2 }
                    ) + fadeIn(tween(180))).togetherWith(
                        slideOutVertically(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                            targetOffsetY = { -it / 2 }
                        ) + fadeOut(tween(140))
                    )
                },
                label = "saveButtonText"
            ) { currentPhase ->
                Text(
                    text = when (currentPhase) {
                        MorphingButtonState.IDLE -> label
                        MorphingButtonState.PENDING -> pendingLabel
                        MorphingButtonState.SUCCESS -> successLabel
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.width(8.dp))

            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    fadeIn(tween(180)).togetherWith(fadeOut(tween(120)))
                },
                label = "saveButtonIcon"
            ) { currentPhase ->
                when (currentPhase) {
                    MorphingButtonState.IDLE -> {
                        if (showArrow) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                        } else {
                            Spacer(Modifier.size(0.dp))
                        }
                    }
                    MorphingButtonState.PENDING -> {
                        CircularProgressIndicator(
                            color = contentColor,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    MorphingButtonState.SUCCESS -> {
                        DrawnCheck(color = contentColor, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
