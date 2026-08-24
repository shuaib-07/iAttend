package com.iattend.app.feature.timetable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.core.data.db.ClassType
import com.iattend.app.core.tutorial.LocalTutorialController
import com.iattend.app.core.tutorial.TutorialSignal
import com.iattend.app.core.tutorial.tutorialTarget
import com.iattend.app.core.ui.DatePickerField
import com.iattend.app.core.ui.formatDateShort
import com.iattend.app.core.ui.formatTime
import com.iattend.app.core.ui.SquircleIconButton
import com.iattend.app.core.ui.SubjectChipRow
import com.iattend.app.core.ui.TimePickerField
import com.iattend.app.core.ui.hapticClick
import com.iattend.app.core.ui.rememberUnsavedChangesGuard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtraClassesScreen(
    onBack: () -> Unit,
    viewModel: ExtraClassesViewModel = hiltViewModel()
) {
    val form by viewModel.form.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val upcoming by viewModel.upcomingExtras.collectAsState()
    val isDirty by viewModel.isDirty.collectAsState()
    val guardedBack = rememberUnsavedChangesGuard(isDirty = isDirty, onConfirmedBack = onBack)
    val tutorialController = LocalTutorialController.current

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
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            item {
                Column(modifier = Modifier.tutorialTarget("extra_class_form")) {
                    DatePickerField("Date", form.date, viewModel::onDateChange, Modifier.fillMaxWidth())
                    SubjectChipRow(subjects, form.subjectId, viewModel::onSubjectChange, Modifier.fillMaxWidth().padding(top = 12.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimePickerField("Start", form.startTime, viewModel::onStartTimeChange, Modifier.weight(1f))
                        TimePickerField("End", form.endTime, viewModel::onEndTimeChange, Modifier.weight(1f))
                    }
                    OutlinedTextField(
                        value = form.room,
                        onValueChange = viewModel::onRoomChange,
                        label = { Text("Room No (Optional)") },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        ClassType.entries.forEachIndexed { index, type ->
                            SegmentedButton(
                                selected = form.classType == type,
                                onClick = { viewModel.onClassTypeChange(type) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = ClassType.entries.size)
                            ) { Text(type.label) }
                        }
                    }
                    Button(
                        onClick = hapticClick {
                            viewModel.schedule()
                            tutorialController?.reportSignal(TutorialSignal.EXTRA_CLASS_SAVED)
                        },
                        enabled = form.subjectId != null,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    ) { Text(if (form.editingOccurrenceId != null) "Update Extra Class" else "Schedule Extra Class") }
                    if (form.editingOccurrenceId != null) {
                        TextButton(
                            onClick = viewModel::cancelEdit,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) { Text("Cancel edit") }
                    }

                    Text(
                        "SCHEDULED EXTRA CLASSES",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                    )
                }
            }
            if (upcoming.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("None scheduled", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(upcoming, key = { it.occurrence.id }) { extra ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).animateItem(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(extra.subjectName, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${formatDateShort(extra.occurrence.date)}  " +
                                        "${extra.occurrence.startTime?.let(::formatTime) ?: "?"}-${extra.occurrence.endTime?.let(::formatTime) ?: "?"}" +
                                        (extra.occurrence.roomNumber?.let { "  · $it" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.startEdit(extra.occurrence) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit extra class")
                            }
                            IconButton(onClick = { viewModel.delete(extra.occurrence) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete extra class", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
