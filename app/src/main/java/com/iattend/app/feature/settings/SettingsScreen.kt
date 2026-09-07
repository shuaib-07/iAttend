package com.iattend.app.feature.settings

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.MainActivity
import com.iattend.app.core.datastore.BackupFrequency
import com.iattend.app.core.notifications.ReminderOffset
import com.iattend.app.core.ui.DatePickerField
import com.iattend.app.core.ui.DateRangePickerField
import com.iattend.app.core.ui.ListItem
import com.iattend.app.core.ui.ListItemPosition
import com.iattend.app.core.ui.ReminderOffsetPicker
import com.iattend.app.core.ui.SpringAlertDialog
import com.iattend.app.core.ui.SquircleIconButton
import com.iattend.app.core.ui.formatDateShort
import com.iattend.app.core.ui.modalsheet.ModalSheet
import com.iattend.app.core.ui.toShape
import com.iattend.app.core.tutorial.tutorialTarget
import com.iattend.app.feature.devsupport.DevSupportTrigger
import com.iattend.app.feature.devsupport.DeveloperSupportSheetContent
import com.iattend.app.feature.profile.ProfileSheetContent
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateToAppearance: () -> Unit = {}, onNavigateToAbout: () -> Unit = {}, viewModel: SettingsViewModel = hiltViewModel()) {
    val requiredPercentage by viewModel.requiredPercentage.collectAsState()
    val trackingEndDate by viewModel.trackingEndDate.collectAsState()
    val trackingStartDate by viewModel.trackingStartDate.collectAsState()

    var showRequiredPercentSheet by remember { mutableStateOf(false) }
    var showTrackingEndDateSheet by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var showDeveloperSupportSheet by remember { mutableStateOf(false) }
    var showExportImportSheet by remember { mutableStateOf(false) }
    var showReminderDefaultsSheet by remember { mutableStateOf(false) }
    var showAutoBackupSheet by remember { mutableStateOf(false) }
    var showResetSheet by remember { mutableStateOf(false) }

    val devSupportRequested by DevSupportTrigger.requestOpen
    LaunchedEffect(devSupportRequested) {
        if (devSupportRequested) {
            showDeveloperSupportSheet = true
            DevSupportTrigger.requestOpen.value = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        SettingsSection(title = "Attendance") {
            SettingsIconRow(
                title = "Required attendance percentage",
                subtitle = "${requiredPercentage}%",
                icon = Icons.Default.Percent,
                colorIndex = 0,
                position = ListItemPosition.Top,
                onClick = { showRequiredPercentSheet = true },
                modifier = Modifier.tutorialTarget("required_pct_row")
            )
            SettingsIconRow(
                title = "Tracking period",
                subtitle = run {
                    val start = trackingStartDate
                    val end = trackingEndDate
                    when {
                        start != null && end != null -> "${formatDateShort(start)} - ${formatDateShort(end)}"
                        start != null -> "From ${formatDateShort(start)}"
                        end != null -> "Until ${formatDateShort(end)}"
                        else -> "Not set"
                    }
                },
                icon = Icons.Default.CalendarMonth,
                colorIndex = 1,
                position = ListItemPosition.Bottom,
                onClick = { showTrackingEndDateSheet = true },
                modifier = Modifier.tutorialTarget("tracking_period_row")
            )
        }

        SettingsSection(title = "General") {
            SettingsIconRow(
                title = "Appearance",
                subtitle = "Theme, style & colors",
                icon = Icons.Default.Palette,
                colorIndex = 2,
                position = ListItemPosition.Top,
                onClick = onNavigateToAppearance
            )
            SettingsIconRow(
                title = "Profile",
                subtitle = "Name & avatar",
                icon = Icons.Default.Person,
                colorIndex = 0,
                position = ListItemPosition.Middle,
                onClick = { showProfileSheet = true }
            )
            SettingsIconRow(
                title = "Export / Import data",
                subtitle = "Backup & restore",
                icon = Icons.Default.ImportExport,
                colorIndex = 1,
                position = ListItemPosition.Middle,
                onClick = { showExportImportSheet = true }
            )
            SettingsIconRow(
                title = "Reminder defaults",
                subtitle = "Default timing for class & exam reminders",
                icon = Icons.Default.Notifications,
                colorIndex = 2,
                position = ListItemPosition.Middle,
                onClick = { showReminderDefaultsSheet = true }
            )
            SettingsIconRow(
                title = "Automated backups",
                subtitle = "Scheduled daily/weekly backups",
                icon = Icons.Default.CloudUpload,
                colorIndex = 0,
                position = ListItemPosition.Bottom,
                onClick = { showAutoBackupSheet = true }
            )
        }

        SettingsSection(title = "Support") {
            SettingsIconRow(
                title = "Buy the developer a chai",
                subtitle = null,
                icon = Icons.Default.LocalCafe,
                colorIndex = 2,
                position = ListItemPosition.Top,
                onClick = { showDeveloperSupportSheet = true }
            )
            SettingsIconRow(
                title = "About",
                subtitle = "Version, source code & licenses",
                icon = Icons.Default.Info,
                colorIndex = 0,
                position = ListItemPosition.Bottom,
                onClick = onNavigateToAbout
            )
        }

        SettingsSection(title = "Danger zone") {
            SettingsIconRow(
                title = "Delete / Reset data",
                subtitle = "Wipe academic data or start over completely",
                icon = Icons.Default.Delete,
                colorIndex = 2,
                position = ListItemPosition.Single,
                onClick = { showResetSheet = true }
            )
        }
    }

    ModalSheet(visible = showRequiredPercentSheet, onVisibleChange = { showRequiredPercentSheet = it }) {
        RequiredPercentageSheetContent(
            requiredPercentage = requiredPercentage,
            onChange = viewModel::setRequiredPercentage,
            onDismiss = { showRequiredPercentSheet = false }
        )
    }
    ModalSheet(visible = showTrackingEndDateSheet, onVisibleChange = { showTrackingEndDateSheet = it }) {
        TrackingDateRangeSheetContent(
            trackingStartDate = trackingStartDate,
            trackingEndDate = trackingEndDate,
            onChange = { start, end ->
                viewModel.setTrackingStartDate(start)
                viewModel.setTrackingEndDate(end)
            },
            onDismiss = { showTrackingEndDateSheet = false }
        )
    }
    ModalSheet(visible = showProfileSheet, onVisibleChange = { showProfileSheet = it }) {
        ProfileSheetContent(onDismiss = { showProfileSheet = false })
    }
    ModalSheet(visible = showDeveloperSupportSheet, onVisibleChange = { showDeveloperSupportSheet = it }) {
        DeveloperSupportSheetContent(onDismiss = { showDeveloperSupportSheet = false })
    }
    ModalSheet(visible = showExportImportSheet, onVisibleChange = { showExportImportSheet = it }) {
        ExportImportSheetContent(onDismiss = { showExportImportSheet = false })
    }
    ModalSheet(visible = showReminderDefaultsSheet, onVisibleChange = { showReminderDefaultsSheet = it }) {
        ReminderDefaultsSheetContent(viewModel = viewModel, onDismiss = { showReminderDefaultsSheet = false })
    }
    ModalSheet(visible = showAutoBackupSheet, onVisibleChange = { showAutoBackupSheet = it }) {
        AutoBackupSheetContent(viewModel = viewModel, onDismiss = { showAutoBackupSheet = false })
    }
    ModalSheet(visible = showResetSheet, onVisibleChange = { showResetSheet = it }) {
        ResetDataSheetContent(settingsViewModel = viewModel, onDismiss = { showResetSheet = false })
    }
}

private enum class ResetScope { ACADEMIC, FULL }

@Composable
private fun ResetDataSheetContent(settingsViewModel: SettingsViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val exportImportViewModel: ExportImportViewModel = hiltViewModel()
    val backupMessage by exportImportViewModel.message.collectAsState()
    var scope by remember { mutableStateOf(ResetScope.ACADEMIC) }
    var showConfirm by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(exportImportViewModel::exportTo)
    }

    LaunchedEffect(backupMessage) {
        backupMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            exportImportViewModel.clearMessage()
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Delete / Reset data", style = MaterialTheme.typography.titleLarge)
            SquircleIconButton(Icons.Default.Close, contentDescription = "Close", onClick = onDismiss)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ResetOptionCard(
                title = "Reset academic data",
                description = "Deletes subjects, timetable, attendance, holidays and exams. Keeps your profile, theme and preferences.",
                selected = scope == ResetScope.ACADEMIC,
                onClick = { scope = ResetScope.ACADEMIC }
            )
            ResetOptionCard(
                title = "Full reset (start over)",
                description = "Deletes everything above plus your profile and all preferences, and takes you back through onboarding - like a fresh install.",
                selected = scope == ResetScope.FULL,
                onClick = { scope = ResetScope.FULL }
            )
        }

        OutlinedButton(
            onClick = { backupLauncher.launch("iattend-backup-${LocalDate.now()}.json") },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Back up my data first (optional)") }

        Button(
            onClick = { showConfirm = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (scope == ResetScope.FULL) "Reset everything" else "Delete academic data") }
    }

    if (showConfirm) {
        SpringAlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Are you sure?") },
            text = {
                Text(
                    if (scope == ResetScope.FULL)
                        "This permanently deletes all your data and preferences and can't be undone. You'll go through onboarding again."
                    else
                        "This permanently deletes your subjects, timetable and attendance history and can't be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    when (scope) {
                        ResetScope.ACADEMIC -> settingsViewModel.resetAcademicData {
                            Toast.makeText(context, "Data reset.", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                        ResetScope.FULL -> settingsViewModel.resetFull {
                            context.startActivity(
                                Intent(context, MainActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            )
                        }
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ResetOptionCard(title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RadioButton(selected = selected, onClick = onClick)
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AutoBackupSheetContent(viewModel: SettingsViewModel, onDismiss: () -> Unit) {
    val enabled by viewModel.autoBackupEnabled.collectAsState()
    val frequency by viewModel.autoBackupFrequency.collectAsState()
    val folderUri by viewModel.autoBackupFolderUri.collectAsState()
    val context = LocalContext.current

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.setAutoBackupFolderUri(uri.toString())
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Automated backups", style = MaterialTheme.typography.titleLarge)
            SquircleIconButton(Icons.Default.Close, contentDescription = "Close", onClick = onDismiss)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable automated backups", modifier = Modifier.weight(1f))
            Switch(checked = enabled, onCheckedChange = viewModel::setAutoBackupEnabled)
        }
        if (enabled) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BackupFrequency.entries.forEach { f ->
                    FilterChip(
                        selected = frequency == f,
                        onClick = { viewModel.setAutoBackupFrequency(f) },
                        label = { Text(if (f == BackupFrequency.DAILY) "Daily" else "Weekly") }
                    )
                }
            }
            Column {
                Text(
                    if (folderUri != null) "Backing up to your chosen folder" else "Backing up to app default folder",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { folderPicker.launch(null) }) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Text("Choose folder")
                    }
                    if (folderUri != null) {
                        TextButton(onClick = { viewModel.setAutoBackupFolderUri(null) }) {
                            Text("Use app default instead")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderDefaultsSheetContent(viewModel: SettingsViewModel, onDismiss: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Reminder defaults", style = MaterialTheme.typography.titleLarge)
            SquircleIconButton(Icons.Default.Close, contentDescription = "Close", onClick = onDismiss)
        }
        ReminderDefaultsFields(viewModel)
    }
}

/** Shared with [com.iattend.app.feature.notifications.NotificationSettingsSheetContent] - same
 * defaults, reachable from both Settings and the header bell icon's notification drawer. */
@Composable
fun ReminderDefaultsFields(viewModel: SettingsViewModel) {
    val classOffsets by viewModel.classReminderDefaultOffsets.collectAsState()
    val examOffsets by viewModel.examReminderDefaultOffsets.collectAsState()

    Column {
        Text("Classes", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
        ReminderOffsetPicker(
            selectedMinutes = ReminderOffset.parse(classOffsets),
            onChange = viewModel::setClassReminderDefaultOffsets
        )
    }
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Text("Tests & Exams", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
        ReminderOffsetPicker(
            selectedMinutes = ReminderOffset.parse(examOffsets),
            onChange = viewModel::setExamReminderDefaultOffsets
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(1.5.dp)) { content() }
    }
}

/** Cycles through the theme's own container roles rather than hardcoded pastels, so icon chips stay correct across light/dark/AMOLED/dynamic-color. */
@Composable
private fun settingsIconColors(index: Int): Pair<Color, Color> = when (index % 3) {
    0 -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    1 -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    else -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
}

@Composable
private fun SettingsIconRow(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    colorIndex: Int,
    position: ListItemPosition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor) = settingsIconColors(colorIndex)
    ListItem(
        headline = { Text(title) },
        modifier = modifier,
        supporting = subtitle?.let { { Text(it) } },
        leading = {
            Box(
                modifier = Modifier.size(40.dp).clip(MaterialTheme.shapes.small).background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            }
        },
        trailing = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        shape = position.toShape(),
        onClick = onClick
    )
}

@Composable
private fun RequiredPercentageSheetContent(requiredPercentage: Float, onChange: (Float) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(requiredPercentage.toString()) }
    LaunchedEffect(requiredPercentage) { text = requiredPercentage.toString() }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Required attendance percentage", style = MaterialTheme.typography.titleLarge)
            SquircleIconButton(Icons.Default.Close, contentDescription = "Close", onClick = onDismiss)
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Required %") },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
        Button(
            onClick = {
                text.toFloatOrNull()?.let(onChange)
                onDismiss()
            },
            enabled = text.toFloatOrNull() != null,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) { Text("Save") }
    }
}

@Composable
private fun TrackingDateRangeSheetContent(
    trackingStartDate: LocalDate?,
    trackingEndDate: LocalDate?,
    onChange: (LocalDate?, LocalDate?) -> Unit,
    onDismiss: () -> Unit
) {
    var start by remember(trackingStartDate) { mutableStateOf(trackingStartDate) }
    var end by remember(trackingEndDate) { mutableStateOf(trackingEndDate) }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Tracking period", style = MaterialTheme.typography.titleLarge)
            SquircleIconButton(Icons.Default.Close, contentDescription = "Close", onClick = onDismiss)
        }
        Text(
            "Set tracking window (needed for Computed subjects). Empty ranges allowed — clear to track indefinitely.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        DateRangePickerField(
            label = "Tracking period",
            startDate = start,
            endDate = end,
            onRangeChange = { s, e -> start = s; end = e },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
        // Keep single-date pickers as fallback for precise edits (Cashiro DateRange + individual fields pattern)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DatePickerField(label = "Start", date = start, onDateChange = { start = it }, modifier = Modifier.weight(1f))
            DatePickerField(label = "End", date = end, onDateChange = { end = it }, modifier = Modifier.weight(1f))
        }
        Button(
            onClick = {
                onChange(start, end)
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) { Text("Save") }
        TextButton(onClick = { start = null; end = null }, modifier = Modifier.fillMaxWidth()) { Text("Clear") }
    }
}
