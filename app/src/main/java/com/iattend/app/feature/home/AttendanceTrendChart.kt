package com.iattend.app.feature.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter

private val DATE_LABEL_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
private const val MAX_POINTS_SHOWN = 60

/**
 * Expandable cumulative-attendance-% trend card: a small sparkline collapsed, a full dotted/gridded
 * line chart with a 3-stat row expanded. Tapping the card (or the chevron) toggles state.
 */
@Composable
fun AttendanceTrendChart(
    points: List<AttendancePoint>,
    thisMonthPercent: Float,
    thisSemesterPercent: Float,
    currentPercent: Float,
    currentPercentCaption: String,
    semesterLabel: String,
    semesterFallbackNotice: String?,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "trendChevron")
    val shown = remember(points) {
        if (points.size <= MAX_POINTS_SHOWN) points
        else points.filterIndexed { index, _ -> index % (points.size / MAX_POINTS_SHOWN + 1) == 0 }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ATTENDANCE TREND", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${currentPercent.toInt()}%", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            currentPercentCaption,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                    semesterFallbackNotice?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.graphicsLayer { rotationZ = chevronRotation }
                )
            }

            if (shown.isEmpty()) {
                Text(
                    "Mark some attendance to see your trend",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
                return@Column
            }

            if (!isExpanded) {
                Sparkline(shown, modifier = Modifier.fillMaxWidth().height(40.dp).padding(top = 8.dp))
            } else {
                ExpandedTrendChart(shown, modifier = Modifier.fillMaxWidth().height(160.dp).padding(top = 16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatColumn("This month", thisMonthPercent)
                    StatColumn(semesterLabel, thisSemesterPercent, semesterFallbackNotice)
                    StatColumn("Current", currentPercent)
                }
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, percent: Float, detail: String? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${percent.toInt()}%", style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        detail?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun Sparkline(points: List<AttendancePoint>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val maxPercent = 100f
        val stepX = size.width / (points.size - 1)
        val path = androidx.compose.ui.graphics.Path()
        points.forEachIndexed { index, point ->
            val x = index * stepX
            val y = size.height * (1f - point.cumulativePercent / maxPercent)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = lineColor, style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
private fun ExpandedTrendChart(points: List<AttendancePoint>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    val backgroundColor = MaterialTheme.colorScheme.surface
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val maxPercent = 100f
            val stepX = if (points.size > 1) size.width / (points.size - 1) else 0f

            // Dashed horizontal gridlines at 0/25/50/75/100%
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            for (fraction in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
                val y = size.height * fraction
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx(), pathEffect = dashEffect)
            }

            val path = androidx.compose.ui.graphics.Path()
            val coordinates = points.mapIndexed { index, point ->
                Offset(index * stepX, size.height * (1f - point.cumulativePercent / maxPercent))
            }
            coordinates.forEachIndexed { index, offset ->
                if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
            }
            drawPath(path, color = lineColor, style = Stroke(width = 2.5.dp.toPx()))

            coordinates.forEach { offset ->
                drawCircle(color = lineColor, radius = 3.dp.toPx(), center = offset)
                drawCircle(color = backgroundColor, radius = 3.dp.toPx(), center = offset, style = Stroke(width = 1.5.dp.toPx()))
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(points.first().date.format(DATE_LABEL_FORMAT), style = labelStyle)
            Text(points.last().date.format(DATE_LABEL_FORMAT), style = labelStyle)
        }
    }
}
