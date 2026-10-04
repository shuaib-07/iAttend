package com.iattend.app.feature.timetable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.core.data.db.ClassOccurrence
import com.iattend.app.core.data.db.ClassType
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.core.tutorial.LocalTutorialController
import com.iattend.app.core.tutorial.TutorialSignal
import com.iattend.app.core.tutorial.tutorialTarget
import com.iattend.app.core.ui.DatePickerField
import com.iattend.app.core.ui.LocalTimeFormat
import com.iattend.app.core.ui.MorphingButtonState
import com.iattend.app.core.ui.MorphingSaveButton
import com.iattend.app.core.ui.ProgressRing
import com.iattend.app.core.ui.SpringAlertDialog
import com.iattend.app.core.ui.SquircleIconButton
import com.iattend.app.core.ui.SubjectChipRow
import com.iattend.app.core.ui.TimePickerField
import com.iattend.app.core.ui.formatDateShort
import com.iattend.app.core.ui.formatTime
import com.iattend.app.core.ui.hapticClick
import kotlinx.coroutines.delay
import com.iattend.app.core.ui.rememberUnsavedChangesGuard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtraClassesScreen(
    onBack: () -> Unit,
    viewModel: ExtraClassesViewModel = hiltViewModel()
) {
    val form by viewModel.form.collectAsState()
    val isFormOpen by viewModel.isFormOpen.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val upcoming by viewModel.upcomingExtras.collectAsState()
    val past by viewModel.pastExtras.collectAsState()
    val isDirty by viewModel.isDirty.collectAsState()
    val guardedBack = rememberUnsavedChangesGuard(isDirty = isDirty, onConfirmedBack = onBack)
    val tutorialController = LocalTutorialController.current

    var occurrenceToDelete by remember { mutableStateOf<ClassOccurrence?>(null) }
    var showDeleteEditingDialog by remember { mutableStateOf(false) }
    var confirmClearDefault by remember { mutableStateOf(false) }
    val selectedSubject = subjects.firstOrNull { it.id == form.subjectId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extra Classes") },
                navigationIcon = {
                    SquircleIconButton(Icons.Default.Close, contentDescription = "Close", onClick = guardedBack)
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top CTA button: Schedule extra class
            item {
                if (!isFormOpen) {
                    Button(
                        onClick = { viewModel.openScheduleForm() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Schedule Extra Class")
                    }
                }
            }

            // Scheduling Form (Expands when open or editing)
            item {
                AnimatedVisibility(
                    visible = isFormOpen,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .tutorialTarget("extra_class_form"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (form.editingOccurrenceId != null) "Edit Extra Class" else "Schedule Extra Class",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                IconButton(onClick = { viewModel.closeScheduleForm() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close form")
                                }
                            }

                            DatePickerField("Date", form.date, viewModel::onDateChange, Modifier.fillMaxWidth())

                            SubjectChipRow(subjects, form.subjectId, viewModel::onSubjectChange, Modifier.fillMaxWidth())

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TimePickerField("Start", form.startTime, viewModel::onStartTimeChange, Modifier.weight(1f))
                                TimePickerField("End", form.endTime, viewModel::onEndTimeChange, Modifier.weight(1f))
                            }

                            OutlinedTextField(
                                value = form.room,
                                onValueChange = viewModel::onRoomChange,
                                label = { Text("Room No (Optional)") },
                                supportingText = { Text(if (form.roomOverridden) "Overridden for this class" else "Subject default: ${selectedSubject?.defaultRoomNumber ?: "Not set"}") },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = if (form.roomOverridden) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (form.roomOverridden) {
                                TextButton(onClick = viewModel::resetRoomToDefault) {
                                    Text("Reset to subject default")
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = form.setSubjectDefaultRoom,
                                    enabled = form.room.isNotBlank() || selectedSubject?.defaultRoomNumber != null,
                                    onCheckedChange = { checked ->
                                        if (checked && form.room.isBlank() && selectedSubject?.defaultRoomNumber != null) confirmClearDefault = true
                                        else viewModel.setSubjectDefaultRoom(checked)
                                    }
                                )
                                Text(if (form.room.isBlank()) "Remove subject default room" else "Set as default for this subject")
                            }

                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                ClassType.entries.forEachIndexed { index, type ->
                                    SegmentedButton(
                                        selected = form.classType == type,
                                        onClick = { viewModel.onClassTypeChange(type) },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ClassType.entries.size)
                                    ) { Text(type.label) }
                                }
                            }

                            MorphingSaveButton(
                                onAction = {
                                    viewModel.schedule()
                                    tutorialController?.reportSignal(TutorialSignal.EXTRA_CLASS_SAVED)
                                    delay(400)
                                },
                                label = if (form.editingOccurrenceId != null) "Save Changes" else "Save Extra Class",
                                pendingLabel = if (form.editingOccurrenceId != null) "Saving Changes..." else "Saving Extra Class...",
                                successLabel = if (form.editingOccurrenceId != null) "Changes Saved" else "Extra Class Saved",
                                enabled = form.subjectId != null,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            )

                            if (form.editingOccurrenceId != null) {
                                OutlinedButton(
                                    onClick = { showDeleteEditingDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Delete extra class")
                                }
                            }

                            OutlinedButton(
                                onClick = { viewModel.closeScheduleForm() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }

            // Upcoming Extra Classes Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Upcoming Extra Classes",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "${upcoming.size} class${if (upcoming.size != 1) "es" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (upcoming.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No upcoming extra classes scheduled.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(upcoming, key = { it.occurrence.id }) { item ->
                    ExtraClassCard(
                        item = item,
                        onEdit = { viewModel.startEdit(item.occurrence) },
                        onDelete = { occurrenceToDelete = item.occurrence },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            // Past Extra Classes Section
            if (past.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Past Extra Classes",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "${past.size} class${if (past.size != 1) "es" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(past, key = { it.occurrence.id }) { item ->
                    ExtraClassCard(
                        item = item,
                        onEdit = { viewModel.startEdit(item.occurrence) },
                        onDelete = { occurrenceToDelete = item.occurrence },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

    if (showDeleteEditingDialog) {
        SpringAlertDialog(
            onDismissRequest = { showDeleteEditingDialog = false },
            title = { Text("Delete extra class?") },
            text = { Text("This will permanently remove this extra class from your attendance records.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteEditingDialog = false
                        viewModel.deleteEditing()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteEditingDialog = false }) { Text("Cancel") }
            }
        )
    }

    occurrenceToDelete?.let { occ ->
        SpringAlertDialog(
            onDismissRequest = { occurrenceToDelete = null },
            title = { Text("Delete extra class?") },
            text = { Text("This will permanently remove this extra class from your attendance records.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(occ)
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

    if (confirmClearDefault) {
        SpringAlertDialog(
            onDismissRequest = { confirmClearDefault = false },
            title = { Text("Remove default room?") },
            text = { Text("Leaving Room No blank with this checked will remove ${selectedSubject?.name.orEmpty()}'s default room number. Classes with overridden rooms will keep their rooms.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClearDefault = false
                    viewModel.setSubjectDefaultRoom(true)
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearDefault = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ExtraClassCard(
    item: ExtraClassItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val occ = item.occurrence
    val timeFormatter = LocalTimeFormat.current
    val timeStr = occ.startTime?.let { formatTime(it, timeFormatter) } ?: ""
    val endTimeStr = occ.endTime?.let { formatTime(it, timeFormatter) } ?: ""

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = item.subjectColorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                )
                Column {
                    Text(
                        item.subjectName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        buildString {
                            append(formatDateShort(occ.date))
                            if (timeStr.isNotBlank()) {
                                append(" · ")
                                append(timeStr)
                                if (endTimeStr.isNotBlank()) {
                                    append(" - ")
                                    append(endTimeStr)
                                }
                            }
                            occ.roomNumber?.takeIf { it.isNotBlank() }?.let {
                                append(" · Room ")
                                append(it)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (occ.status != OccurrenceStatus.UNMARKED) {
                        Text(
                            occ.status.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = when (occ.status) {
                                OccurrenceStatus.PRESENT -> MaterialTheme.colorScheme.primary
                                OccurrenceStatus.ABSENT -> MaterialTheme.colorScheme.error
                                OccurrenceStatus.CANCELLED -> MaterialTheme.colorScheme.outline
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit extra class")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete extra class",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
