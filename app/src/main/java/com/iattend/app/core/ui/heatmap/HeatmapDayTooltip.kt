package com.iattend.app.core.ui.heatmap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Floating card shown above a tapped heatmap cell.
 * Animated in and out with smooth scale + fade and automatically positioned above/below the cell.
 */
@Composable
fun HeatmapDayTooltip(day: HeatmapDay, visible: Boolean, instantDismissals: Int = 0) {
    val gap = with(LocalDensity.current) { 6.dp.roundToPx() }
    val positionProvider = remember(gap) { AboveAnchorPositionProvider(gap) }
    val visibility = remember(instantDismissals) { MutableTransitionState(false) }
    visibility.targetState = visible

    if (!visibility.currentState && !visibility.targetState) return

    Popup(popupPositionProvider = positionProvider) {
        AnimatedVisibility(
            visibleState = visibility,
            enter = fadeIn(tween(160)) +
                scaleIn(tween(200, easing = FastOutSlowInEasing), initialScale = 0.85f, transformOrigin = TransformOrigin(0.5f, 1f)) +
                slideInVertically(tween(200, easing = FastOutSlowInEasing)) { it / 6 },
            exit = fadeOut(tween(120)) +
                scaleOut(tween(150), targetScale = 0.9f, transformOrigin = TransformOrigin(0.5f, 1f)) +
                slideOutVertically(tween(150)) { it / 8 }
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = formatTooltipDate(day.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.8f)
                    )
                    Text(
                        text = formatAttendanceCount(day.intensity),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatTooltipDate(date: LocalDate): String {
    val pattern = if (date.year == LocalDate.now().year) "EEE, MMM d" else "EEE, MMM d, yyyy"
    return date.format(DateTimeFormatter.ofPattern(pattern))
}

private fun formatAttendanceCount(count: Int): String = when {
    count <= 0 -> "No classes attended"
    count == 1 -> "1 class attended"
    else -> "$count classes attended"
}

/**
 * Centres the popup horizontally over the anchor, keeping it within the window.
 * Flips below the anchor if there isn't room above.
 */
private class AboveAnchorPositionProvider(
    private val gap: Int
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val x = (anchorBounds.center.x - popupContentSize.width / 2).coerceIn(0, maxX)

        val above = anchorBounds.top - popupContentSize.height - gap
        val y = if (above >= 0) above else anchorBounds.bottom + gap

        return IntOffset(x, y)
    }
}
