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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.core.data.db.TotalMode
import com.iattend.app.core.data.db.WeightageSystem
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import com.iattend.app.core.notifications.ReminderOffset
import com.iattend.app.core.tutorial.LocalTutorialController
import com.iattend.app.core.tutorial.TutorialSignal
import com.iattend.app.core.tutorial.tutorialTarget
import com.iattend.app.core.ui.DatePickerField
import com.iattend.app.core.ui.MorphingButtonState
import com.iattend.app.core.ui.MorphingSaveButton
import com.iattend.app.core.ui.ReminderOffsetPicker
import com.iattend.app.core.ui.SquircleIconButton
import com.iattend.app.core.ui.SpringAlertDialog
import com.iattend.app.core.ui.SubjectColorPalette
import com.iattend.app.core.ui.hapticClick
import com.iattend.app.core.ui.rememberUnsavedChangesGuard
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.filled.Check
import com.iattend.app.core.domain.stats.isValidAttendancePercentage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var isSaving by remember { mutableStateOf(false) }
    val overrideValid = state.requiredPercentageOverride.isBlank() || isValidAttendancePercentage(state.requiredPercentageOverride.toFloatOrNull())
    val totalValid = state.totalMode != TotalMode.KNOWN || (state.knownTotalClasses.toIntOrNull()?.let { it > 0 } == true)

    LaunchedEffect(state.saved) {
        if (state.saved) {
            tutorialController?.let {
                it.setTutorialSubjectId(state.subjectId)
                it.reportSignal(TutorialSignal.SUBJECT_SAVED)
            }
            delay(600)
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNew) "Add Subject" else "Edit Subject") },
                navigationIcon = {
                    SquircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = guardedBack)
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
            modifier = Modifier.fillMaxSize().padding(padding).imePadding().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Subject name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().tutorialTarget("subject_name_field")
            )
            OutlinedTextField(
                value = state.code,
                onValueChange = viewModel::onCodeChange,
                label = { Text("Code (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.teacherName,
                onValueChange = viewModel::onTeacherNameChange,
                label = { Text("Teacher's name (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Color", style = MaterialTheme.typography.titleSmall)
            FlowRow(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SubjectColorPalette.forEachIndexed { index, color ->
                    ColorSwatch(
                        color = color,
                        selected = color.toArgb() == state.colorArgb,
                        onClick = { viewModel.onColorChange(color.toArgb()) },
                        label = "Subject color ${index + 1}"
                    )
                }
            }

            OutlinedTextField(
                value = state.requiredPercentageOverride,
                onValueChange = viewModel::onRequiredOverrideChange,
                label = { Text("Required % override (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                suffix = { Text("%") },
                isError = !overrideValid,
                supportingText = { Text(if (overrideValid) "Leave blank to use the Settings default." else "Enter a percentage from 0 to 100.") },
                modifier = Modifier.fillMaxWidth().tutorialTarget("required_pct_override_field")
            )

            Text("Total classes", style = MaterialTheme.typography.titleSmall)
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
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !totalValid,
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

            Text("Assessment weightage system", style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val systems = listOf(null to "None", WeightageSystem.PERCENTAGE to "Percentage", WeightageSystem.WEIGHTED_MARKS to "Marks")
                systems.forEachIndexed { index, (sys, label) ->
                    SegmentedButton(
                        selected = state.gradingSystem == sys,
                        onClick = { viewModel.onGradingSystemChange(sys) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = systems.size)
                    ) {
                        Text(label)
                    }
                }
            }
            if (state.gradingSystem == WeightageSystem.WEIGHTED_MARKS) {
                OutlinedTextField(
                    value = state.totalWeightedMarks,
                    onValueChange = viewModel::onTotalWeightedMarksChange,
                    label = { Text("Total possible weighted marks (e.g. 100)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (state.gradingSystem != null) {
                Text(
                    "Assessments in this subject will track weighted contributions. Changing system requires re-entering weights.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text("Class reminders", style = MaterialTheme.typography.titleSmall)
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

            MorphingSaveButton(
                enabled = state.name.isNotBlank() && overrideValid && totalValid && !isSaving,
                onClick = {
                    isSaving = true
                    viewModel.save()
                },
                state = when {
                    state.saved -> MorphingButtonState.SUCCESS
                    isSaving -> MorphingButtonState.PENDING
                    else -> MorphingButtonState.IDLE
                },
                modifier = Modifier.fillMaxWidth().tutorialTarget("subject_save_button")
            )

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
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit, label: String) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(48.dp)
            .background(color, CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
    }
}
