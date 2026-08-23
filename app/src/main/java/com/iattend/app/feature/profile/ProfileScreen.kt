package com.iattend.app.feature.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.iattend.app.core.ui.AvatarImage
import com.iattend.app.core.ui.SquircleIconButton

/** Fixed seeds so the same 10 avatars show every time the picker opens (not random) - reused across
 * every style below, so switching styles re-imagines the same "people" rather than a new random set. */
private val DICEBEAR_AVATAR_SEEDS = listOf(
    "Kiwi", "Sage", "Nova", "Milo", "Zara", "Finn", "Luna", "Remy", "Ivy", "Otto"
)

private data class AvatarStyle(val slug: String, val label: String)

private val AVATAR_STYLES = listOf(
    AvatarStyle("avataaars", "Avataaars"),
    AvatarStyle("open-peeps", "Open Peeps"),
    AvatarStyle("notionists", "Notionists"),
    AvatarStyle("lorelei", "Lorelei")
)

private fun diceBearUrl(style: String, seed: String) = "https://api.dicebear.com/9.x/$style/png?seed=$seed"

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

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::setPicture)
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Profile", style = MaterialTheme.typography.titleLarge)
            SquircleIconButton(Icons.Default.Close, contentDescription = "Close", onClick = onDismiss)
        }
        AvatarImage(
            pictureUri = pictureUri,
            size = 96.dp,
            modifier = Modifier.clickable {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
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

        Text(
            "Or pick an avatar",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        var selectedStyle by remember { mutableStateOf(AVATAR_STYLES.first()) }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AVATAR_STYLES) { style ->
                FilterChip(
                    selected = style == selectedStyle,
                    onClick = { selectedStyle = style },
                    label = { Text(style.label) }
                )
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DICEBEAR_AVATAR_SEEDS.forEach { seed ->
                val url = diceBearUrl(selectedStyle.slug, seed)
                AvatarImage(
                    pictureUri = url,
                    size = 48.dp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { viewModel.setAvatarUrl(url) }
                )
            }
        }
    }
}
