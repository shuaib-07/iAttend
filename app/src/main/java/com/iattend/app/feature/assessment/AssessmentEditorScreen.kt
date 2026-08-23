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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.core.data.db.AssessmentType
import com.iattend.app.core.ui.DatePickerField
import com.iattend.app.core.ui.ReminderOffsetPicker
import com.iattend.app.core.ui.SquircleIconButton
import com.iattend.app.core.ui.SpringAlertDialog
import com.iattend.app.core.ui.TimePickerField
import com.iattend.app.core.ui.hapticClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentEditorScreen(
    onBack: () -> Unit,
    viewModel: AssessmentEditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved, state.deleted) {
        if (state.saved || state.deleted) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNew) "Add Test/Exam" else "Edit Test/Exam") },
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.type == AssessmentType.TEST,
                    onClick = { viewModel.onTypeChange(AssessmentType.TEST) },
                    label = { Text("Test") }
                )
                FilterChip(
                    selected = state.type == AssessmentType.EXAM,
                    onClick = { viewModel.onTypeChange(AssessmentType.EXAM) },
                    label = { Text("Exam") }
                )
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Title (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            DatePickerField(
                label = "Date",
                date = state.date,
                onDateChange = viewModel::onDateChange,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimePickerField(
                    label = "Start time",
                    time = state.startTime,
                    onTimeChange = viewModel::onStartTimeChange,
                    modifier = Modifier.weight(1f)
                )
                TimePickerField(
                    label = "End time",
                    time = state.endTime,
                    onTimeChange = viewModel::onEndTimeChange,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = state.totalMarks,
                onValueChange = viewModel::onTotalMarksChange,
                label = { Text("Total marks (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
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

            Button(onClick = hapticClick(viewModel::save), modifier = Modifier.fillMaxWidth()) {
                Text("Save")
            }
        }
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
