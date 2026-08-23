package com.iattend.app.feature.timetable

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackdatedFillScreen(
    onDone: () -> Unit,
    viewModel: BackdatedFillViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.doneEvents.collect { onDone() } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Fill in past classes") }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("This timetable starts in the past. Choose how to fill in attendance before today for each subject — you can edit individual dates afterward.")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Apply to all:")
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val allMatch = { choice: FillChoice -> state.subjects.isNotEmpty() && state.subjects.all { it.choice == choice } }
                FilterChip(selected = allMatch(FillChoice.PRESENT_ALL), onClick = { viewModel.applyPresetToAll(FillChoice.PRESENT_ALL) }, label = { Text("Present all") })
                FilterChip(selected = allMatch(FillChoice.ABSENT_ALL), onClick = { viewModel.applyPresetToAll(FillChoice.ABSENT_ALL) }, label = { Text("Absent all") })
                FilterChip(selected = allMatch(FillChoice.LEAVE_UNMARKED), onClick = { viewModel.applyPresetToAll(FillChoice.LEAVE_UNMARKED) }, label = { Text("Leave unmarked") })
            }

            if (state.subjects.isEmpty()) {
                Text(
                    "No past classes fall in this window for any subject - nothing to fill in. Just continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(state.subjects, key = { it.subject.id }) { sf ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${sf.subject.name} — ${sf.occurrenceCount} classes before today")
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(selected = sf.choice == FillChoice.PRESENT_ALL, onClick = { viewModel.setChoice(sf.subject.id, FillChoice.PRESENT_ALL) }, label = { Text("Present all") })
                                FilterChip(selected = sf.choice == FillChoice.ABSENT_ALL, onClick = { viewModel.setChoice(sf.subject.id, FillChoice.ABSENT_ALL) }, label = { Text("Absent all") })
                                FilterChip(selected = sf.choice == FillChoice.LEAVE_UNMARKED, onClick = { viewModel.setChoice(sf.subject.id, FillChoice.LEAVE_UNMARKED) }, label = { Text("Leave unmarked") })
                                FilterChip(selected = sf.choice == FillChoice.AGGREGATE_BASELINE, onClick = { viewModel.setChoice(sf.subject.id, FillChoice.AGGREGATE_BASELINE) }, label = { Text("I know my count") })
                            }
                            if (sf.choice == FillChoice.AGGREGATE_BASELINE) {
                                OutlinedTextField(
                                    value = sf.aggregateAttended,
                                    onValueChange = { viewModel.setAggregateAttended(sf.subject.id, it) },
                                    label = { Text("Classes attended so far (of ${sf.occurrenceCount})") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            Button(onClick = viewModel::confirm, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
        }
    }
}
