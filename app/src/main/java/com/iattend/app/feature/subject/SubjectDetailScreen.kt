package com.iattend.app.feature.subject

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.core.data.db.Assessment
import com.iattend.app.core.data.db.AssessmentAttemptStatus
import com.iattend.app.core.data.db.ClassOccurrence
import com.iattend.app.core.data.db.OccurrenceSource
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.core.data.db.WeightageSystem
import com.iattend.app.core.datastore.CalendarViewMode
import com.iattend.app.core.domain.assessment.SubjectGradingSummary
import com.iattend.app.core.ui.LocalTimeFormat
import com.iattend.app.core.ui.ProgressRing
import com.iattend.app.core.ui.SpringAlertDialog
import com.iattend.app.core.ui.SquircleIconButton
import com.iattend.app.core.ui.formatDateShort
import com.iattend.app.core.ui.formatTime
import com.iattend.app.core.ui.hapticClick
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val historyDateFormat = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onAssessments: (Long) -> Unit,
    onEditAssessment: ((subjectId: Long, assessmentId: Long) -> Unit)? = null,
    viewModel: SubjectDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var occurrenceToDelete by remember { mutableStateOf<ClassOccurrence?>(null) }
    var linkedTestsForDeletion by remember { mutableStateOf<List<Assessment>>(emptyList()) }

    var scoreConflictData by remember { mutableStateOf<Pair<ClassOccurrence, OccurrenceStatus>?>(null) }
    var conflictingTests by remember { mutableStateOf<List<Assessment>>(emptyList()) }

    LaunchedEffect(Unit) { viewModel.deleted.collect { onBack() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.subject?.name ?: "Subject", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                navigationIcon = {
                    SquircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)
                },
                actions = {
                    state.subject?.let { subject ->
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                if (state.viewMode == CalendarViewMode.MONTH) Icons.Default.ViewWeek else Icons.Default.CalendarMonth,
                                contentDescription = "Toggle view mode"
                            )
                        }
                        IconButton(onClick = { onEdit(subject.id) }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                        IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                    }
                }
            )
        }
    ) { padding ->
        val stats = state.stats
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Tests & Exams Navigation Header Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clickable { state.subject?.let { onAssessments(it.id) } }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Tests & Exams", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Manage assessments, weightages and scores",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }

            // Subject Grading Summary Card (if grading system configured)
            state.gradingSummary?.let { summary ->
                item {
                    SubjectGradingSummaryCard(
                        summary = summary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            // Attendance Stats Card
            if (stats != null) {
                item {
                    val belowThreshold = stats.totalSoFar > 0 && stats.percentage < state.requiredPercentage
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProgressRing(percentage = stats.percentage, aboveThreshold = !belowThreshold, size = 88.dp)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${stats.attended} / ${stats.totalSoFar}", style = MaterialTheme.typography.headlineSmall)
                                Text("Required: ${state.requiredPercentage.toInt()}%")
                                when {
                                    stats.totalOverall == null -> Text("Projection: N/A", style = MaterialTheme.typography.bodyMedium)
                                    else -> {
                                        Text("Total: ${stats.totalOverall}, remaining: ${stats.remaining}", style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            if ((stats.classesNeeded ?: 0) > 0) "Needs ${stats.classesNeeded} more to hit target"
                                            else "Can skip ${stats.classesCanSkip} and stay above target",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Subject Filtered Calendar Section
            item {
                SubjectCalendarSection(
                    selectedDate = state.selectedDate,
                    calendarMonth = state.calendarMonth,
                    viewMode = state.viewMode,
                    attendanceStatusByDate = state.attendanceStatusByDate,
                    assessmentDates = state.assessmentDates,
                    onDateSelected = viewModel::selectDate,
                    onPreviousPeriod = viewModel::previousPeriod,
                    onNextPeriod = viewModel::nextPeriod,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Selected Day Schedule Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        state.selectedDate.format(historyDateFormat),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "${state.selectedDateOccurrences.size} class${if (state.selectedDateOccurrences.size != 1) "es" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.selectedDateOccurrences.isEmpty() && state.selectedDateAssessments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No classes or independent assessments on this day.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Occurrences on selected date
                items(state.selectedDateOccurrences, key = { it.id }) { occurrence ->
                    val linkedTests = state.linkedAssessmentsByOccurrenceId[occurrence.id] ?: emptyList()
                    SubjectOccurrenceCard(
                        occurrence = occurrence,
                        linkedTests = linkedTests,
                        onClick = {
                            viewModel.cycleStatus(
                                occurrence = occurrence,
                                onConflict = { conflicts, targetStatus ->
                                    conflictingTests = conflicts
                                    scoreConflictData = occurrence to targetStatus
                                }
                            )
                        },
                        onDelete = {
                            scope.launch {
                                val tests = viewModel.getLinkedTests(occurrence.id)
                                linkedTestsForDeletion = tests
                                occurrenceToDelete = occurrence
                            }
                        },
                        onSetCancelReason = { reason -> viewModel.setCancelReason(occurrence, reason) },
                        onTestClick = { testId ->
                            state.subject?.id?.let { subId -> onEditAssessment?.invoke(subId, testId) }
                        }
                    )
                }

                // Independent assessments on selected date
                if (state.selectedDateAssessments.isNotEmpty()) {
                    item {
                        Text(
                            "Independent Assessments",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                        )
                    }
                    items(state.selectedDateAssessments, key = { it.id }) { assessment ->
                        IndependentAssessmentCard(
                            assessment = assessment,
                            onClick = {
                                state.subject?.id?.let { subId -> onEditAssessment?.invoke(subId, assessment.id) }
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete Subject Confirmation Dialog
    if (showDeleteConfirm) {
        SpringAlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete subject?") },
            text = { Text("This removes ${state.subject?.name} and its entire attendance history and assessments. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; viewModel.delete() }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    // Delete Class Occurrence Confirmation Dialog
    occurrenceToDelete?.let { occ ->
        val hasLinkedTests = linkedTestsForDeletion.isNotEmpty()
        val hasScoredTests = linkedTestsForDeletion.any { it.obtainedMarks != null }

        SpringAlertDialog(
            onDismissRequest = { occurrenceToDelete = null },
            title = { Text(if (occ.source == OccurrenceSource.EXTRA) "Delete extra class?" else "Delete class occurrence?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This will remove this class from your schedule.")
                    if (hasScoredTests) {
                        Text(
                            "Warning: One or more linked tests have scored marks entered.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    if (hasLinkedTests) {
                        Text("This class has ${linkedTestsForDeletion.size} linked test(s). You can delete them together or keep them as independent tests.")
                    }
                }
            },
            confirmButton = {
                if (hasLinkedTests) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                viewModel.deleteOccurrence(occ, deleteLinkedTests = true)
                                occurrenceToDelete = null
                            }
                        ) {
                            Text("Delete with tests", color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(
                            onClick = {
                                viewModel.deleteOccurrence(occ, deleteLinkedTests = false)
                                occurrenceToDelete = null
                            }
                        ) {
                            Text("Keep tests")
                        }
                    }
                } else {
                    TextButton(
                        onClick = {
                            viewModel.deleteOccurrence(occ, deleteLinkedTests = false)
                            occurrenceToDelete = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { occurrenceToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // Score Clearing Conflict Dialog
    scoreConflictData?.let { (occ, targetStatus) ->
        val testNames = conflictingTests.joinToString { it.title ?: it.type.name }
        SpringAlertDialog(
            onDismissRequest = { scoreConflictData = null },
            title = { Text("Clear assessment scores?") },
            text = {
                Text("Changing attendance to ${targetStatus.name} will mark linked test(s) ($testNames) as Missed or Pending and clear their entered scores. Are you sure?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateOccurrenceStatus(occ, targetStatus, forceClearScores = true)
                        scoreConflictData = null
                    }
                ) {
                    Text("Clear and update", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { scoreConflictData = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SubjectGradingSummaryCard(
    summary: SubjectGradingSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Assessment Weightage",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    when (summary.system) {
                        WeightageSystem.PERCENTAGE -> "Percentage (100%)"
                        WeightageSystem.WEIGHTED_MARKS -> "Weighted Marks"
                        null -> "None"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Current Weighted Score",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        summary.earnedPercentageOfSubjectTotal?.let { "%.1f%%".format(it) }
                            ?: "%.1f marks".format(summary.earnedContribution),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Graded Weight",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        when (summary.system) {
                            WeightageSystem.PERCENTAGE -> "%.1f%% / 100%%".format(summary.gradedWeight)
                            WeightageSystem.WEIGHTED_MARKS -> "%.1f / %.1f".format(summary.gradedWeight, summary.maxWeightAllowed ?: summary.totalConfiguredWeight)
                            null -> "-"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            if (summary.exceedsMaxWeight) {
                Text(
                    "Warning: Total configured weight exceeds limit (${summary.maxWeightAllowed})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SubjectCalendarSection(
    selectedDate: LocalDate,
    calendarMonth: YearMonth,
    viewMode: CalendarViewMode,
    attendanceStatusByDate: Map<LocalDate, OccurrenceStatus>,
    assessmentDates: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    modifier: Modifier = Modifier
) {
    val weekdays = listOf(
        java.time.DayOfWeek.SUNDAY, java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY,
        java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.THURSDAY, java.time.DayOfWeek.FRIDAY,
        java.time.DayOfWeek.SATURDAY
    )

    val dates = if (viewMode == CalendarViewMode.MONTH) {
        val firstDate = calendarMonth.atDay(1)
        val gridStart = firstDate.minusDays((firstDate.dayOfWeek.value % 7).toLong())
        val weekCount = ((firstDate.dayOfWeek.value % 7) + calendarMonth.lengthOfMonth() + 6) / 7
        (0 until weekCount * 7).map { gridStart.plusDays(it.toLong()) }
    } else {
        val gridStart = selectedDate.minusDays((selectedDate.dayOfWeek.value % 7).toLong())
        (0 until 7).map { gridStart.plusDays(it.toLong()) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (viewMode == CalendarViewMode.MONTH)
                    calendarMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                else
                    "Week of ${formatDateShort(dates.first())}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Row {
                IconButton(onClick = onPreviousPeriod) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                }
                IconButton(onClick = onNextPeriod) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp)) {
            weekdays.forEach { day ->
                Text(
                    day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        dates.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    val status = attendanceStatusByDate[date]
                    val hasAssessment = assessmentDates.contains(date)
                    val inMonth = date.month == calendarMonth.month && date.year == calendarMonth.year
                    val selected = date == selectedDate
                    val today = date == LocalDate.now()
                    val shape = MaterialTheme.shapes.medium

                    val statusColor = when (status) {
                        OccurrenceStatus.UNMARKED -> Color(0xFFFFE4A8)
                        OccurrenceStatus.ABSENT -> Color(0xFFFFC9C9)
                        OccurrenceStatus.PRESENT -> Color(0xFFC9EFD2)
                        OccurrenceStatus.CANCELLED -> Color(0xFFE1E4E8)
                        null -> if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    }

                    val numberColor = when {
                        status != null -> Color(0xFF202124)
                        selected -> MaterialTheme.colorScheme.onPrimaryContainer
                        today -> MaterialTheme.colorScheme.primary
                        inMonth -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(2.dp)
                            .height(48.dp)
                            .clip(shape)
                            .background(statusColor)
                            .then(
                                if (selected) Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), shape)
                                else if (today) Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary), shape)
                                else Modifier
                            )
                            .clickable { onDateSelected(date) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(date.dayOfMonth.toString(), color = numberColor, style = MaterialTheme.typography.bodyMedium)
                            if (hasAssessment) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectOccurrenceCard(
    occurrence: ClassOccurrence,
    linkedTests: List<Assessment>,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onSetCancelReason: (String?) -> Unit,
    onTestClick: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showReasonDialog by remember(occurrence.id) { mutableStateOf(false) }
    val markable = !occurrence.date.isAfter(LocalDate.now())
    val statusColor = when (occurrence.status) {
        OccurrenceStatus.PRESENT -> MaterialTheme.colorScheme.primary
        OccurrenceStatus.ABSENT -> MaterialTheme.colorScheme.error
        OccurrenceStatus.CANCELLED, OccurrenceStatus.UNMARKED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .alpha(if (markable) 1f else 0.8f)
            .let { if (markable) it.clickable(onClick = hapticClick(onClick)) else it }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val time = occurrence.startTime?.let { formatTime(it, LocalTimeFormat.current) }
                    Text(
                        listOfNotNull(
                            time?.let { start -> start + (occurrence.endTime?.let { " - ${formatTime(it, LocalTimeFormat.current)}" } ?: "") },
                            occurrence.roomNumber?.takeIf { it.isNotBlank() }?.let { "Room $it" }
                        ).joinToString(" · ").ifBlank { "Class" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    if (occurrence.source == OccurrenceSource.EXTRA) {
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Extra",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        occurrence.status.name,
                        color = statusColor,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (occurrence.status == OccurrenceStatus.CANCELLED) {
                        IconButton(onClick = { showReasonDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit cancel reason")
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete class",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (occurrence.status == OccurrenceStatus.CANCELLED && !occurrence.cancelReason.isNullOrBlank()) {
                Text(
                    occurrence.cancelReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Nested Linked Tests
            linkedTests.forEach { test ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .then(if (onTestClick != null) Modifier.clickable { onTestClick(test.id) } else Modifier),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    test.type.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    test.title ?: test.type.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                            val marksText = test.totalMarks?.let { marks ->
                                val formatted = if (marks % 1.0 == 0.0) marks.toInt().toString() else marks.toString()
                                "Total: $formatted"
                            }
                            val weightText = test.weight?.let { w ->
                                val formatted = if (w % 1.0 == 0.0) w.toInt().toString() else w.toString()
                                "Weight: $formatted"
                            }
                            val subInfo = listOfNotNull(marksText, weightText).joinToString(" · ")
                            if (subInfo.isNotBlank()) {
                                Text(
                                    subInfo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (test.attemptStatus != AssessmentAttemptStatus.PENDING) {
                            Text(
                                test.attemptStatus.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (test.attemptStatus == AssessmentAttemptStatus.ATTEMPTED)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (showReasonDialog) {
        var draftReason by remember { mutableStateOf(occurrence.cancelReason ?: "") }
        SpringAlertDialog(
            onDismissRequest = { showReasonDialog = false },
            title = { Text("Cancellation reason") },
            text = {
                OutlinedTextField(
                    value = draftReason,
                    onValueChange = { draftReason = it },
                    label = { Text("Reason (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { onSetCancelReason(draftReason); showReasonDialog = false }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showReasonDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun IndependentAssessmentCard(
    assessment: Assessment,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        assessment.type.name,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        assessment.title ?: assessment.type.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
                val timeStr = formatTime(assessment.startTime, LocalTimeFormat.current)
                val marksStr = assessment.totalMarks?.let { marks ->
                    val formatted = if (marks % 1.0 == 0.0) marks.toInt().toString() else marks.toString()
                    "$formatted marks"
                }
                val weightStr = assessment.weight?.let { w ->
                    val formatted = if (w % 1.0 == 0.0) w.toInt().toString() else w.toString()
                    "weight $formatted"
                }
                Text(
                    listOfNotNull(timeStr, marksStr, weightStr).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (assessment.needsRescheduling) {
                Text(
                    "Needs Rescheduling",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (assessment.attemptStatus != AssessmentAttemptStatus.PENDING) {
                Text(
                    assessment.attemptStatus.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (assessment.attemptStatus == AssessmentAttemptStatus.ATTEMPTED)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
