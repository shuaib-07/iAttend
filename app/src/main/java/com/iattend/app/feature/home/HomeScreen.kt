package com.iattend.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.core.tutorial.LocalTutorialController
import com.iattend.app.core.tutorial.tutorialTarget
import com.iattend.app.core.ui.ProgressRing

@Composable
fun HomeScreen(
    onSubjectClick: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val tutorialSubjectId = LocalTutorialController.current?.tutorialSubjectId

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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 110.dp)
    ) {
        item {
            GreetingRow(collapseFraction = collapseFraction, modifier = Modifier.padding(bottom = 20.dp))
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
                modifier = Modifier.tutorialTarget("attendance_chart")
            )

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
}

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
                    "${summary.attended}/${summary.total} Occurred • ${summary.stats.totalOverall ?: summary.total} Total",
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
