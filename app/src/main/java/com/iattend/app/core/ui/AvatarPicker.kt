package com.iattend.app.core.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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

/** Tap-to-pick-from-gallery avatar plus a DiceBear generated-avatar grid. Shared by Settings'
 * Profile sheet and the onboarding name step so both offer the same picker and stay in sync. */
@Composable
fun AvatarPicker(
    pictureUri: String?,
    onPictureSelected: (Uri) -> Unit,
    onAvatarUrlSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 96.dp
) {
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onPictureSelected)
    }
    var selectedStyle by remember { mutableStateOf(AVATAR_STYLES.first()) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AvatarImage(
            pictureUri = pictureUri,
            size = avatarSize,
            modifier = Modifier.clickable {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )
        Text(
            "Or pick an avatar",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
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
                        .clickable { onAvatarUrlSelected(url) }
                )
            }
        }
    }
}
