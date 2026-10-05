package com.iattend.app.feature.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.iattend.app.ui.theme.foregroundFor
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.iattend.app.core.datastore.NavBarStyle
import com.iattend.app.core.datastore.ThemeMode
import com.iattend.app.core.ui.PreferenceSwitch
import com.iattend.app.core.ui.SquircleIconButton
import com.iattend.app.core.ui.overScrollVertical
import com.iattend.app.ui.theme.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = rememberHazeState()

    // Single source for haze + overscroll config parity with Cashiro AppearanceScreen
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // Reuse app's haze blur 10dp config via BrandHeader-style TopAppBar
            // Simplified: regular TopAppBar that participates in hazeSource below
            TopAppBar(
                title = { Text("Appearance") },
                navigationIcon = {
                    SquircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)
                },
                scrollBehavior = scrollBehaviorSmall,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.0f), scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            )
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
                    .overScrollVertical()
                    .verticalScroll(rememberScrollState())
                    .padding(top = paddingValues.calculateTopPadding())
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AppearanceSelectors(viewModel)
            }
        }
    }
}

/** Theme mode / nav style / dynamic color / accent selectors - shared between [AppearanceScreen]
 * (Settings) and the onboarding theme step, both just drive the same [SettingsViewModel]. */
@Composable
fun AppearanceSelectors(viewModel: SettingsViewModel) {
    val themeMode by viewModel.themeMode.collectAsState()
    val dynamicColorEnabled by viewModel.dynamicColorEnabled.collectAsState()
    val navBarStyle by viewModel.navBarStyle.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    // Theme 4-box selector (System / Light / Dark / Amoled) — Cashiro 3-box idea extended
    Column(
        modifier = Modifier.animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Theme", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            ThemeBox(
                label = "System",
                icon = Icons.Default.AutoAwesome,
                selected = themeMode == ThemeMode.SYSTEM,
                onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 4.dp),
                modifier = Modifier.weight(1f)
            )
            ThemeBox(
                label = "Light",
                icon = Icons.Default.LightMode,
                selected = themeMode == ThemeMode.LIGHT,
                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.weight(1f)
            )
            ThemeBox(
                label = "Dark",
                icon = Icons.Default.DarkMode,
                selected = themeMode == ThemeMode.DARK,
                onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.weight(1f)
            )
            ThemeBox(
                label = "Amoled",
                icon = Icons.Default.DarkMode,
                selected = themeMode == ThemeMode.AMOLED,
                onClick = { viewModel.setThemeMode(ThemeMode.AMOLED) },
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple(NavBarStyle.PILL, "Pill", "Classic"),
                Triple(NavBarStyle.CAPSULE, "Capsule", "Floating"),
                Triple(NavBarStyle.EXPANDABLE, "Rectangle", "Expandable")
            ).forEach { (style, label, subtitle) ->
                val selected = navBarStyle == style
                val foreground = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                Box(
                    modifier = Modifier.weight(1f).heightIn(min = 80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { viewModel.setNavBarStyle(style) })
                        .padding(horizontal = 4.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = foreground)
                        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = foreground.copy(alpha = 0.7f))
                    }
                }
            }
        }
        // Grouped PreferenceSwitches — Dynamic color (reuses global PreferenceSwitch)
        Column(verticalArrangement = Arrangement.spacedBy(1.5.dp), modifier = Modifier.animateContentSize()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PreferenceSwitch(
                    title = "Dynamic color",
                    subtitle = "Material You — from wallpaper",
                    checked = dynamicColorEnabled,
                    onCheckedChange = viewModel::setDynamicColorEnabled,
                    isSingle = false,
                    isFirst = true,
                    padding = PaddingValues(horizontal = 16.dp)
                )
            }
        }

        // Accent picker — 10 Cashiro themes (5 Catppuccin + 5 Rosé Pine) + Forest default, shown only when custom (dynamic off)
        AnimatedVisibility(
            visible = !dynamicColorEnabled,
            enter = fadeIn() + slideInVertically { -it / 4 },
            exit = fadeOut() + slideOutVertically { -it / 4 }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Accent", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp))
                Text(
                    "Forest is default — pick a Cashiro accent to tint primary / secondary / tertiary",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(AvailableAccents) { accent ->
                        val isSelected = accentColor == accent
                        val preview = getAccentPreviewColor(accent, isDark)
                        AccentChip(accent = accent, color = preview, isSelected = isSelected, onClick = { viewModel.setAccentColor(accent) })
                    }
                }
            }
        }
    }
}

private fun getAccentPreviewColor(accent: String, isDark: Boolean): Color = when (accent) {
    "FOREST" -> if (isDark) ForestPrimaryDark else ForestPrimary
    "ROSEWATER" -> if (isDark) Macchiato_Rosewater else Latte_Rosewater
    "BLUE" -> if (isDark) Macchiato_Blue else Latte_Blue
    "GREEN" -> if (isDark) Macchiato_Green else Latte_Green
    "MAUVE" -> if (isDark) Macchiato_Mauve else Latte_Mauve
    "PEACH" -> if (isDark) Macchiato_Peach else Latte_Peach
    "PINE_ROSE" -> if (isDark) RosePine_Rose else Dawn_Rose
    "PINE_IRIS" -> if (isDark) RosePine_Iris else Dawn_Iris
    "PINE_PINE" -> if (isDark) RosePine_Pine else Dawn_Pine
    "PINE_GOLD" -> if (isDark) RosePine_Gold else Dawn_Gold
    "PINE_FOAM" -> if (isDark) RosePine_Foam else Dawn_Foam
    else -> Color.Gray
}

@Composable
private fun AccentChip(accent: String, color: Color, isSelected: Boolean, onClick: () -> Unit) {
    val label = accent.replace("_", " ").lowercase().replaceFirstChar { it.titlecase() }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = CircleShape
                )
                .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = foregroundFor(color), modifier = Modifier.size(24.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ThemeBox(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceContainerLow, shape = shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface)
        }
    }
}
