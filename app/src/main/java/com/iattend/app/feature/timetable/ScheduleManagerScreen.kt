package com.iattend.app.feature.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.core.data.db.Semester
import com.iattend.app.core.navigation.LocalNavigationContentPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.iattend.app.core.tutorial.tutorialTarget
import com.iattend.app.core.ui.hapticClick
import com.iattend.app.feature.semester.ArchivedSemesterBanner
import com.iattend.app.feature.semester.SemesterEditorDialog
import com.iattend.app.feature.semester.SemesterSelectorChip
import com.iattend.app.feature.semester.SemesterSwitcherBottomSheet
import com.iattend.app.feature.semester.SemesterViewModel
import java.time.LocalDate

@Composable
fun ScheduleManagerScreen(
    onSubjects: () -> Unit,
    onRecurringHolidays: () -> Unit,
    onManageTimetable: () -> Unit,
    onManageHolidays: () -> Unit,
    onExtraClasses: () -> Unit,
    onExamManager: () -> Unit,
    semesterViewModel: SemesterViewModel = hiltViewModel()
) {
    val semesterState by semesterViewModel.uiState.collectAsState()
    var showSemesterSheet by remember { mutableStateOf(false) }
    var editingSemester by remember { mutableStateOf<Semester?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = LocalNavigationContentPadding.current)
    ) {
        if (semesterState.isViewingArchived && semesterState.activeSemester != null) {
            ArchivedSemesterBanner(
                semesterName = semesterState.activeSemester!!.name,
                onSwitchToCurrent = {
                    val current = semesterState.semesters.firstOrNull { !it.isArchived && !it.endDate.isBefore(LocalDate.now()) }
                        ?: semesterState.semesters.firstOrNull()
                    if (current != null) semesterViewModel.switchSemester(current.id)
                },
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Schedule Manager", style = MaterialTheme.typography.headlineSmall)
            SemesterSelectorChip(
                semester = semesterState.activeSemester,
                onClick = { showSemesterSheet = true }
            )
        }
        Text(
            "CONFIGURATION",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        ManagerTile(Icons.Default.Group, "Subjects", "Add, edit, and remove subjects", onSubjects, Modifier.tutorialTarget("subjects_tile"))
        ManagerTile(Icons.Default.EventRepeat, "Recurring Holidays", "Set standard off-days (e.g. Sundays)", onRecurringHolidays)
        ManagerTile(Icons.Default.Schedule, "Manage Timetable", "Edit slots and versions", onManageTimetable, Modifier.tutorialTarget("manage_timetable_tile"))
        ManagerTile(Icons.Default.CalendarMonth, "Manage Holidays", "Set exception holidays", onManageHolidays)
        ManagerTile(Icons.AutoMirrored.Filled.EventNote, "Extra Classes", "Schedule one-off sessions", onExtraClasses)
        ManagerTile(Icons.Default.Quiz, "Tests & Exams", "Add and manage assessment timetables", onExamManager)
    }

    if (showSemesterSheet) {
        SemesterSwitcherBottomSheet(
            activeSemester = semesterState.activeSemester,
            semesters = semesterState.semesters,
            onSelectSemester = { id -> semesterViewModel.switchSemester(id) },
            onAddNew = {
                showSemesterSheet = false
                showCreateDialog = true
            },
            onEdit = { sem ->
                showSemesterSheet = false
                editingSemester = sem
            },
            onToggleArchive = { sem -> semesterViewModel.toggleArchive(sem) },
            onDelete = { id -> semesterViewModel.deleteSemester(id) },
            onDismiss = { showSemesterSheet = false }
        )
    }

    if (showCreateDialog) {
        SemesterEditorDialog(
            existingSemesters = semesterState.semesters,
            onSave = { name, start, end, targetPct, copyRules, copyHolidays ->
                semesterViewModel.createSemester(name, start, end, targetPct, copyRules, copyHolidays)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    editingSemester?.let { sem ->
        SemesterEditorDialog(
            initialSemester = sem,
            existingSemesters = semesterState.semesters,
            onSave = { name, start, end, targetPct, _, _ ->
                semesterViewModel.updateSemester(sem.copy(name = name, startDate = start, endDate = end, targetPercentage = targetPct))
                editingSemester = null
            },
            onDismiss = { editingSemester = null }
        )
    }
}

@Composable
private fun ManagerTile(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = hapticClick(onClick))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
