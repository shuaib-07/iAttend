package com.iattend.app.feature.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.iattend.app.core.navigation.LocalNavigationContentPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.core.data.db.Assessment
import com.iattend.app.core.data.db.AssessmentAttemptStatus
import com.iattend.app.core.data.db.AssessmentType
import com.iattend.app.core.data.db.ClassOccurrence
import com.iattend.app.core.data.db.OccurrenceSource
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.core.datastore.CalendarViewMode
import com.iattend.app.core.tutorial.LocalTutorialController
import com.iattend.app.core.tutorial.TutorialSignal
import com.iattend.app.core.tutorial.tutorialTarget
import com.iattend.app.core.ui.LocalTimeFormat
import com.iattend.app.core.ui.ProgressRing
import com.iattend.app.core.ui.SpringAlertDialog
import com.iattend.app.core.ui.formatDateShort
import com.iattend.app.core.ui.formatTime
import com.iattend.app.core.ui.modalsheet.ModalSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private const val SWIPE_THRESHOLD_PX = 120f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onAddExtra: (LocalDate) -> Unit,
    onEditExtra: (Long) -> Unit = {},
    onEditScheduledClass: ((versionId: Long, dayOfWeek: DayOfWeek, slotId: Long?) -> Unit)? = null,
    onSubjectClick: (Long) -> Unit = {},
    onAssessmentClick: ((subjectId: Long, assessmentId: Long) -> Unit)? = null,
    initialDate: String? = null,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val visibleWeek by viewModel.visibleWeek.collectAsState()
    val rows by viewModel.rows.collectAsState()
    val isPast by viewModel.isPast.collectAsState()
    val assessmentRows by viewModel.assessmentsForSelectedDate.collectAsState()
    val assessmentDates by viewModel.assessmentDates.collectAsState()
    val assessmentDatesInWeek by viewModel.assessmentDatesInWeek.collectAsState()
    val attendanceStatusByDate by viewModel.attendanceStatusByDate.collectAsState()
    val monthAttendancePercentage by viewModel.monthAttendancePercentage.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var showDatePicker by remember { mutableStateOf(false) }
    var selectedOccurrenceForMenu by remember { mutableStateOf<DayOccurrenceRow?>(null) }

    var occurrenceToDelete by remember { mutableStateOf<ClassOccurrence?>(null) }
    var linkedTestsForDeletion by remember { mutableStateOf<List<Assessment>>(emptyList()) }

    var scoreConflictData by remember { mutableStateOf<Pair<ClassOccurrence, OccurrenceStatus>?>(null) }
    var conflictingTests by remember { mutableStateOf<List<Assessment>>(emptyList()) }

    var assessmentScoreConflict by remember { mutableStateOf<Pair<Assessment, AssessmentAttemptStatus>?>(null) }

    LaunchedEffect(initialDate) {
        initialDate?.let { runCatching { LocalDate.parse(it) }.getOrNull()?.let(viewModel::selectDate) }
    }

    fun handleAssessmentStatusAttempt(assessment: Assessment, targetStatus: AssessmentAttemptStatus) {
        if (targetStatus == AssessmentAttemptStatus.MISSED || targetStatus == AssessmentAttemptStatus.PENDING) {
            if (assessment.obtainedMarks != null) {
                assessmentScoreConflict = assessment to targetStatus
                return
            }
        }
        viewModel.updateAssessmentAttemptStatus(assessment.id, targetStatus)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = LocalNavigationContentPadding.current)
    ) {
        item(key = "calendar_header") {
            Column(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { showDatePicker = true }
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AnimatedContent(
                            targetState = YearMonth.from(selectedDate),
                            transitionSpec = {
                                if (targetState.isAfter(initialState)) {
                                    (slideInHorizontally(
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                                        initialOffsetX = { 20 }
                                    ) + fadeIn(tween(200))).togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                                            targetOffsetX = { -20 }
                                        ) + fadeOut(tween(150))
                                    )
                                } else {
                                    (slideInHorizontally(
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                                        initialOffsetX = { -20 }
                                    ) + fadeIn(tween(200))).togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                                            targetOffsetX = { 20 }
                                        ) + fadeOut(tween(150))
                                    )
                                }
                            },
                            label = "MonthHeaderTitleSlide"
                        ) { ym ->
                            Text(
                                text = "${ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${ym.year}",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onAddExtra(selectedDate) }) {
                            Icon(Icons.Default.Add, contentDescription = "Add extra class")
                        }
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.setViewMode(
                                if (viewMode == CalendarViewMode.WEEK) CalendarViewMode.MONTH else CalendarViewMode.WEEK
                            )
                        }) {
                            Icon(
                                if (viewMode == CalendarViewMode.WEEK) Icons.Default.CalendarMonth else Icons.Default.ViewWeek,
                                contentDescription = "Toggle calendar view"
                            )
                        }
                    }
                }

                var dragAccum by remember { mutableFloatStateOf(0f) }

                if (viewMode == CalendarViewMode.WEEK) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.previousWeek()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous week")
                        }
                        AnimatedContent(
                            targetState = visibleWeek.firstOrNull() ?: LocalDate.now(),
                            transitionSpec = {
                                if (targetState.isAfter(initialState)) {
                                    (slideInHorizontally(
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                                        initialOffsetX = { fullWidth -> fullWidth }
                                    ) + fadeIn(tween(220))).togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                                            targetOffsetX = { fullWidth -> -fullWidth }
                                        ) + fadeOut(tween(180))
                                    )
                                } else {
                                    (slideInHorizontally(
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                                        initialOffsetX = { fullWidth -> -fullWidth }
                                    ) + fadeIn(tween(220))).togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                                            targetOffsetX = { fullWidth -> fullWidth }
                                        ) + fadeOut(tween(180))
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .pointerInputDragWeek(
                                    onDragDelta = { dragAccum += it },
                                    onDragEnd = {
                                        if (dragAccum <= -SWIPE_THRESHOLD_PX) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.nextWeek()
                                        } else if (dragAccum >= SWIPE_THRESHOLD_PX) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.previousWeek()
                                        }
                                        dragAccum = 0f
                                    }
                                ),
                            label = "WeekSlideTransition"
                        ) { weekStart ->
                            val currentWeek = remember(weekStart) {
                                (0..6).map { weekStart.plusDays(it.toLong()) }
                            }
                            WeekStripRow(
                                week = currentWeek,
                                selectedDate = selectedDate,
                                assessmentDatesInWeek = assessmentDatesInWeek,
                                onDateSelected = { date ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.selectDate(date)
                                }
                            )
                        }
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.nextWeek()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next week")
                        }
                    }
                } else {
                    MonthCalendarGrid(
                        selectedDate = selectedDate,
                        attendanceStatusByDate = attendanceStatusByDate,
                        attendancePercentage = monthAttendancePercentage,
                        assessmentDates = assessmentDates,
                        onDateSelected = viewModel::selectDate,
                        onPreviousMonth = viewModel::previousMonth,
                        onNextMonth = viewModel::nextMonth
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Schedule · ${selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}, ${formatDateShort(selectedDate)}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "${rows.size} ${if (rows.size == 1) "class" else "classes"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

            }
        }

        if (rows.isEmpty() && assessmentRows.isEmpty()) {
            item(key = "empty_schedule") {
                Box(modifier = Modifier.fillParentMaxHeight(0.5f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No classes scheduled", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(assessmentRows, key = { "a${it.assessment.id}" }) { row ->
                AssessmentCard(
                    row = row,
                    onClick = {
                        row.subject?.let { sub ->
                            onAssessmentClick?.invoke(sub.id, row.assessment.id)
                        }
                    },
                    onAttemptStatusChange = { newStatus ->
                        handleAssessmentStatusAttempt(row.assessment, newStatus)
                    },
                    modifier = Modifier.animateItem()
                )
            }
            items(rows, key = { it.occurrence.id }) { row ->
                TimelineRow(
                    row = row,
                    isPast = isPast,
                    onSubjectClick = { row.subject?.id?.let(onSubjectClick) },
                    onMark = { status ->
                        scope.launch {
                            val conflicts = viewModel.findScoredLinkedTestConflicts(row.occurrence.id, status)
                            if (conflicts.isNotEmpty()) {
                                conflictingTests = conflicts
                                scoreConflictData = row.occurrence to status
                            } else {
                                viewModel.mark(row.occurrence, status)
                            }
                        }
                    },
                    onLongClick = {
                        selectedOccurrenceForMenu = row
                    },
                    onSetCancelReason = { reason -> viewModel.setCancelReason(row.occurrence, reason) },
                    onTestClick = { testId ->
                        row.subject?.id?.let { subId -> onAssessmentClick?.invoke(subId, testId) }
                    },
                    onTestAttemptStatusChange = { test, targetStatus ->
                        handleAssessmentStatusAttempt(test, targetStatus)
                    },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        viewModel.selectDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = state)
        }
    }

    // Long press action sheet for any class occurrence
    selectedOccurrenceForMenu?.let { row ->
        val occ = row.occurrence
        ModalSheet(
            visible = true,
            onVisibleChange = { if (!it) selectedOccurrenceForMenu = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp, top = 8.dp)
            ) {
                Text(
                    row.subject?.name ?: "Class",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${occ.startTime?.let { formatTime(it, LocalTimeFormat.current) } ?: "?"} - ${occ.endTime?.let { formatTime(it, LocalTimeFormat.current) } ?: "?"}" +
                        (occ.roomNumber?.takeIf { it.isNotBlank() }?.let { " · Room $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable {
                            val occurrenceToEdit = occ
                            selectedOccurrenceForMenu = null
                            if (occurrenceToEdit.source == OccurrenceSource.EXTRA) {
                                onEditExtra(occurrenceToEdit.id)
                            } else {
                                scope.launch {
                                    val target = viewModel.findTimetableEditTarget(occurrenceToEdit)
                                    if (target != null) {
                                        onEditScheduledClass?.invoke(target.versionId, target.dayOfWeek, target.slotId)
                                    }
                                }
                            }
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Edit Class", style = MaterialTheme.typography.bodyLarge)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable {
                            val occurrenceToDeleteTarget = occ
                            selectedOccurrenceForMenu = null
                            scope.launch {
                                linkedTestsForDeletion = viewModel.getLinkedTests(occurrenceToDeleteTarget.id)
                                occurrenceToDelete = occurrenceToDeleteTarget
                            }
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text(
                        "Delete Class",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // Delete occurrence dialog
    occurrenceToDelete?.let { occ ->
        val hasLinkedTests = linkedTestsForDeletion.isNotEmpty()
        val hasScoredTests = linkedTestsForDeletion.any { it.obtainedMarks != null }

        SpringAlertDialog(
            onDismissRequest = { occurrenceToDelete = null },
            title = { Text(if (occ.source == OccurrenceSource.EXTRA) "Delete extra class?" else "Delete class occurrence?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Are you sure you want to delete this scheduled class?")
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

    // Score Clearing Conflict Dialog for Class Attendance Changes
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
                        viewModel.mark(occ, targetStatus, forceClearScores = true)
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

    // Score Clearing Dialog for Assessment Attempt Status Changes
    assessmentScoreConflict?.let { (assessment, targetStatus) ->
        SpringAlertDialog(
            onDismissRequest = { assessmentScoreConflict = null },
            title = { Text("Clear assessment marks?") },
            text = {
                Text("Setting ${assessment.title ?: "this assessment"} to ${targetStatus.name.lowercase()} will clear the entered score (${assessment.obtainedMarks} marks). Are you sure?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateAssessmentAttemptStatus(assessment.id, targetStatus, clearScore = true)
                        assessmentScoreConflict = null
                    }
                ) {
                    Text("Clear and update", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { assessmentScoreConflict = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MonthCalendarGrid(
    selectedDate: LocalDate,
    attendanceStatusByDate: Map<LocalDate, OccurrenceStatus>,
    attendancePercentage: Float?,
    assessmentDates: Set<LocalDate> = emptySet(),
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val month = YearMonth.from(selectedDate)
    val weekdays = listOf(
        DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY
    )
    var dragAccum by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                attendancePercentage?.let { "${it.toInt()}% attendance" } ?: "Attendance —",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPreviousMonth()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month")
            }
            IconButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onNextMonth()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month")
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragAccum <= -SWIPE_THRESHOLD_PX) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNextMonth()
                            } else if (dragAccum >= SWIPE_THRESHOLD_PX) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onPreviousMonth()
                            }
                            dragAccum = 0f
                        },
                        onHorizontalDrag = { _, dragAmount -> dragAccum += dragAmount }
                    )
                }
        ) {
            AnimatedContent(
                targetState = month,
                transitionSpec = {
                    if (targetState.isAfter(initialState)) {
                        (slideInHorizontally(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                            initialOffsetX = { fullWidth -> fullWidth }
                        ) + fadeIn(tween(220))).togetherWith(
                            slideOutHorizontally(
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                                targetOffsetX = { fullWidth -> -fullWidth }
                            ) + fadeOut(tween(180))
                        )
                    } else {
                        (slideInHorizontally(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                            initialOffsetX = { fullWidth -> -fullWidth }
                        ) + fadeIn(tween(220))).togetherWith(
                            slideOutHorizontally(
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                                targetOffsetX = { fullWidth -> fullWidth }
                            ) + fadeOut(tween(180))
                        )
                    }
                },
                label = "MonthGridTransition"
            ) { currentMonth ->
                MonthGridBody(
                    month = currentMonth,
                    selectedDate = selectedDate,
                    attendanceStatusByDate = attendanceStatusByDate,
                    assessmentDates = assessmentDates,
                    onDateSelected = onDateSelected
                )
            }
        }
    }
}

@Composable
private fun MonthGridBody(
    month: YearMonth,
    selectedDate: LocalDate,
    attendanceStatusByDate: Map<LocalDate, OccurrenceStatus>,
    assessmentDates: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit
) {
    val firstDate = month.atDay(1)
    val gridStart = firstDate.minusDays((firstDate.dayOfWeek.value % 7).toLong())
    val weekCount = 6
    val dates = remember(month) { (0 until weekCount * 7).map { gridStart.plusDays(it.toLong()) } }
    val haptic = LocalHapticFeedback.current
    val selectedCellIndex = dates.indexOf(selectedDate)

    val hasSelection = selectedCellIndex >= 0
    val targetCol = if (hasSelection) (selectedCellIndex % 7).toFloat() else 0f
    val targetRow = if (hasSelection) (selectedCellIndex / 7).toFloat() else 0f

    val animCol by animateFloatAsState(
        targetValue = targetCol,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
        label = "monthCol"
    )
    val animRow by animateFloatAsState(
        targetValue = targetRow,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
        label = "monthRow"
    )
    val selectionAlpha by animateFloatAsState(
        targetValue = if (hasSelection) 1f else 0f,
        animationSpec = tween(150),
        label = "monthAlpha"
    )
    val selectionScale by animateFloatAsState(
        targetValue = if (hasSelection) 1f else 0.7f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
        label = "monthScale"
    )

    val shape = MaterialTheme.shapes.medium
    val cellHeight = 48.dp

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val colWidth = maxWidth / 7f

        // 1. Grid of Day Cells
        Column(modifier = Modifier.fillMaxWidth()) {
            dates.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        val status = attendanceStatusByDate[date]
                        val hasAssessment = assessmentDates.contains(date)
                        val inMonth = date.month == month.month && date.year == month.year
                        val selected = date == selectedDate
                        val today = date == LocalDate.now()
                        val statusColor = when (status) {
                            OccurrenceStatus.UNMARKED -> Color(0xFFFFE4A8)
                            OccurrenceStatus.ABSENT -> Color(0xFFFFC9C9)
                            OccurrenceStatus.PRESENT -> Color(0xFFC9EFD2)
                            OccurrenceStatus.CANCELLED -> Color(0xFFE1E4E8)
                            null -> Color.Transparent
                        }
                        val numberColor = when {
                            status != null -> Color(0xFF202124)
                            selected -> MaterialTheme.colorScheme.primary
                            today -> MaterialTheme.colorScheme.primary
                            inMonth -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(cellHeight)
                                .padding(2.dp)
                                .clip(shape)
                                .background(statusColor)
                                .then(
                                    if (today && !selected) Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary), shape)
                                    else Modifier
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDateSelected(date)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    date.dayOfMonth.toString(),
                                    color = numberColor,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected || today) FontWeight.Bold else FontWeight.Normal
                                )
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

        // 2. Gliding Direction-Aware Selection Indicator (Option A: 2dp primary border + 15% primary tint)
        Box(
            modifier = Modifier
                .width(colWidth)
                .height(cellHeight)
                .graphicsLayer {
                    translationX = animCol * size.width
                    translationY = animRow * size.height
                    alpha = selectionAlpha
                    scaleX = selectionScale
                    scaleY = selectionScale
                }
                .padding(2.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), shape)
        )
    }
}

private fun Modifier.pointerInputDragWeek(onDragDelta: (Float) -> Unit, onDragEnd: () -> Unit): Modifier =
    this.pointerInput(Unit) {
        detectHorizontalDragGestures(
            onDragEnd = onDragEnd,
            onHorizontalDrag = { _, dragAmount -> onDragDelta(dragAmount) }
        )
    }

@Composable
private fun WeekStripRow(
    week: List<LocalDate>,
    selectedDate: LocalDate,
    assessmentDatesInWeek: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = week.indexOf(selectedDate)
    val hasSelection = selectedIndex >= 0

    val animIndex by animateFloatAsState(
        targetValue = if (hasSelection) selectedIndex.toFloat() else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
        label = "weekPillX"
    )
    val selectionAlpha by animateFloatAsState(
        targetValue = if (hasSelection) 1f else 0f,
        animationSpec = tween(150),
        label = "weekPillAlpha"
    )
    val selectionScale by animateFloatAsState(
        targetValue = if (hasSelection) 1f else 0.7f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
        label = "weekPillScale"
    )

    val cellHeight = 64.dp

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val colWidth = maxWidth / 7f

        // Background gliding selection pill
        Box(
            modifier = Modifier
                .width(colWidth)
                .height(cellHeight)
                .graphicsLayer {
                    translationX = animIndex * size.width
                    alpha = selectionAlpha
                    scaleX = selectionScale
                    scaleY = selectionScale
                }
                .padding(horizontal = 2.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.primaryContainer)
        )

        // Row of 7 day cells
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(cellHeight),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            week.forEach { date ->
                WeekDayCell(
                    date = date,
                    selected = date == selectedDate,
                    hasAssessment = date in assessmentDatesInWeek,
                    onClick = { onDateSelected(date) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WeekDayCell(
    date: LocalDate,
    selected: Boolean,
    hasAssessment: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = date == LocalDate.now()
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 2.dp)
            .clip(MaterialTheme.shapes.medium)
            .then(
                if (today && !selected) Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary), MaterialTheme.shapes.medium)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected || today) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
        if (hasAssessment) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary)
            )
        } else {
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun AssessmentCard(
    row: DayAssessmentRow,
    onClick: () -> Unit,
    onAttemptStatusChange: (AssessmentAttemptStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val assessment = row.assessment
    val subjectColor = row.subject?.let { Color(it.colorArgb) } ?: MaterialTheme.colorScheme.error
    val markable = !assessment.date.isAfter(LocalDate.now())
    val haptics = LocalHapticFeedback.current

    Row(modifier = modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Column(modifier = Modifier.width(64.dp * LocalDensity.current.fontScale)) {
            Text(formatTime(assessment.startTime, LocalTimeFormat.current), style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
        }
        Column(
            modifier = Modifier.width(16.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(subjectColor))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (assessment.type == AssessmentType.EXAM) "Exam" else "Test",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    softWrap = false
                )
                if (assessment.needsRescheduling) {
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(MaterialTheme.colorScheme.error)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "Needs rescheduling",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
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

            Text(
                assessment.title?.takeIf { it.isNotBlank() } ?: (row.subject?.name ?: "?"),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            val marksText = assessment.totalMarks?.let { marks ->
                val formatted = if (marks % 1.0 == 0.0) marks.toInt().toString() else marks.toString()
                "$formatted marks"
            }
            val weightText = assessment.weight?.let { w ->
                val formatted = if (w % 1.0 == 0.0) w.toInt().toString() else w.toString()
                "weight $formatted"
            }
            val subDetails = listOfNotNull(row.subject?.name, marksText, weightText).joinToString(" · ")
            if (subDetails.isNotBlank()) {
                Text(
                    subDetails,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Action chips for Attempted and Missed
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MarkChip(
                    label = "Missed",
                    selected = assessment.attemptStatus == AssessmentAttemptStatus.MISSED,
                    enabled = markable,
                    modifier = Modifier.weight(1f)
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    val next = if (assessment.attemptStatus == AssessmentAttemptStatus.MISSED)
                        AssessmentAttemptStatus.PENDING
                    else
                        AssessmentAttemptStatus.MISSED
                    onAttemptStatusChange(next)
                }
                MarkChip(
                    label = "Attempted",
                    selected = assessment.attemptStatus == AssessmentAttemptStatus.ATTEMPTED,
                    enabled = markable,
                    modifier = Modifier.weight(1f)
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    val next = if (assessment.attemptStatus == AssessmentAttemptStatus.ATTEMPTED)
                        AssessmentAttemptStatus.PENDING
                    else
                        AssessmentAttemptStatus.ATTEMPTED
                    onAttemptStatusChange(next)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimelineRow(
    row: DayOccurrenceRow,
    isPast: Boolean,
    onSubjectClick: () -> Unit,
    onMark: (OccurrenceStatus) -> Unit,
    onLongClick: () -> Unit,
    onSetCancelReason: (String?) -> Unit,
    onTestClick: ((Long) -> Unit)? = null,
    onTestAttemptStatusChange: (Assessment, AssessmentAttemptStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val occurrence = row.occurrence
    val subject = row.subject
    val subjectColor = subject?.let { Color(it.colorArgb) } ?: MaterialTheme.colorScheme.primary
    val tutorialController = LocalTutorialController.current
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = 16.dp).tutorialTarget("mark_chip")
    ) {
        Column(modifier = Modifier.width(64.dp * LocalDensity.current.fontScale)) {
            occurrence.startTime?.let {
                Text(formatTime(it, LocalTimeFormat.current), style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
            }
            occurrence.endTime?.let {
                Text(formatTime(it, LocalTimeFormat.current), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
            }
        }
        Column(
            modifier = Modifier.width(16.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(subjectColor))
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .padding(top = 4.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                    MaterialTheme.shapes.large
                )
                .pointerInput(occurrence.id) {
                    detectTapGestures(
                        onLongPress = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongClick()
                        }
                    )
                }
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(subjectColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(occurrence.classType.letter, color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            subject?.name ?: "?",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clickable(onClick = onSubjectClick),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
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
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                    subject?.teacherName?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    row.stats?.classesCanSkip?.let { canSkip ->
                        Text(
                            "Can miss up to $canSkip ${occurrence.classType.pluralLabel}(s).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    occurrence.roomNumber?.takeIf { it.isNotBlank() }?.let {
                        Text("Room $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                row.stats?.let { stats ->
                    ProgressRing(
                        percentage = stats.percentage,
                        aboveThreshold = stats.percentage >= row.requiredPercentage,
                        size = 44.dp,
                        strokeWidth = 4.dp,
                        textStyle = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Linked tests nested inside occurrence
            row.linkedTests.forEach { test ->
                val markable = !test.date.isAfter(LocalDate.now())
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (onTestClick != null) Modifier.clickable { onTestClick(test.id) } else Modifier),
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MarkChip(
                                label = "Missed",
                                selected = test.attemptStatus == AssessmentAttemptStatus.MISSED,
                                enabled = markable,
                                modifier = Modifier.weight(1f)
                            ) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                val next = if (test.attemptStatus == AssessmentAttemptStatus.MISSED)
                                    AssessmentAttemptStatus.PENDING
                                else
                                    AssessmentAttemptStatus.MISSED
                                onTestAttemptStatusChange(test, next)
                            }
                            MarkChip(
                                label = "Attempted",
                                selected = test.attemptStatus == AssessmentAttemptStatus.ATTEMPTED,
                                enabled = markable,
                                modifier = Modifier.weight(1f)
                            ) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                val next = if (test.attemptStatus == AssessmentAttemptStatus.ATTEMPTED)
                                    AssessmentAttemptStatus.PENDING
                                else
                                    AssessmentAttemptStatus.ATTEMPTED
                                onTestAttemptStatusChange(test, next)
                            }
                        }
                    }
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val markAndReport: (OccurrenceStatus) -> Unit = { status ->
                    onMark(status)
                    tutorialController?.reportSignal(TutorialSignal.OCCURRENCE_MARKED)
                }
                val chipModifier = Modifier.weight(1f).widthIn(min = 72.dp * LocalDensity.current.fontScale)
                MarkChip("Cancel", occurrence.status == OccurrenceStatus.CANCELLED, isPast, chipModifier) { markAndReport(OccurrenceStatus.CANCELLED) }
                MarkChip("Absent", occurrence.status == OccurrenceStatus.ABSENT, isPast, chipModifier) { markAndReport(OccurrenceStatus.ABSENT) }
                MarkChip("Present", occurrence.status == OccurrenceStatus.PRESENT, isPast, chipModifier) { markAndReport(OccurrenceStatus.PRESENT) }
            }

            if (occurrence.status == OccurrenceStatus.CANCELLED) {
                var reason by remember(occurrence.id) { mutableStateOf(occurrence.cancelReason ?: "") }
                LaunchedEffect(reason) {
                    delay(500)
                    if (reason != (occurrence.cancelReason ?: "")) onSetCancelReason(reason)
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (optional)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun MarkChip(label: String, selected: Boolean, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    FilterChip(
        selected = selected,
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        label = {
            Text(
                text = label,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                softWrap = false
            )
        },
        shape = MaterialTheme.shapes.small,
        enabled = enabled,
        modifier = modifier
    )
}
