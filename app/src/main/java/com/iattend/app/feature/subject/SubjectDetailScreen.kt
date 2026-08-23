package com.iattend.app.feature.subject

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.core.data.db.ClassOccurrence
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.core.ui.ProgressRing
import com.iattend.app.core.ui.SquircleIconButton
import com.iattend.app.core.ui.SpringAlertDialog
import com.iattend.app.core.ui.hapticClick
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val historyDateFormat = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onAssessments: (Long) -> Unit,
    viewModel: SubjectDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.deleted.collect { onBack() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.subject?.name ?: "") },
                navigationIcon = {
                    SquircleIconButton(Icons.Default.ArrowBack, contentDescription = "Back", onClick = onBack)
                },
                actions = {
                    state.subject?.let { subject ->
                        IconButton(onClick = { onEdit(subject.id) }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                        IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                    }
                }
            )
        }
    ) { padding ->
        val stats = state.stats
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        .clickable { state.subject?.let { onAssessments(it.id) } }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tests & Exams")
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                }
            }
            if (stats != null) {
                item {
                    val belowThreshold = stats.totalSoFar > 0 && stats.percentage < state.requiredPercentage
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProgressRing(percentage = stats.percentage, aboveThreshold = !belowThreshold, size = 88.dp)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${stats.attended} / ${stats.totalSoFar}", style = MaterialTheme.typography.headlineSmall)
                                Text("Required: ${state.requiredPercentage.toInt()}%")
                                when {
                                    stats.totalOverall == null -> Text("Projection: N/A", style = MaterialTheme.typography.bodyMedium)
                                    else -> {
                                        Text("Total: ${stats.totalOverall}, remaining: ${stats.remaining}", style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            if ((stats.classesNeeded ?: 0) > 0) "Needs ${stats.classesNeeded} more to hit target"
                                            else "Can skip ${stats.classesCanSkip} and stay above target",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item { Text("History", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) }
            }
            if (state.history.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No classes yet")
                    }
                }
            } else {
                items(state.history, key = { it.id }) { occurrence ->
                    HistoryRow(
                        occurrence,
                        onClick = { viewModel.cycleStatus(occurrence) },
                        onSetCancelReason = { reason -> viewModel.setCancelReason(occurrence, reason) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        SpringAlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete subject?") },
            text = { Text("This removes ${state.subject?.name} and its entire attendance history. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; viewModel.delete() }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun HistoryRow(
    occurrence: ClassOccurrence,
    onClick: () -> Unit,
    onSetCancelReason: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showReasonDialog by remember(occurrence.id) { mutableStateOf(false) }
    val markable = !occurrence.date.isAfter(LocalDate.now())
    val statusColor = when (occurrence.status) {
        OccurrenceStatus.PRESENT -> MaterialTheme.colorScheme.primary
        OccurrenceStatus.ABSENT -> MaterialTheme.colorScheme.error
        OccurrenceStatus.CANCELLED, OccurrenceStatus.UNMARKED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)
            .alpha(if (markable) 1f else 0.5f)
            .let { if (markable) it.clickable(onClick = hapticClick(onClick)) else it }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(occurrence.date.format(historyDateFormat))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(occurrence.status.name, color = statusColor)
                    if (occurrence.status == OccurrenceStatus.CANCELLED) {
                        IconButton(onClick = { showReasonDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit cancel reason")
                        }
                    }
                }
            }
            if (occurrence.status == OccurrenceStatus.CANCELLED && !occurrence.cancelReason.isNullOrBlank()) {
                Text(
                    occurrence.cancelReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    if (showReasonDialog) {
        var draftReason by remember { mutableStateOf(occurrence.cancelReason ?: "") }
        SpringAlertDialog(
            onDismissRequest = { showReasonDialog = false },
            title = { Text("Cancellation reason") },
            text = {
                OutlinedTextField(
                    value = draftReason,
                    onValueChange = { draftReason = it },
                    label = { Text("Reason (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { onSetCancelReason(draftReason); showReasonDialog = false }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showReasonDialog = false }) { Text("Cancel") } }
        )
    }
}
