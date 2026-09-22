package com.iattend.app.core.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.iattend.app.core.ui.LottieMorphIcon
import com.iattend.app.core.ui.hapticClick
import com.iattend.app.feature.devsupport.DevSupportTrigger

private data class NavTab(val lottieAsset: String, val label: String, val isSelected: (NavDestination?) -> Boolean, val route: Any)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppBottomBar(navController: NavHostController, destination: NavDestination?, modifier: Modifier = Modifier) {
    val tabs = remember {
        listOf(
            NavTab("lottie/home.json", "Home", { it?.hasRoute<HomeRoute>() == true }, HomeRoute),
            NavTab("lottie/calendar.json", "Calendar", { it?.hasRoute<CalendarRoute>() == true }, CalendarRoute()),
            NavTab("lottie/timetable.json", "Timetable", { it?.hasRoute<TimetableHubRoute>() == true }, TimetableHubRoute),
            NavTab("lottie/insights.json", "Insights", { it?.hasRoute<InsightsRoute>() == true }, InsightsRoute),
            NavTab("lottie/settings.json", "Settings", { it?.hasRoute<SettingsRoute>() == true }, SettingsRoute)
        )
    }

    val configuration = LocalConfiguration.current
    val fontScale = LocalDensity.current.fontScale
    val shouldHideLabel = fontScale > 1.25f || configuration.screenWidthDp < 400

    val isSettings = destination?.hasRoute<SettingsRoute>() == true

    HorizontalFloatingToolbar(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        expanded = true,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(
            toolbarContentColor = MaterialTheme.colorScheme.onSurface,
            toolbarContainerColor = MaterialTheme.colorScheme.primary
        ),
        floatingActionButton = {
            if (isSettings) {
                QuickAddFab(
                    lottieAsset = "lottie/heart.json",
                    selected = false,
                    contentDescription = "Support the developer",
                    onClick = hapticClick { DevSupportTrigger.requestOpen.value = true }
                )
            } else {
                ManageMenuFab(navController)
            }
        },
        content = {
            tabs.forEach { tab ->
                val selected = tab.isSelected(destination)
                FloatingNavItem(
                    lottieAsset = tab.lottieAsset,
                    label = tab.label,
                    selected = selected,
                    hideLabel = shouldHideLabel,
                    onClick = hapticClick { navController.navigateToTab(tab.route) }
                )
            }
        }
    )
}

@Composable
internal fun QuickAddFab(
    onClick: () -> Unit,
    lottieAsset: String = "lottie/plus_to_x.json",
    selected: Boolean = false,
    contentDescription: String = "Quick add"
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.large,
        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
    ) {
        LottieMorphIcon(
            assetPath = lottieAsset,
            selected = selected,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            contentDescription = contentDescription
        )
    }
}

/** "+" FAB with a management dropdown, shared by every root tab except Settings. */
@Composable
internal fun ManageMenuFab(navController: NavHostController) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FloatingActionButton(
            onClick = hapticClick { expanded = !expanded },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.large,
            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
        ) {
            LottieMorphIcon(
                assetPath = "lottie/plus_to_x.json",
                selected = expanded,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                contentDescription = if (expanded) "Close" else "Manage"
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Subjects") },
                leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                onClick = { expanded = false; navController.navigate(SubjectListRoute) }
            )
            DropdownMenuItem(
                text = { Text("Timetable versions") },
                leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                onClick = { expanded = false; navController.navigate(TimetableVersionListRoute) }
            )
            DropdownMenuItem(
                text = { Text("Holidays") },
                leadingIcon = { Icon(Icons.Default.BeachAccess, contentDescription = null) },
                onClick = { expanded = false; navController.navigate(HolidayListRoute) }
            )
            DropdownMenuItem(
                text = { Text("Recurring holidays") },
                leadingIcon = { Icon(Icons.Default.EventRepeat, contentDescription = null) },
                onClick = { expanded = false; navController.navigate(RecurringHolidayRoute) }
            )
            DropdownMenuItem(
                text = { Text("Extra classes") },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = { expanded = false; navController.navigate(ExtraClassesRoute()) }
            )
        }
    }
}

@Composable
private fun FloatingNavItem(
    lottieAsset: String,
    label: String,
    selected: Boolean,
    hideLabel: Boolean,
    onClick: () -> Unit
) {
    val showLabel = selected && !hideLabel
    val labelWidth by animateDpAsState(
        targetValue = if (showLabel) 72.dp else 0.dp,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "navLabelWidth"
    )
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .width(48.dp + labelWidth)
            .height(48.dp),
        colors = if (selected) {
            IconButtonDefaults.filledIconButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.surface
            )
        } else {
            IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                containerColor = Color.Transparent
            )
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            LottieMorphIcon(assetPath = lottieAsset, selected = selected, tint = tint, size = 24.dp, contentDescription = label)
            if (showLabel) {
                Spacer(Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
    }
}

internal fun NavHostController.navigateToTab(route: Any) {
    // Home can already be underneath a date-specific CalendarRoute opened from its unmarked list.
    // Pop directly to that existing Home entry instead of saving/restoring the Calendar stack.
    if (route == HomeRoute && popBackStack<HomeRoute>(inclusive = false)) return

    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
