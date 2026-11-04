package com.iattend.app.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Plots weekly attendance % (0-100) as an interactive smoothed line chart with tap-able tooltip info.
 */
@Composable
fun AttendanceBalanceChart(
    weeklyTrend: List<WeeklyAttendancePoint>,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    height: Int = 200
) {
    if (weeklyTrend.isEmpty()) {
        Surface(modifier = modifier.fillMaxWidth().height(height.dp), color = backgroundColor, shape = RoundedCornerShape(12.dp)) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Mark attendance to see weekly trend", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    val sorted = remember(weeklyTrend) { weeklyTrend.sortedBy { it.weekStart } }
    val smoothed = remember(sorted) { smoothWeeklyData(sorted) }

    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(weeklyTrend) {
        animationProgress.animateTo(1f, animationSpec = tween(1500, easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1f)))
    }

    val maxP = smoothed.maxOf { it.percent }
    val minP = smoothed.minOf { it.percent }
    val range = (maxP - minP).let { if (it < 5f) 10f else it } // avoid flat line collapse
    val paddedMin = (minP - range * 0.1f).coerceIn(0f, 100f)
    val paddedMax = (maxP + range * 0.1f).coerceIn(0f, 100f)
    val paddedRange = (paddedMax - paddedMin).let { if (it == 0f) 1f else it }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var lastSelectedPoint by remember { mutableStateOf<WeeklyAttendancePoint?>(null) }
    var chartWidth by remember { mutableFloatStateOf(0f) }
    var chartHeight by remember { mutableFloatStateOf(0f) }

    // Instant dismissal counter: bumps on scroll or screen departure to reset popup without lag
    var instantDismissals by remember { mutableIntStateOf(0) }
    val dismissNow = {
        if (selectedIndex != null) {
            instantDismissals++
            selectedIndex = null
        }
    }

    // Dismiss when screen moves (scroll)
    val lastCardY = remember { floatArrayOf(Float.NaN) }

    // Dismiss when screen leaves foreground
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                dismissNow()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Auto-dismiss tooltip after 3 seconds of inactivity
    LaunchedEffect(selectedIndex) {
        if (selectedIndex != null) {
            delay(3_000)
            selectedIndex = null
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .onGloballyPositioned { coordinates ->
                val y = coordinates.positionInWindow().y
                if (!lastCardY[0].isNaN() && y != lastCardY[0]) dismissNow()
                lastCardY[0] = y
            },
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "${paddedMax.toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "Weekly Attendance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
            }
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).weight(1f)) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(smoothed) {
                            detectTapGestures { tapOffset ->
                                if (smoothed.isEmpty() || size.width == 0) return@detectTapGestures
                                val w = size.width.toFloat()
                                val closestIdx = if (smoothed.size < 2) {
                                    0
                                } else {
                                    ((tapOffset.x / w) * (smoothed.size - 1)).roundToInt().coerceIn(0, smoothed.size - 1)
                                }
                                if (selectedIndex == closestIdx) {
                                    selectedIndex = null
                                } else {
                                    selectedIndex = closestIdx
                                    lastSelectedPoint = smoothed[closestIdx]
                                }
                            }
                        }
                ) {
                    chartWidth = size.width
                    chartHeight = size.height
                    val w = size.width
                    val h = size.height
                    // grid
                    for (i in 0..4) {
                        val y = h * (i / 4f)
                        drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                    }
                    for (i in 0..5) {
                        val x = w * (i / 5f)
                        drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                    }
                    if (smoothed.size < 2) {
                        val y = h * (1f - ((smoothed.first().percent - paddedMin) / paddedRange))
                        drawCircle(lineColor, radius = 4.dp.toPx(), center = Offset(w / 2, y))
                    } else {
                        val path = Path()
                        val points = mutableListOf<Offset>()
                        smoothed.forEachIndexed { idx, pt ->
                            val x = w * (idx.toFloat() / (smoothed.size - 1).coerceAtLeast(1))
                            val y = h * (1f - ((pt.percent - paddedMin) / paddedRange))
                            val off = Offset(x, y)
                            points.add(off)
                            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        clipRect(right = w * animationProgress.value) {
                            val fillPath = Path().apply { addPath(path); lineTo(w, h); lineTo(0f, h); close() }
                            drawPath(fillPath, brush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.3f), lineColor.copy(alpha = 0.05f)), startY = 0f, endY = h))
                            drawPath(path, color = lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                        points.forEachIndexed { idx, pt ->
                            val prog = (animationProgress.value * points.size - idx).coerceIn(0f, 1f)
                            if (prog > 0f) {
                                drawCircle(lineColor, radius = 4.dp.toPx() * prog, center = pt)
                                drawCircle(backgroundColor, radius = 2.dp.toPx() * prog, center = pt, style = Stroke(width = 2.dp.toPx()))
                            }
                        }
                    }

                    // Draw vertical guide line and indicator for selected point
                    if (selectedIndex != null && selectedIndex in smoothed.indices) {
                        val selIdx = selectedIndex!!
                        val selX = if (smoothed.size < 2) w / 2f else w * (selIdx.toFloat() / (smoothed.size - 1))
                        val selY = h * (1f - ((smoothed[selIdx].percent - paddedMin) / paddedRange))

                        drawLine(
                            color = lineColor.copy(alpha = 0.35f),
                            start = Offset(selX, 0f),
                            end = Offset(selX, h),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )

                        // Highlighted halo and circle
                        drawCircle(lineColor.copy(alpha = 0.25f), radius = 10.dp.toPx(), center = Offset(selX, selY))
                        drawCircle(lineColor, radius = 5.dp.toPx(), center = Offset(selX, selY))
                        drawCircle(backgroundColor, radius = 2.5.dp.toPx(), center = Offset(selX, selY))
                    }
                }

                // Tooltip popup anchored above selected point
                if (chartWidth > 0f) {
                    val currentPoint = if (selectedIndex != null && selectedIndex in smoothed.indices) {
                        smoothed[selectedIndex!!]
                    } else {
                        lastSelectedPoint
                    }

                    val selIdx = selectedIndex ?: smoothed.indexOf(currentPoint).takeIf { it != -1 } ?: 0
                    val selX = if (smoothed.size < 2) chartWidth / 2f else chartWidth * (selIdx.toFloat() / (smoothed.size - 1).coerceAtLeast(1))
                    val selY = if (currentPoint != null) {
                        chartHeight * (1f - ((currentPoint.percent - paddedMin) / paddedRange))
                    } else 0f

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(selX.roundToInt(), selY.roundToInt()) }
                            .size(0.dp)
                    ) {
                        WeeklyTrendTooltip(
                            point = currentPoint,
                            visible = selectedIndex != null,
                            instantDismissals = instantDismissals
                        )
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                val fmt = DateTimeFormatter.ofPattern("MMM d")
                Text(text = smoothed.first().weekStart.format(fmt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                if (smoothed.size > 1) Text(text = smoothed.last().weekStart.format(fmt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(text = "${paddedMin.toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp, bottom = 6.dp))
        }
    }
}

@Composable
private fun WeeklyTrendTooltip(
    point: WeeklyAttendancePoint?,
    visible: Boolean,
    instantDismissals: Int = 0
) {
    if (point == null) return
    val gap = with(LocalDensity.current) { 8.dp.roundToPx() }
    val positionProvider = remember(gap) { ChartAboveAnchorPositionProvider(gap) }
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
                        text = formatWeekRange(point.weekStart),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.8f)
                    )
                    Text(
                        text = formatPercent(point.percent),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatWeekRange(weekStart: LocalDate): String {
    val weekEnd = weekStart.plusDays(6)
    val startFmt = DateTimeFormatter.ofPattern("MMM d")
    val endFmt = if (weekStart.monthValue == weekEnd.monthValue) {
        DateTimeFormatter.ofPattern("d")
    } else {
        DateTimeFormatter.ofPattern("MMM d")
    }
    val yearStr = if (weekStart.year != LocalDate.now().year) ", ${weekStart.year}" else ""
    return "${weekStart.format(startFmt)} – ${weekEnd.format(endFmt)}$yearStr"
}

private fun formatPercent(percent: Float): String {
    val formatted = if (percent % 1f == 0f) {
        "${percent.toInt()}%"
    } else {
        String.format(Locale.getDefault(), "%.1f%%", percent)
    }
    return "$formatted Attendance"
}

private class ChartAboveAnchorPositionProvider(
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

private fun smoothWeeklyData(data: List<WeeklyAttendancePoint>, maxPoints: Int = 50): List<WeeklyAttendancePoint> {
    if (data.size <= maxPoints) return data
    val interval = maxOf(1, data.size / maxPoints)
    return data.chunked(interval).map { chunk ->
        val avg = chunk.map { it.percent }.average().toFloat()
        WeeklyAttendancePoint(chunk[chunk.size / 2].weekStart, avg)
    }
}
