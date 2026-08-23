package com.iattend.app.core.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage

/**
 * Shows a profile avatar from [pictureUri]: a remote DiceBear URL (`AsyncImage`/Coil), a local
 * file path (`BitmapFactory`, same as [pictureUri] set via the system photo picker), or a plain
 * person icon placeholder when null. Shared between Profile's picker and the Home greeting row so
 * both always show the same avatar without duplicating the decode logic.
 */
@Composable
fun AvatarImage(pictureUri: String?, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(size).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when {
            pictureUri == null -> Icon(
                Icons.Default.Person,
                contentDescription = "No profile picture",
                modifier = Modifier.size(size / 2)
            )
            pictureUri.startsWith("http") -> AsyncImage(
                model = pictureUri,
                contentDescription = "Profile picture",
                modifier = Modifier.size(size).clip(CircleShape)
            )
            else -> {
                val bitmap = remember(pictureUri) { BitmapFactory.decodeFile(pictureUri) }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Profile picture",
                        modifier = Modifier.size(size).clip(CircleShape)
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = "No profile picture", modifier = Modifier.size(size / 2))
                }
            }
        }
    }
}
