package com.iattend.app.feature.assessment

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import com.iattend.app.core.ui.EmptyListState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.core.data.db.Assessment
import com.iattend.app.core.data.db.AssessmentType
import com.iattend.app.core.ui.SquircleIconButton
import com.iattend.app.core.ui.formatDateShort
import com.iattend.app.core.ui.formatTime
import com.iattend.app.core.ui.LocalTimeFormat
import com.iattend.app.core.ui.hapticClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentListScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: AssessmentListViewModel = hiltViewModel()
) {
    val assessments by viewModel.assessments.collectAsState()
    val subjectName by viewModel.subjectName.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (subjectName.isBlank()) "Tests & Exams" else "$subjectName - Tests & Exams", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                navigationIcon = {
                    SquircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = hapticClick(onAdd)) { Icon(Icons.Default.Add, contentDescription = "Add test or exam") }
        }
    ) { padding ->
        if (assessments.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyListState("No tests or exams yet", "Schedule an assessment for this subject.", actionLabel = "Add test or exam", onAction = onAdd)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp)) {
                items(assessments, key = { it.id }) { assessment ->
                    AssessmentRow(assessment, onClick = { onEdit(assessment.id) }, modifier = Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun AssessmentRow(assessment: Assessment, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val typeColor = if (assessment.type == AssessmentType.EXAM) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
    Card(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(12.dp).background(typeColor, CircleShape))
            Column(modifier = Modifier.weight(1f)) {
                Text(assessment.title?.takeIf { it.isNotBlank() } ?: assessment.type.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium)
                val dateText = "${formatDateShort(assessment.date)} - ${formatTime(assessment.startTime, LocalTimeFormat.current)} to ${formatTime(assessment.endTime, LocalTimeFormat.current)}"
                Text("${assessment.type.name.lowercase().replaceFirstChar { it.uppercase() }} · $dateText", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            assessment.totalMarks?.let { Text("$it marks", style = MaterialTheme.typography.bodySmall) }
        }
    }
}
