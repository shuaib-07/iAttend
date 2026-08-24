package com.iattend.app.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.iattend.app.core.ui.AvatarPicker
import com.iattend.app.core.ui.SquircleIconButton

/** Bottom-sheet content (see ModalSheet) - no Scaffold of its own, hosted by SettingsScreen. */
@Composable
fun ProfileSheetContent(
    onDismiss: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val name by viewModel.name.collectAsState()
    val pictureUri by viewModel.pictureUri.collectAsState()
    // Local draft, decoupled from the DataStore-backed StateFlow. Only commits to viewModel.setName()
    // on explicit Save (below) - typing no longer writes through on every keystroke. Keyed on name so
    // the draft re-syncs once the real persisted value loads in (StateFlow starts at "" before the
    // first DataStore emission) instead of getting stuck comparing against that stale initial "".
    var draftName by remember(name) { mutableStateOf(name) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Profile", style = MaterialTheme.typography.titleLarge)
            SquircleIconButton(Icons.Default.Close, contentDescription = "Close", onClick = onDismiss)
        }
        AvatarPicker(
            pictureUri = pictureUri,
            onPictureSelected = viewModel::setPicture,
            onAvatarUrlSelected = viewModel::setAvatarUrl
        )
        OutlinedTextField(
            value = draftName,
            onValueChange = { draftName = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                viewModel.setName(draftName)
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save") }
    }
}
