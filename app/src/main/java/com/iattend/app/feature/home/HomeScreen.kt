package com.iattend.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.core.tutorial.LocalTutorialController
import com.iattend.app.core.tutorial.tutorialTarget
import com.iattend.app.core.ui.ProgressRing
import com.iattend.app.core.ui.LocalTimeFormat
import com.iattend.app.core.ui.formatDateShort
import com.iattend.app.core.ui.formatTime
import com.iattend.app.core.ui.modalsheet.ModalSheet
import java.time.LocalDate

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeScreen(
    onSubjectClick: (Long) -> Unit,
    onOpenCalendar: (LocalDate) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val tutorialSubjectId = LocalTutorialController.current?.tutorialSubjectId
    var showUnmarkedClasses by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val collapseThresholdPx = with(LocalDensity.current) { 80.dp.toPx() }
    val collapseFraction by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / collapseThresholdPx).coerceIn(0f, 1f)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 150.dp)
    ) {
        item {
            GreetingRow(
                collapseFraction = collapseFraction,
                attendancePercentage = state.overallMarkedPercentage,
                hasMarkedClasses = state.overallTotal > 0,
                requiredPercentage = state.requiredPercentageDefault,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            Text("Attendance", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Text(
                "Target: ${state.requiredPercentageDefault.toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            AttendanceTrendChart(
                points = state.attendanceTrend,
                thisMonthPercent = state.thisMonthPercent,
                thisSemesterPercent = state.thisSemesterPercent,
                currentPercent = state.overallPercentage,
                currentPercentCaption = state.overallPercentageCaption,
                semesterLabel = state.semesterLabel,
                semesterFallbackNotice = state.semesterFallbackNotice,
                modifier = Modifier.tutorialTarget("attendance_chart")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                Text(state.semesterLabel.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    HomeMetricCard(
                        "Occurred",
                        state.semesterOccurred,
                        denominator = state.semesterTotalClasses,
                        ringPercent = state.semesterTotalClasses?.let { percentOf(state.semesterOccurred, it) },
                        showRing = true,
                        modifier = Modifier.weight(1f)
                    )
                    HomeMetricCard(
                        "Attended",
                        state.semesterPresent,
                        denominator = state.semesterOccurred,
                        ringPercent = percentOfOrNull(state.semesterPresent, state.semesterOccurred),
                        showRing = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    HomeMetricCard(
                        "Absent",
                        state.semesterAbsent,
                        denominator = state.semesterOccurred,
                        ringPercent = percentOfOrNull(state.semesterAbsent, state.semesterOccurred),
                        ringAboveThreshold = false,
                        showRing = true,
                        modifier = Modifier.weight(1f)
                    )
                    HomeMetricCard("Unmarked", state.semesterUnmarked, Modifier.weight(1f)) {
                        showUnmarkedClasses = true
                    }
                }
            }

            Text(
                "SUBJECT BREAKDOWN",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
            )
        }

        items(state.subjects, key = { it.subject.id }) { summary ->
            val cardModifier = Modifier.animateItem().let {
                if (summary.subject.id == tutorialSubjectId) it.tutorialTarget("subject_summary_card") else it
            }
            SubjectSummaryCard(summary, onClick = { onSubjectClick(summary.subject.id) }, modifier = cardModifier)
        }
    }

    if (showUnmarkedClasses) {
        val subjectsById = state.subjects.associate { it.subject.id to it.subject }
        ModalSheet(visible = true, onVisibleChange = { showUnmarkedClasses = it }) {
            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.82f).padding(20.dp)) {
                Text("Past unmarked classes", style = MaterialTheme.typography.titleLarge)
                Text(
                    state.semesterLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
                HorizontalDivider()
                if (state.pastUnmarkedClasses.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("You’re all caught up", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(state.pastUnmarkedClasses, key = { it.id }) { occurrence ->
                            val subject = subjectsById[occurrence.subjectId]
                            val time = listOfNotNull(
                                occurrence.startTime?.let { formatTime(it, LocalTimeFormat.current) },
                                occurrence.endTime?.let { formatTime(it, LocalTimeFormat.current) }
                            ).joinToString(" – ")
                            val room = occurrence.roomNumber?.takeIf { it.isNotBlank() }?.let { " · Room $it" }.orEmpty()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable {
                                        showUnmarkedClasses = false
                                        onOpenCalendar(occurrence.date)
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp)
                            ) {
                                Text(subject?.name ?: "Class", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${formatDateShort(occurrence.date)}${if (time.isNotBlank()) " · $time" else ""}$room",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
                Button(onClick = { showUnmarkedClasses = false }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun HomeMetricCard(
    label: String,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) = HomeMetricCard(label, count, null, null, true, false, modifier, onClick)

@Composable
private fun HomeMetricCard(
    label: String,
    count: Int,
    denominator: Int?,
    ringPercent: Float?,
    ringAboveThreshold: Boolean = true,
    showRing: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 62.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (denominator != null) "$count / $denominator" else count.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    softWrap = false
                )
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (showRing) {
                ProgressRing(
                    percentage = ringPercent ?: 0f,
                    aboveThreshold = ringAboveThreshold,
                    size = 42.dp,
                    strokeWidth = 4.dp,
                    textStyle = MaterialTheme.typography.labelSmall,
                    showPercentageText = ringPercent != null
                )
            }
        }
    }
}

private fun percentOf(value: Int, total: Int): Float =
    if (total <= 0) 0f else value * 100f / total

private fun percentOfOrNull(value: Int, total: Int): Float? =
    if (total <= 0) null else percentOf(value, total)

@Composable
private fun SubjectSummaryCard(summary: SubjectSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(16.dp).background(Color(summary.subject.colorArgb), CircleShape))
            Column(modifier = Modifier.weight(1f)) {
                Text(summary.subject.name, style = MaterialTheme.typography.bodyLarge)
                summary.subject.teacherName?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    "${summary.total} classes occurred · ${summary.attended} Present · ${summary.stats.totalOverall ?: summary.total} Total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val needed = summary.stats.classesNeeded
                val canSkip = summary.stats.classesCanSkip
                Text(
                    when {
                        needed == null -> "Projection: N/A"
                        needed > 0 -> "Needs $needed more to hit ${summary.requiredPercentage.toInt()}%"
                        else -> "Can skip $canSkip and stay above ${summary.requiredPercentage.toInt()}%"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            val belowThreshold = summary.total > 0 && summary.percentage < summary.requiredPercentage
            ProgressRing(
                percentage = summary.percentage,
                aboveThreshold = !belowThreshold,
                size = 52.dp,
                strokeWidth = 5.dp,
                textStyle = MaterialTheme.typography.labelMedium
            )
        }
    }
}
