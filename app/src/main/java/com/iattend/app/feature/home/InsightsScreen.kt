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
import com.iattend.app.core.navigation.LocalNavigationContentPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.core.domain.stats.StreakCalculator
import com.iattend.app.core.domain.stats.StreakStats
import com.iattend.app.core.ui.AttendanceHeatmap
import com.iattend.app.core.ui.StreakCards
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import com.iattend.app.feature.bunk.BunkSimulatorBottomSheet
import com.iattend.app.core.ui.DateRangePickerField
import com.iattend.app.ui.theme.Motion
import com.iattend.app.core.ui.EmptyListState
import androidx.compose.foundation.layout.widthIn
import java.time.LocalDate

@Composable
fun InsightsScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var filterStart by remember { mutableStateOf<LocalDate?>(null) }
    var filterEnd by remember { mutableStateOf<LocalDate?>(null) }
    var showBunkSimulator by remember { mutableStateOf(false) }

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
    val streakStats = remember(state.markedOccurrences, state.bestStreak, state.currentStreak, filterStart, filterEnd) {
        if (filterStart == null && filterEnd == null) {
            StreakStats(currentStreak = state.currentStreak, bestStreak = state.bestStreak)
        } else {
            val occs = state.markedOccurrences.filter {
                (filterStart == null || !it.date.isBefore(filterStart)) &&
                (filterEnd == null || !it.date.isAfter(filterEnd))
            }
            StreakCalculator.compute(occs)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = LocalNavigationContentPadding.current)
    ) {
        if (state.subjects.isEmpty()) item {
            EmptyListState("No attendance insights yet", "Add subjects and mark classes to see your attendance trends here.")
        }
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

        // Semesters comparison if multiple semesters exist
        if (state.allSemestersStats.size > 1) {
            item {
                Text(
                    "SEMESTERS OVERVIEW",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                )
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        state.allSemestersStats.forEach { stat ->
                            SemesterBarRow(stat, isActive = stat.semester.id == state.activeSemester?.id)
                        }
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

        // Streaks
        if (state.subjects.isNotEmpty()) {
            item {
                Text(
                    "STREAKS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                )
                StreakCards(
                    bestStreak = streakStats.bestStreak,
                    currentStreak = streakStats.currentStreak,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Bunk Simulation
        item {
            Text(
                "BUNK SIMULATION",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
            )
            Card(
                onClick = { showBunkSimulator = true },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Smart Bunk Simulator",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Simulate skipped classes, calculate target recovery, and plan holiday bridge breaks.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open Simulator",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (filteredHeatmap.isNotEmpty()) {
            item {
                Text(
                    "ACTIVITY",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                )
                AttendanceHeatmap(
                    presentCountByDate = filteredHeatmap,
                    filterStartDate = filterStart,
                    filterEndDate = filterEnd,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showBunkSimulator) {
        BunkSimulatorBottomSheet(
            initialSubjectId = null,
            onDismiss = { showBunkSimulator = false }
        )
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
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(112.dp)
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
            modifier = Modifier.widthIn(min = 40.dp)
        )
    }
}

@Composable
private fun SemesterBarRow(stat: SemesterAttendanceStat, isActive: Boolean) {
    val barColor = if (stat.total > 0 && stat.percentage < stat.semester.targetPercentage) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    val fraction by animateFloatAsState(
        targetValue = (stat.percentage / 100f).coerceIn(0f, 1f),
        animationSpec = Motion.gentle(),
        label = "semesterBarFraction"
    )

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(modifier = Modifier.width(112.dp)) {
            Text(
                stat.semester.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isActive) {
                Text(
                    "Active",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
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
            "${stat.percentage.toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = barColor,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.widthIn(min = 40.dp)
        )
    }
}
