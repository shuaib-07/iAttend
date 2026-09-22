package com.iattend.app.feature.assessment

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.core.data.db.AssessmentType
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.ui.SpringAlertDialog
import com.iattend.app.core.ui.SquircleIconButton
import com.iattend.app.core.ui.formatDateShort
import com.iattend.app.core.ui.formatTime
import com.iattend.app.core.ui.LocalTimeFormat
import com.iattend.app.core.ui.hapticClick

/** Top-level "Tests & Exams" hub across all subjects, reached from the Timetable hub. Adding
 * still requires picking a subject first since [com.iattend.app.core.data.db.Assessment] is
 * owned by one - see AssessmentEditorRoute. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllAssessmentsScreen(
    onBack: () -> Unit,
    onAddForSubject: (Long) -> Unit,
    onEdit: (subjectId: Long, assessmentId: Long) -> Unit,
    viewModel: AllAssessmentsViewModel = hiltViewModel()
) {
    val rows by viewModel.rows.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    var showSubjectPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tests & Exams") },
                navigationIcon = {
                    SquircleIconButton(Icons.Default.ArrowBack, contentDescription = "Back", onClick = onBack)
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = hapticClick { showSubjectPicker = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        if (rows.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No tests or exams yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                items(rows, key = { it.assessment.id }) { row ->
                    AllAssessmentRow(row, onClick = { onEdit(row.assessment.subjectId, row.assessment.id) }, modifier = Modifier.animateItem())
                }
            }
        }
    }

    if (showSubjectPicker) {
        SubjectPickerDialog(
            subjects = subjects,
            onDismiss = { showSubjectPicker = false },
            onPick = { subject ->
                showSubjectPicker = false
                onAddForSubject(subject.id)
            }
        )
    }
}

@Composable
private fun AllAssessmentRow(row: AllAssessmentsRow, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val assessment = row.assessment
    val typeColor = if (assessment.type == AssessmentType.EXAM) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
    Card(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(12.dp).background(typeColor, CircleShape))
            Column(modifier = Modifier.weight(1f)) {
                Text(assessment.title?.takeIf { it.isNotBlank() } ?: row.subjectName)
                Text(row.subjectName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val dateText = "${formatDateShort(assessment.date)} - ${formatTime(assessment.startTime, LocalTimeFormat.current)} to ${formatTime(assessment.endTime, LocalTimeFormat.current)}"
                Text(dateText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            assessment.totalMarks?.let { Text("$it marks", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun SubjectPickerDialog(subjects: List<Subject>, onDismiss: () -> Unit, onPick: (Subject) -> Unit) {
    SpringAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose subject") },
        text = {
            if (subjects.isEmpty()) {
                Text("Add a subject first, then come back to add its tests and exams.")
            } else {
                Column {
                    subjects.forEach { subject ->
                        Text(
                            subject.name,
                            modifier = Modifier.fillMaxWidth().clickable { onPick(subject) }.padding(vertical = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
