package com.iattend.app.feature.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.iattend.app.core.ui.hapticClick

private data class Feature(val title: String, val description: String, val icon: @Composable () -> Unit)

@Composable
fun FeaturesStep() {
    val features = remember {
        listOf(
            Feature("Attendance tracking", "Mark present, absent or cancelled per class") { CheckToggleIcon(Icons.Filled.CheckCircle, Icons.Filled.RadioButtonUnchecked) },
            Feature("Flexible timetable", "Different subjects and timings, any day") { PopIcon(Icons.Filled.CalendarMonth) },
            Feature("Recurring holidays", "Set your weekly off-days once") { CheckToggleIcon(Icons.Filled.EventAvailable, Icons.Filled.EventBusy) },
            Feature("Extra classes", "Schedule one-off sessions anytime") { PopIcon(Icons.Filled.AddCircle) },
            Feature("Multiple timetables", "Switch versions without losing your class count") { PulseIcon(Icons.Filled.Layers) },
            Feature("Exams & tests", "Track dates, marks and portions") { WobbleIcon(Icons.Filled.EditCalendar) },
            Feature("Smart reminders", "Get notified before classes, tests and exams") { WobbleIcon(Icons.Filled.Notifications) },
            Feature("Themes", "Light, dark, AMOLED, and accent colors") { ColorCycleIcon(Icons.Filled.Palette) },
            Feature("Bunk calculator", "See exactly how many classes you can skip") { CountdownIcon(Icons.Filled.EventBusy) },
            Feature("Share your timetable", "Send it to friends in one tap") { PopIcon(Icons.AutoMirrored.Filled.Send) },
            Feature("Home screen widget", "Glance at upcoming classes without opening the app") { PulseIcon(Icons.Filled.Widgets) },
            Feature("Fully offline & private", "No account, no analytics, nothing leaves your device") { PulseIcon(Icons.Filled.Lock) }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Everything you need", style = MaterialTheme.typography.headlineSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Text(
            "A quick look at what iAttend can do",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp, top = 4.dp)
        )
        val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
        Box(modifier = Modifier.weight(1f)) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(features) { feature -> FeatureCard(feature) }
            }

            val canScrollMore by remember { androidx.compose.runtime.derivedStateOf { gridState.canScrollForward } }
            androidx.compose.animation.AnimatedVisibility(
                visible = canScrollMore,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(androidx.compose.ui.graphics.Color.Transparent, MaterialTheme.colorScheme.background)
                                )
                            )
                    )
                    val infinite = rememberInfiniteTransition(label = "scrollHint")
                    val offsetY by infinite.animateFloat(0f, 8f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "bounce")
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Scroll for more",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .graphicsLayer { translationY = offsetY }
                            .background(MaterialTheme.colorScheme.background, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(feature: Feature) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().aspectRatio(0.85f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(10.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, androidx.compose.foundation.shape.RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) { feature.icon() }
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(feature.title, style = MaterialTheme.typography.labelLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                Text(feature.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun NotificationPermissionStep(onGranted: () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        granted = isGranted
        if (isGranted) onGranted()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier = Modifier.size(130.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            WobbleIcon(Icons.Filled.Notifications, size = 56.dp, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Text("Stay on top of your classes", style = MaterialTheme.typography.headlineSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, textAlign = TextAlign.Center)
        Text(
            "iAttend can remind you before class starts, right after it ends, and before tests and exams. Nothing is sent anywhere - reminders are scheduled entirely on your device.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (granted) {
            Text("Notifications enabled", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        } else {
            Button(onClick = hapticClick { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Enable notifications") }
            TextButton(onClick = {}) { Text("Skip for now", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
fun ThemeStep() {
    val viewModel: com.iattend.app.feature.settings.SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Make it yours", style = MaterialTheme.typography.headlineSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Text(
            "Pick a look - change this anytime later in Settings",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp, top = 4.dp)
        )
        com.iattend.app.feature.settings.AppearanceSelectors(viewModel)
    }
}

// --- Small reusable animated-icon wrappers for feature cards ---

@Composable
private fun PulseIcon(icon: ImageVector, tint: Color = MaterialTheme.colorScheme.primary) {
    val infinite = rememberInfiniteTransition(label = "pulse")
    val scale by infinite.animateFloat(0.85f, 1.15f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "scale")
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(48.dp).scale(scale))
}

@Composable
private fun WobbleIcon(icon: ImageVector, size: androidx.compose.ui.unit.Dp = 28.dp, tint: Color = MaterialTheme.colorScheme.primary) {
    val infinite = rememberInfiniteTransition(label = "wobble")
    val rotation by infinite.animateFloat(-14f, 14f, infiniteRepeatable(tween(220), RepeatMode.Reverse), label = "rotation")
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size).rotate(rotation))
}

@Composable
private fun CheckToggleIcon(onIcon: ImageVector, offIcon: ImageVector, tint: Color = MaterialTheme.colorScheme.primary) {
    val infinite = rememberInfiniteTransition(label = "checkToggle")
    val progress by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "progress")
    Crossfade(targetState = progress > 0.5f, label = "checkCrossfade") { checked ->
        Icon(if (checked) onIcon else offIcon, contentDescription = null, tint = tint, modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun ColorCycleIcon(icon: ImageVector) {
    val colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.secondary)
    val infinite = rememberInfiniteTransition(label = "colorCycle")
    val index by infinite.animateFloat(0f, colors.size.toFloat(), infiniteRepeatable(tween(2400), RepeatMode.Restart), label = "colorIndex")
    Icon(icon, contentDescription = null, tint = colors[index.toInt().coerceIn(0, colors.size - 1)], modifier = Modifier.size(48.dp))
}

@Composable
private fun CountdownIcon(icon: ImageVector) {
    val infinite = rememberInfiniteTransition(label = "countdown")
    val n by infinite.animateFloat(6f, 0f, infiniteRepeatable(tween(2100), RepeatMode.Restart), label = "n")
    Box(contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), modifier = Modifier.size(48.dp))
        Text(n.toInt().toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun PopIcon(icon: ImageVector, tint: Color = MaterialTheme.colorScheme.primary) {
    val infinite = rememberInfiniteTransition(label = "pop")
    val scale by infinite.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = androidx.compose.animation.core.FastOutSlowInEasing), RepeatMode.Reverse),
        label = "popScale"
    )
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(48.dp).scale(scale))
}
