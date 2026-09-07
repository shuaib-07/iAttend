package com.iattend.app.feature.subject

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.core.data.db.TotalMode
import com.iattend.app.core.notifications.ReminderOffset
import com.iattend.app.core.tutorial.LocalTutorialController
import com.iattend.app.core.tutorial.TutorialSignal
import com.iattend.app.core.tutorial.tutorialTarget
import com.iattend.app.core.ui.DatePickerField
import com.iattend.app.core.ui.ReminderOffsetPicker
import com.iattend.app.core.ui.SquircleIconButton
import com.iattend.app.core.ui.SpringAlertDialog
import com.iattend.app.core.ui.SubjectColorPalette
import com.iattend.app.core.ui.hapticClick
import com.iattend.app.core.ui.rememberUnsavedChangesGuard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectEditorScreen(
    onBack: () -> Unit,
    viewModel: SubjectEditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val isDirty by viewModel.isDirty.collectAsState()
    val guardedBack = rememberUnsavedChangesGuard(isDirty = isDirty, onConfirmedBack = onBack)
    val tutorialController = LocalTutorialController.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            tutorialController?.let {
                it.setTutorialSubjectId(state.subjectId)
                it.reportSignal(TutorialSignal.SUBJECT_SAVED)
            }
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNew) "Add Subject" else "Edit Subject") },
                navigationIcon = {
                    SquircleIconButton(Icons.Default.ArrowBack, contentDescription = "Back", onClick = guardedBack)
                },
                actions = {
                    if (!viewModel.isNew) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete subject",
                                tint = MaterialTheme.colorScheme.error
                            )
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
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Subject name") },
                modifier = Modifier.fillMaxWidth().tutorialTarget("subject_name_field")
            )
            OutlinedTextField(
                value = state.code,
                onValueChange = viewModel::onCodeChange,
                label = { Text("Code (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.teacherName,
                onValueChange = viewModel::onTeacherNameChange,
                label = { Text("Teacher's name (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Color")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SubjectColorPalette.forEach { color ->
                    ColorSwatch(
                        color = color,
                        selected = color.toArgb() == state.colorArgb,
                        onClick = { viewModel.onColorChange(color.toArgb()) }
                    )
                }
            }

            OutlinedTextField(
                value = state.requiredPercentageOverride,
                onValueChange = viewModel::onRequiredOverrideChange,
                label = { Text("Required % override (optional)") },
                modifier = Modifier.fillMaxWidth().tutorialTarget("required_pct_override_field")
            )

            Text("Total classes")
            Column(modifier = Modifier.tutorialTarget("total_mode_radios")) {
            TotalMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.onTotalModeChange(mode) },
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    RadioButton(selected = state.totalMode == mode, onClick = { viewModel.onTotalModeChange(mode) })
                    Text(
                        when (mode) {
                            TotalMode.KNOWN -> "Known total (enter below)"
                            TotalMode.COMPUTED -> "Computed from tracking end date"
                            TotalMode.OPEN_ENDED -> "Open-ended (no fixed total)"
                        }
                    )
                }
            }
            }
            if (state.totalMode == TotalMode.KNOWN) {
                OutlinedTextField(
                    value = state.knownTotalClasses,
                    onValueChange = viewModel::onKnownTotalChange,
                    label = { Text("Total classes this term") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (state.totalMode == TotalMode.COMPUTED) {
                DatePickerField(
                    label = "End date (optional, uses Settings default if blank)",
                    date = state.trackingEndDateOverride,
                    onDateChange = viewModel::onTrackingEndDateOverrideChange,
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.trackingEndDateOverride != null) {
                    TextButton(onClick = { viewModel.onTrackingEndDateOverrideChange(null) }) {
                        Text("Use Settings default instead")
                    }
                }
            }

            Text("Class reminders")
            val reminderOverride = state.reminderOffsetsOverride
            if (reminderOverride == null) {
                TextButton(onClick = { viewModel.onReminderOffsetsOverrideChange("") }) {
                    Text("Using Settings default — customize for this subject")
                }
            } else {
                ReminderOffsetPicker(
                    selectedMinutes = ReminderOffset.parse(reminderOverride),
                    onChange = { viewModel.onReminderOffsetsOverrideChange(ReminderOffset.format(it)) }
                )
                TextButton(onClick = { viewModel.onReminderOffsetsOverrideChange(null) }) {
                    Text("Use Settings default instead")
                }
            }

            Button(
                onClick = hapticClick(viewModel::save),
                modifier = Modifier.fillMaxWidth().tutorialTarget("subject_save_button")
            ) {
                Text("Save")
            }

            if (!viewModel.isNew) {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete Subject")
                }
            }
        }
    }

    if (showDeleteDialog) {
        SpringAlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete subject?") },
            text = {
                Text("This will permanently delete \"${state.name.ifBlank { "this subject" }}\" along with all scheduled classes, attendance history, and assessments associated with it. Are you sure?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(if (selected) 36.dp else 28.dp)
            .background(color, CircleShape)
            .clickable(onClick = onClick)
    )
}
