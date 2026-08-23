package com.iattend.app.feature.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.iattend.app.core.ui.hapticClick

@Composable
fun ScheduleManagerScreen(
    onSubjects: () -> Unit,
    onRecurringHolidays: () -> Unit,
    onManageTimetable: () -> Unit,
    onManageHolidays: () -> Unit,
    onExtraClasses: () -> Unit,
    onExamManager: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 110.dp)
    ) {
        Text("Schedule Manager", style = MaterialTheme.typography.headlineSmall)
        Text(
            "CONFIGURATION",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        ManagerTile(Icons.Default.Group, "Subjects", "Add, edit, and remove subjects", onSubjects)
        ManagerTile(Icons.Default.EventRepeat, "Recurring Holidays", "Set standard off-days (e.g. Sundays)", onRecurringHolidays)
        ManagerTile(Icons.Default.Schedule, "Manage Timetable", "Edit slots and versions", onManageTimetable)
        ManagerTile(Icons.Default.CalendarMonth, "Manage Holidays", "Set exception holidays", onManageHolidays)
        ManagerTile(Icons.AutoMirrored.Filled.EventNote, "Extra Classes", "Schedule one-off sessions", onExtraClasses)
        ManagerTile(Icons.Default.Quiz, "Tests & Exams", "Add and manage assessment timetables", onExamManager)
    }
}

@Composable
private fun ManagerTile(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
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
