package com.iattend.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val CELL_SPACING = 4.dp
private val CELL_SHAPE = RoundedCornerShape(4.dp)
private val MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM")

/**
 * Cashiro-exact visual style (HeatmapWidget.kt) but counting PRESENT classes per day.
 * Colors: 0 -> surfaceContainerHigh, 1 -> primary 0.25, 2 -> 0.5, 3-4 -> 0.75, 5+ -> primary.
 * Includes intensity legend below grid per spec #4.
 */
@Composable
fun AttendanceHeatmap(
    presentCountByDate: Map<LocalDate, Int>,
    modifier: Modifier = Modifier
) {
    if (presentCountByDate.isEmpty()) return

    val weeksToShow = 26 // Cashiro: last 6 months
    val today = LocalDate.now()
    // Cashiro starts from Monday of N weeks ago
    val endDate = presentCountByDate.keys.maxOrNull()?.let { maxOf(it, today) } ?: today
    val startDate = endDate.minusWeeks((weeksToShow - 1).toLong()).with(DayOfWeek.MONDAY)

    val totalWeeks = weeksToShow

    val monthLabels = remember(startDate, endDate) {
        val allMonthStarts = mutableListOf<Pair<Int, String>>()
        var current = startDate
        var lastMonth = -1
        var weekIndex = 0
        while (current <= endDate) {
            if (current.monthValue != lastMonth) {
                val formatter = DateTimeFormatter.ofPattern("MMM")
                allMonthStarts.add(weekIndex to current.format(formatter))
                lastMonth = current.monthValue
            }
            current = current.plusWeeks(1)
            weekIndex++
        }
        val filtered = mutableListOf<Pair<Int, String>>()
        for (i in allMonthStarts.indices) {
            val (week, label) = allMonthStarts[i]
            if (i == 0 && allMonthStarts.size > 1) {
                val nextWeek = allMonthStarts[1].first
                if (nextWeek - week < 4) continue
            }
            if (filtered.isEmpty()) filtered.add(week to label)
            else {
                val lastAddedWeek = filtered.last().first
                if (week - lastAddedWeek >= 4) filtered.add(week to label)
            }
        }
        filtered
    }

    val scrollState = rememberScrollState()
    LaunchedEffect(totalWeeks) { scrollState.scrollTo(scrollState.maxValue) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp)
            .clip(RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .horizontalScroll(scrollState)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(CELL_SPACING), modifier = Modifier.padding(bottom = 8.dp)) {
                for (w in 0 until weeksToShow) {
                    Column(verticalArrangement = Arrangement.spacedBy(CELL_SPACING)) {
                        for (d in 0 until 7) {
                            val date = startDate.plusWeeks(w.toLong()).plusDays(d.toLong())
                            val count = presentCountByDate[date] ?: 0
                            val primary = MaterialTheme.colorScheme.primary
                            val color = when {
                                date.isAfter(today) -> MaterialTheme.colorScheme.surfaceContainerHigh
                                count == 0 -> MaterialTheme.colorScheme.surfaceContainerHigh
                                count == 1 -> primary.copy(alpha = 0.25f)
                                count < 3 -> primary.copy(alpha = 0.5f)
                                count < 5 -> primary.copy(alpha = 0.75f)
                                else -> primary
                            }
                            Box(modifier = Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(color))
                        }
                    }
                }
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                monthLabels.forEach { (weekIndex, label) ->
                    val xOffset = (weekIndex * 18).dp
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        modifier = Modifier.offset(x = xOffset)
                    )
                }
            }
            // Intensity legend per spec
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Less", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LegendCell(MaterialTheme.colorScheme.surfaceContainerHigh)
                LegendCell(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                LegendCell(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                LegendCell(MaterialTheme.colorScheme.primary.copy(alpha = 0.75f))
                LegendCell(MaterialTheme.colorScheme.primary)
                Text("More", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                Text("present / day", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun LegendCell(color: androidx.compose.ui.graphics.Color) {
    Box(modifier = Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(color))
}
