package com.iattend.app.feature.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter

/**
 * Cashiro BalanceChart.kt adapted: plots weekly attendance % (0-100) as a smoothed line.
 * Original used BigDecimal balances; here Float percent. Keeps animation, grid, gradient fill.
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

    Surface(modifier = modifier.fillMaxWidth().height(height.dp), color = backgroundColor, shape = RoundedCornerShape(12.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "${paddedMax.toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "Weekly Attendance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
            }
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).weight(1f)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
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

private fun smoothWeeklyData(data: List<WeeklyAttendancePoint>, maxPoints: Int = 50): List<WeeklyAttendancePoint> {
    if (data.size <= maxPoints) return data
    val interval = maxOf(1, data.size / maxPoints)
    return data.chunked(interval).map { chunk ->
        val avg = chunk.map { it.percent }.average().toFloat()
        WeeklyAttendancePoint(chunk[chunk.size / 2].weekStart, avg)
    }
}
