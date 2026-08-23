package com.iattend.app.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.core.ui.AttendanceHeatmap
import com.iattend.app.core.ui.DateRangePickerField
import com.iattend.app.ui.theme.Motion
import java.time.LocalDate

@Composable
fun InsightsScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var filterStart by remember { mutableStateOf<LocalDate?>(null) }
    var filterEnd by remember { mutableStateOf<LocalDate?>(null) }

    // Filtered copies for DateRange per spec 2b
    val filteredHeatmap = remember(state.heatmap, filterStart, filterEnd) {
        if (filterStart == null && filterEnd == null) state.heatmap
        else state.heatmap.filterKeys { d ->
            (filterStart == null || !d.isBefore(filterStart)) && (filterEnd == null || !d.isAfter(filterEnd))
        }
    }
    val filteredWeekly = remember(state.weeklyTrend, filterStart, filterEnd) {
        if (filterStart == null && filterEnd == null) state.weeklyTrend
        else state.weeklyTrend.filter { (filterStart == null || !it.weekStart.isBefore(filterStart)) && (filterEnd == null || !it.weekStart.isAfter(filterEnd)) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 110.dp)
    ) {
        // Date range filter for insights
        item {
            DateRangePickerField(
                label = "Filter period",
                startDate = filterStart,
                endDate = filterEnd,
                onRangeChange = { s, e -> filterStart = s; filterEnd = e },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (state.subjects.isNotEmpty()) {
            item {
                Text(
                    "COMPARISON",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                )
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.subjects.forEach { summary -> SubjectBarRow(summary) }
                    }
                }
            }
        }

        // Balance Chart (weekly avg smoothed) above heatmap per spec #5
        item {
            Text(
                "WEEKLY TREND",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
            )
            AttendanceBalanceChart(
                weeklyTrend = filteredWeekly,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (filteredHeatmap.isNotEmpty()) {
            item {
                Text(
                    "ACTIVITY",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                )
                Card(modifier = Modifier.fillMaxWidth()) {
                    AttendanceHeatmap(filteredHeatmap, modifier = Modifier.fillMaxWidth().padding(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SubjectBarRow(summary: SubjectSummary) {
    val belowThreshold = summary.total > 0 && summary.percentage < summary.requiredPercentage
    val barColor = if (belowThreshold) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val fraction by animateFloatAsState(
        targetValue = (summary.percentage / 100f).coerceIn(0f, 1f),
        animationSpec = Motion.gentle(),
        label = "subjectBarFraction"
    )

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            summary.subject.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(88.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.extraSmall)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(barColor, MaterialTheme.shapes.extraSmall)
            )
        }
        Text(
            "${summary.percentage.toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = barColor,
            modifier = Modifier.width(36.dp)
        )
    }
}
