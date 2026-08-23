package com.iattend.app.feature.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.core.ui.SpringAlertDialog
import com.iattend.app.core.ui.SquircleIconButton
import java.time.LocalDate

/** Bottom-sheet content (see ModalSheet) - no Scaffold of its own, hosted by SettingsScreen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportImportSheetContent(
    onDismiss: () -> Unit,
    viewModel: ExportImportViewModel = hiltViewModel()
) {
    val message by viewModel.message.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val context = LocalContext.current
    var showImportConfirm by remember { mutableStateOf(false) }

    // template options state
    var selectedIds by remember(subjects) {
        mutableStateOf(subjects.map { it.id }.toSet())
    }
    var includeExtra by remember { mutableStateOf(true) }
    var includeAssessments by remember { mutableStateOf(true) }

    val exportFullLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(viewModel::exportTo)
    }
    val exportTemplateLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.exportTemplateTo(it, selectedIds, includeExtra, includeAssessments) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importFrom)
    }

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessage()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Export / Import", style = MaterialTheme.typography.titleLarge)
            SquircleIconButton(Icons.Default.Close, contentDescription = "Close", onClick = onDismiss)
        }

        // Full backup section
        Text("Full Backup", style = MaterialTheme.typography.titleMedium)
        Text(
            "Back up your entire subjects, timetable, and attendance history to a single JSON file, or restore from one.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = { exportFullLauncher.launch("iattend-backup-${LocalDate.now()}.json") },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Export full backup") }

        HorizontalDivider()

        // Template section
        Text("Timetable Template (Shareable)", style = MaterialTheme.typography.titleMedium)
        Text(
            "Share subjects, timetable, holidays and tracking dates with a classmate - optionally exams/tests too. No attendance history is included. Your classmate can import it and start marking their own attendance.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (subjects.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Subjects (${selectedIds.size}/${subjects.size})",
                    style = MaterialTheme.typography.labelLarge
                )
                TextButton(
                    onClick = {
                        selectedIds = if (selectedIds.size == subjects.size) emptySet() else subjects.map { it.id }.toSet()
                    }
                ) { Text(if (selectedIds.size == subjects.size) "Deselect all" else "Select all") }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subjects.forEach { subject ->
                    val selected = subject.id in selectedIds
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedIds = if (selected) selectedIds - subject.id else selectedIds + subject.id
                        },
                        label = { Text(subject.name) }
                    )
                }
            }
        } else {
            Text(
                "No subjects yet — add subjects first to share a template.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = includeExtra, onCheckedChange = { includeExtra = it })
            Text("Include extra classes", style = MaterialTheme.typography.bodyMedium)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = includeAssessments, onCheckedChange = { includeAssessments = it })
            Text("Include exams & tests", style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = { exportTemplateLauncher.launch("iattend-template-${LocalDate.now()}.json") },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedIds.isNotEmpty()
        ) { Text("Export template") }

        OutlinedButton(
            onClick = { viewModel.shareTemplate(selectedIds, includeExtra, includeAssessments) },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedIds.isNotEmpty()
        ) { Text("Share template") }

        HorizontalDivider()

        // Single import section (auto-detects full vs template)
        Text("Import", style = MaterialTheme.typography.titleMedium)
        Text(
            "Import a full backup or a timetable template. The app will detect the file type automatically.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = { showImportConfirm = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Import backup / template")
        }
    }

    if (showImportConfirm) {
        SpringAlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text("Replace data?") },
            text = {
                Text(
                    "Importing replaces data on this device. " +
                        "A full backup replaces everything — subjects, timetables, and attendance history. " +
                        "A timetable template replaces your subjects, timetable, holidays and extra classes, clears existing attendance and regenerates it as unmarked. " +
                        "This can't be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    importLauncher.launch(arrayOf("application/json"))
                }) { Text("Choose file") }
            },
            dismissButton = { TextButton(onClick = { showImportConfirm = false }) { Text("Cancel") } }
        )
    }
}
