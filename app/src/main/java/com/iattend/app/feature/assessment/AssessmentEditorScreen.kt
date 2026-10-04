package com.iattend.app.feature.assessment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.core.data.db.AssessmentAttemptStatus
import com.iattend.app.core.data.db.AssessmentType
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.core.data.db.WeightageSystem
import com.iattend.app.core.ui.DatePickerField
import com.iattend.app.core.ui.DirectionAwareTabs
import com.iattend.app.core.ui.LocalTimeFormat
import com.iattend.app.core.ui.MorphingButtonState
import com.iattend.app.core.ui.MorphingSaveButton
import com.iattend.app.core.ui.ReminderOffsetPicker
import com.iattend.app.core.ui.TabItem
import com.iattend.app.core.ui.SpringAlertDialog
import com.iattend.app.core.ui.SquircleIconButton
import com.iattend.app.core.ui.TimePickerField
import com.iattend.app.core.ui.formatDateShort
import com.iattend.app.core.ui.formatTime
import com.iattend.app.core.ui.hapticClick
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentEditorScreen(
    onBack: () -> Unit,
    viewModel: AssessmentEditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved, state.deleted) {
        if (state.saved) {
            delay(600)
            onBack()
        } else if (state.deleted) {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNew) "Add Assessment" else "Edit Assessment") },
                navigationIcon = {
                    SquircleIconButton(Icons.Default.ArrowBack, contentDescription = "Back", onClick = onBack)
                },
                actions = {
                    if (!viewModel.isNew) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            DirectionAwareTabs(
                tabs = listOf(
                    TabItem(AssessmentType.TEST, "Test"),
                    TabItem(AssessmentType.EXAM, "Exam")
                ),
                selectedTab = state.type,
                onTabSelected = viewModel::onTypeChange
            ) { type ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val explanation = if (type == AssessmentType.TEST) {
                        "Test: Schedule independently or link to a class. For a linked test, class attendance automatically records whether you attempted it; you can override this."
                    } else {
                        "Exam: Schedule independently of classes and track its attempt and result separately."
                    }
                    Text(
                        explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (type == AssessmentType.TEST) {
                        var expanded by remember { mutableStateOf(false) }
                        val selectedOcc = state.candidateClasses.find { it.id == state.linkedOccurrenceId }
                        val linkLabel = selectedOcc?.let {
                            "${formatDateShort(it.date)} · ${it.startTime?.let { t -> formatTime(t, LocalTimeFormat.current) } ?: ""}" +
                                (it.roomNumber?.takeIf { r -> r.isNotBlank() }?.let { r -> " (Room $r)" } ?: "")
                        } ?: "Independent (No class link)"

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = linkLabel,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                label = { Text("Linked class (optional)") }
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Independent (No class link)") },
                                    onClick = {
                                        viewModel.onLinkOccurrenceChange(null)
                                        expanded = false
                                    }
                                )
                                state.candidateClasses.forEach { occ ->
                                    val text = "${formatDateShort(occ.date)} · ${occ.startTime?.let { formatTime(it, LocalTimeFormat.current) } ?: ""}" +
                                        (occ.roomNumber?.takeIf { it.isNotBlank() }?.let { " (Room $it)" } ?: "") +
                                        (if (occ.status != OccurrenceStatus.UNMARKED) " [${occ.status.name}]" else "")
                                    DropdownMenuItem(
                                        text = { Text(text) },
                                        onClick = {
                                            viewModel.onLinkOccurrenceChange(occ.id)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text("Attempt status", style = MaterialTheme.typography.titleSmall)
            if (state.linkedOccurrenceId != null && !state.isAttemptOverridden) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Automatic: ${state.attemptStatus.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = { viewModel.onAttemptStatusChange(state.attemptStatus) }) {
                        Text("Override manually")
                    }
                }
            } else {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val statuses = listOf(
                        AssessmentAttemptStatus.PENDING to "Pending",
                        AssessmentAttemptStatus.ATTEMPTED to "Attempted",
                        AssessmentAttemptStatus.MISSED to "Missed"
                    )
                    statuses.forEachIndexed { index, (st, lbl) ->
                        SegmentedButton(
                            selected = state.attemptStatus == st,
                            onClick = { viewModel.onAttemptStatusChange(st) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = statuses.size)
                        ) {
                            Text(lbl)
                        }
                    }
                }
                if (state.linkedOccurrenceId != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = viewModel::resetToAutomaticStatus) {
                            Text("Reset to automatic")
                        }
                    }
                }
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Title (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            DatePickerField(
                label = if (state.linkedOccurrenceId != null) "Date (inherited from class)" else "Date",
                date = state.date,
                onDateChange = if (state.linkedOccurrenceId == null) viewModel::onDateChange else { _ -> },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimePickerField(
                    label = if (state.linkedOccurrenceId != null) "Start (inherited)" else "Start time",
                    time = state.startTime,
                    onTimeChange = if (state.linkedOccurrenceId == null) viewModel::onStartTimeChange else { _ -> },
                    modifier = Modifier.weight(1f)
                )
                TimePickerField(
                    label = if (state.linkedOccurrenceId != null) "End (inherited)" else "End time",
                    time = state.endTime,
                    onTimeChange = if (state.linkedOccurrenceId == null) viewModel::onEndTimeChange else { _ -> },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.totalMarks,
                    onValueChange = viewModel::onTotalMarksChange,
                    label = { Text("Total marks") },
                    modifier = Modifier.weight(1f)
                )
                if (state.attemptStatus == AssessmentAttemptStatus.ATTEMPTED) {
                    OutlinedTextField(
                        value = state.obtainedMarks,
                        onValueChange = viewModel::onObtainedMarksChange,
                        label = { Text("Obtained marks") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            val weightLabel = when (state.subjectGradingSystem) {
                WeightageSystem.PERCENTAGE -> "Weight (%)"
                WeightageSystem.WEIGHTED_MARKS -> "Weight (marks)"
                null -> "Weight (optional)"
            }
            OutlinedTextField(
                value = state.weight,
                onValueChange = viewModel::onWeightChange,
                label = { Text(weightLabel) },
                modifier = Modifier.fillMaxWidth()
            )

            state.weightWarning?.let { warning ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        warning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            state.validationError?.let { err ->
                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            OutlinedTextField(
                value = state.portions,
                onValueChange = viewModel::onPortionsChange,
                label = { Text("Portions / syllabus coverage (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Reminders")
            ReminderOffsetPicker(
                selectedMinutes = state.reminderOffsets,
                onChange = viewModel::onReminderOffsetsChange
            )

            MorphingSaveButton(
                onClick = {
                    isSaving = true
                    viewModel.save()
                },
                state = when {
                    state.saved -> MorphingButtonState.SUCCESS
                    isSaving -> MorphingButtonState.PENDING
                    else -> MorphingButtonState.IDLE
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (state.showScoreClearConfirm) {
        SpringAlertDialog(
            onDismissRequest = viewModel::dismissScoreClear,
            title = { Text("Clear entered score?") },
            text = { Text("Changing status away from Attempted will clear the entered score (${state.obtainedMarks} marks). Dismissing preserves the existing state.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmScoreClear) { Text("Clear & Change") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissScoreClear) { Text("Cancel") }
            }
        )
    }

    if (showDeleteConfirm) {
        SpringAlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this ${if (state.type == AssessmentType.EXAM) "exam" else "test"}?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; viewModel.delete() }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}
