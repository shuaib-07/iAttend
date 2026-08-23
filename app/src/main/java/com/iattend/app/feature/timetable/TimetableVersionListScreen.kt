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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.iattend.app.core.data.db.TimetableVersion
import com.iattend.app.core.ui.SpringAlertDialog
import com.iattend.app.core.ui.SquircleIconButton
import com.iattend.app.core.ui.formatDateWithLong
import com.iattend.app.core.ui.hapticClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableVersionListScreen(
    onBack: () -> Unit,
    onAddVersion: () -> Unit,
    onEditVersion: (Long) -> Unit,
    viewModel: TimetableVersionListViewModel = hiltViewModel()
) {
    val versions by viewModel.versions.collectAsState()
    var pendingDelete by remember { mutableStateOf<TimetableVersion?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timetable Versions") },
                navigationIcon = {
                    SquircleIconButton(Icons.Default.Close, contentDescription = "Close", onClick = onBack)
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = hapticClick(onAddVersion)) { Icon(Icons.Default.Add, contentDescription = "Add timetable version") }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            items(versions, key = { it.id }) { version ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).animateItem().clickable { onEditVersion(version.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Column {
                                Text("Effective: ${formatDateWithLong(version.effectiveFrom)}", style = MaterialTheme.typography.titleMedium)
                                Text("Tap to edit slots", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick = { pendingDelete = version }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete version", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { version ->
        SpringAlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this timetable version?") },
            text = { Text("This removes it and its slots. Occurrences already marked Present/Absent/Cancelled are kept; unmarked future occurrences from this version will be regenerated.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(version); pendingDelete = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
}
