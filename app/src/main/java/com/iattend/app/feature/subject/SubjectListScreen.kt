package com.iattend.app.feature.subject

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.ui.SquircleIconButton
import com.iattend.app.core.ui.hapticClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectListScreen(
    onBack: () -> Unit,
    onAddSubject: () -> Unit,
    onEditSubject: (Long) -> Unit,
    viewModel: SubjectListViewModel = hiltViewModel()
) {
    val subjects by viewModel.subjects.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subjects") },
                navigationIcon = {
                    SquircleIconButton(Icons.Default.ArrowBack, contentDescription = "Back", onClick = onBack)
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = hapticClick(onAddSubject)) { Icon(Icons.Default.Add, contentDescription = "Add subject") }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            items(subjects, key = { it.id }) { subject ->
                SubjectRow(subject, onClick = { onEditSubject(subject.id) }, modifier = Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun SubjectRow(subject: Subject, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color(subject.colorArgb), CircleShape)
            )
            Text("${subject.name} (${subject.code})")
        }
    }
}
