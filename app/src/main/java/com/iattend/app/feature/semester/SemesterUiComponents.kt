package com.iattend.app.feature.semester

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iattend.app.core.data.db.Semester
import com.iattend.app.core.ui.DatePickerField
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

@Composable
fun SemesterSelectorChip(
    semester: Semester?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = semester?.name ?: "Select Semester",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 135.dp)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Switch semester",
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun ArchivedSemesterBanner(
    semesterName: String,
    onSwitchToCurrent: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Viewing archived: $semesterName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Medium
                )
            }
            TextButton(onClick = onSwitchToCurrent) {
                Text("Switch", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterSwitcherBottomSheet(
    activeSemester: Semester?,
    semesters: List<Semester>,
    onSelectSemester: (Long) -> Unit,
    onAddNew: () -> Unit,
    onEdit: (Semester) -> Unit,
    onToggleArchive: (Semester) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var semesterToDelete by remember { mutableStateOf<Semester?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Semesters",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onAddNew) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Semester")
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(semesters, key = { it.id }) { semester ->
                    val isSelected = semester.id == activeSemester?.id
                    val today = LocalDate.now()
                    val statusText = when {
                        semester.isArchived -> "Archived"
                        today.isBefore(semester.startDate) -> "Upcoming"
                        today.isAfter(semester.endDate) -> "Ended"
                        else -> "Current"
                    }
                    val statusColor = when (statusText) {
                        "Current" -> MaterialTheme.colorScheme.primary
                        "Archived", "Ended" -> MaterialTheme.colorScheme.outline
                        else -> MaterialTheme.colorScheme.tertiary
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectSemester(semester.id)
                                onDismiss()
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = semester.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = statusColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = statusText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = statusColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "${semester.startDate.format(dateFormatter)} – ${semester.endDate.format(dateFormatter)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onEdit(semester) }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(onClick = { onToggleArchive(semester) }) {
                                    Icon(
                                        imageVector = if (semester.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                        contentDescription = if (semester.isArchived) "Unarchive" else "Archive",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                if (semesters.size > 1) {
                                    IconButton(onClick = { semesterToDelete = semester }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Active",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    semesterToDelete?.let { sem ->
        AlertDialog(
            onDismissRequest = { semesterToDelete = null },
            title = { Text("Delete ${sem.name}?") },
            text = { Text("This will permanently delete this semester and all its subjects, timetables, and attendance records. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(sem.id)
                        semesterToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { semesterToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SemesterEditorDialog(
    initialSemester: Semester? = null,
    existingSemesters: List<Semester> = emptyList(),
    onSave: (name: String, start: LocalDate, end: LocalDate, targetPercentage: Float, copyRulesFrom: Long?, copyHolidaysFrom: Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val isEditing = initialSemester != null
    var name by remember { mutableStateOf(initialSemester?.name ?: "") }
    var startDate by remember { mutableStateOf(initialSemester?.startDate ?: LocalDate.now()) }
    var endDate by remember { mutableStateOf(initialSemester?.endDate ?: LocalDate.now().plusMonths(4)) }
    var targetPercentage by remember { mutableStateOf(initialSemester?.targetPercentage?.toInt()?.toString() ?: "75") }

    var copyRecurringRules by remember { mutableStateOf(!isEditing && existingSemesters.isNotEmpty()) }
    var copyHolidays by remember { mutableStateOf(false) }

    val copySourceSemesterId = existingSemesters.firstOrNull()?.id

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Semester" else "New Semester") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Semester Name") },
                    placeholder = { Text("e.g. Semester 2, Fall 2026") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                DatePickerField(
                    label = "Start Date",
                    date = startDate,
                    onDateChange = { startDate = it },
                    modifier = Modifier.fillMaxWidth()
                )

                DatePickerField(
                    label = "End Date",
                    date = endDate,
                    onDateChange = { endDate = it },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetPercentage,
                    onValueChange = { targetPercentage = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("Target Attendance %") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isEditing && existingSemesters.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = copyRecurringRules,
                                onCheckedChange = { copyRecurringRules = it }
                            )
                            Text("Copy weekend rules from previous semester", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = copyHolidays,
                                onCheckedChange = { copyHolidays = it }
                            )
                            Text("Copy holidays from previous semester", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && !endDate.isBefore(startDate)) {
                        val pct = targetPercentage.toFloatOrNull() ?: 75f
                        onSave(
                            name,
                            startDate,
                            endDate,
                            pct,
                            if (copyRecurringRules) copySourceSemesterId else null,
                            if (copyHolidays) copySourceSemesterId else null
                        )
                    }
                },
                enabled = name.isNotBlank() && !endDate.isBefore(startDate)
            ) {
                Text(if (isEditing) "Save" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
