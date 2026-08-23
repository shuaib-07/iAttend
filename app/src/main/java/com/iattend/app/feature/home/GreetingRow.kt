package com.iattend.app.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.core.ui.AvatarImage
import com.iattend.app.feature.profile.ProfileViewModel
import java.time.LocalTime

/**
 * Home-only header row: avatar + name + a time-of-day greeting. [collapseFraction] (0f expanded,
 * 1f collapsed) shrinks/fades the greeting line away as the Home list scrolls, leaving just the
 * avatar + name - driven directly by the caller's own LazyListState scroll offset, not a separate
 * animation, so it tracks the finger 1:1 like a parallax list header.
 */
@Composable
fun GreetingRow(collapseFraction: Float, modifier: Modifier = Modifier, viewModel: ProfileViewModel = hiltViewModel()) {
    val name by viewModel.name.collectAsState()
    val pictureUri by viewModel.pictureUri.collectAsState()
    val greeting = remember { timeOfDayGreeting() }

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        AvatarImage(pictureUri = pictureUri, size = 40.dp)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(name.ifBlank { "You" }, style = MaterialTheme.typography.titleMedium)
            Box(
                modifier = Modifier
                    .height(lerp(20.dp, 0.dp, collapseFraction))
                    .graphicsLayer { alpha = 1f - collapseFraction }
            ) {
                Text(
                    greeting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

private fun timeOfDayGreeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Good night"
}
