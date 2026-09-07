package com.iattend.app.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.iattend.app.core.tutorial.LocalTutorialController
import com.iattend.app.core.tutorial.TutorialSignal
import com.iattend.app.core.tutorial.tutorialTarget
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.core.data.db.AssessmentType
import com.iattend.app.core.data.db.ClassOccurrence
import com.iattend.app.core.data.db.OccurrenceSource
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.core.ui.ProgressRing
import com.iattend.app.core.ui.SpringAlertDialog
import com.iattend.app.core.ui.formatDateShort
import com.iattend.app.core.ui.formatTime
import com.iattend.app.core.ui.modalsheet.ModalSheet
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale

private const val SWIPE_THRESHOLD_PX = 120f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onAddExtra: (LocalDate) -> Unit,
    onEditExtra: (Long) -> Unit = {},
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val visibleWeek by viewModel.visibleWeek.collectAsState()
    val rows by viewModel.rows.collectAsState()
    val isPast by viewModel.isPast.collectAsState()
    val assessmentRows by viewModel.assessmentsForSelectedDate.collectAsState()
    val assessmentDatesInWeek by viewModel.assessmentDatesInWeek.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedExtraForMenu by remember { mutableStateOf<DayOccurrenceRow?>(null) }
    var occurrenceToDelete by remember { mutableStateOf<ClassOccurrence?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 150.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                selectedDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                style = MaterialTheme.typography.titleLarge
            )
            Row {
                IconButton(onClick = { onAddExtra(selectedDate) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add extra class")
                }
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "Jump to date")
                }
            }
        }

        var dragAccum by remember { mutableFloatStateOf(0f) }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = viewModel::previousWeek) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Previous week")
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .pointerInputDragWeek(
                        onDragDelta = { dragAccum += it },
                        onDragEnd = {
                            if (dragAccum <= -SWIPE_THRESHOLD_PX) viewModel.nextWeek()
                            else if (dragAccum >= SWIPE_THRESHOLD_PX) viewModel.previousWeek()
                            dragAccum = 0f
                        }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                visibleWeek.forEach { date ->
                    WeekDayCell(
                        date = date,
                        selected = date == selectedDate,
                        hasAssessment = date in assessmentDatesInWeek,
                        onClick = { viewModel.selectDate(date) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            IconButton(onClick = viewModel::nextWeek) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Next week")
            }
        }

        if (rows.isEmpty() && assessmentRows.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No classes scheduled", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 20.dp)) {
                items(assessmentRows, key = { "a${it.assessment.id}" }) { row ->
                    AssessmentCard(row, modifier = Modifier.animateItem())
                }
                items(rows, key = { it.occurrence.id }) { row ->
                    TimelineRow(
                        row = row,
                        isPast = isPast,
                        onMark = { status -> viewModel.mark(row.occurrence, status) },
                        onSetCancelReason = { reason -> viewModel.setCancelReason(row.occurrence, reason) },
                        onLongClickExtra = { if (row.occurrence.source == OccurrenceSource.EXTRA) selectedExtraForMenu = row },
                        modifier = Modifier.animateItem()
                    )
                }
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

    selectedExtraForMenu?.let { extraRow ->
        ModalSheet(
            visible = true,
            onVisibleChange = { if (!it) selectedExtraForMenu = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = extraRow.subject?.name ?: "Extra Class",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${formatDateShort(extraRow.occurrence.date)}  ·  " +
                                "${extraRow.occurrence.startTime?.let(::formatTime) ?: "?"} - ${extraRow.occurrence.endTime?.let(::formatTime) ?: "?"}" +
                                (extraRow.occurrence.roomNumber?.let { "  · $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "Extra Class",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable {
                            val occId = extraRow.occurrence.id
                            selectedExtraForMenu = null
                            onEditExtra(occId)
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Edit Extra Class", style = MaterialTheme.typography.bodyLarge)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable {
                            val occ = extraRow.occurrence
                            selectedExtraForMenu = null
                            occurrenceToDelete = occ
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text(
                        "Delete Extra Class",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    occurrenceToDelete?.let { occ ->
        SpringAlertDialog(
            onDismissRequest = { occurrenceToDelete = null },
            title = { Text("Delete Extra Class?") },
            text = { Text("Are you sure you want to delete this scheduled extra class? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteOccurrence(occ)
                        occurrenceToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { occurrenceToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

private fun Modifier.pointerInputDragWeek(onDragDelta: (Float) -> Unit, onDragEnd: () -> Unit): Modifier =
    this.pointerInput(Unit) {
        detectHorizontalDragGestures(
            onDragEnd = onDragEnd,
            onHorizontalDrag = { change, dragAmount ->
                change.consume()
                onDragDelta(dragAmount)
            }
        )
    }

@Composable
private fun WeekDayCell(date: LocalDate, selected: Boolean, hasAssessment: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = when {
                    selected -> MaterialTheme.colorScheme.onPrimary
                    date == today -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(4.dp)
                .clip(CircleShape)
                .background(if (hasAssessment) MaterialTheme.colorScheme.error else Color.Transparent)
        )
    }
}

@Composable
private fun AssessmentCard(row: DayAssessmentRow, modifier: Modifier = Modifier) {
    val assessment = row.assessment
    val subjectColor = row.subject?.let { Color(it.colorArgb) } ?: MaterialTheme.colorScheme.error
    Row(modifier = modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Column(modifier = Modifier.width(58.dp)) {
            Text(formatTime(assessment.startTime), style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
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
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(12.dp)
        ) {
            Text(
                if (assessment.type == AssessmentType.EXAM) "Exam" else "Test",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                maxLines = 1,
                softWrap = false
            )
            Text(
                assessment.title?.takeIf { it.isNotBlank() } ?: (row.subject?.name ?: "?"),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            assessment.totalMarks?.let {
                Text("$it marks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
private fun TimelineRow(
    row: DayOccurrenceRow,
    isPast: Boolean,
    onMark: (OccurrenceStatus) -> Unit,
    onSetCancelReason: (String?) -> Unit,
    onLongClickExtra: () -> Unit = {},
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
        Column(modifier = Modifier.width(58.dp)) {
            occurrence.startTime?.let {
                Text(formatTime(it), style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
            }
            occurrence.endTime?.let {
                Text(formatTime(it), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
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
                .then(
                    if (occurrence.source == OccurrenceSource.EXTRA) {
                        Modifier.pointerInput(occurrence.id) {
                            detectTapGestures(
                                onLongPress = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onLongClickExtra()
                                }
                            )
                        }
                    } else Modifier
                )
                .padding(12.dp)
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
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f, fill = false),
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
                }
                row.stats?.let { stats ->
                    ProgressRing(
                        percentage = stats.percentage,
                        aboveThreshold = stats.percentage >= row.requiredPercentage,
                        size = 48.dp,
                        strokeWidth = 5.dp,
                        textStyle = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val markAndReport: (OccurrenceStatus) -> Unit = { status ->
                    onMark(status)
                    tutorialController?.reportSignal(TutorialSignal.OCCURRENCE_MARKED)
                }
                MarkChip("Cancel", occurrence.status == OccurrenceStatus.CANCELLED, isPast, Modifier.weight(1f)) { markAndReport(OccurrenceStatus.CANCELLED) }
                MarkChip("Absent", occurrence.status == OccurrenceStatus.ABSENT, isPast, Modifier.weight(1f)) { markAndReport(OccurrenceStatus.ABSENT) }
                MarkChip("Present", occurrence.status == OccurrenceStatus.PRESENT, isPast, Modifier.weight(1f)) { markAndReport(OccurrenceStatus.PRESENT) }
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
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
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
