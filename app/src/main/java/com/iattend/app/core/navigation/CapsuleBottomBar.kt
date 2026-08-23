package com.iattend.app.core.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.iattend.app.core.ui.LottieMorphIcon
import com.iattend.app.core.ui.hapticClick
import com.iattend.app.feature.devsupport.DevSupportTrigger

private data class CapsuleTab(
    val lottieAsset: String,
    val label: String,
    val isSelected: (NavDestination?) -> Boolean,
    val route: Any
)

/**
 * Style B nav bar: a liquid-glass capsule with a sliding selection indicator, reimplemented
 * independently from the general "capsule with sliding pill" idea (see docs/NAVBAR_HEADER_PLAN.md) -
 * no pager, no drag gesture, tap-only, same contextual FAB as the [AppBottomBar] pill style.
 */
@Composable
fun CapsuleBottomBar(navController: NavHostController, backdrop: Backdrop, destination: NavDestination?, modifier: Modifier = Modifier) {
    val tabs = remember {
        listOf(
            CapsuleTab("lottie/home.json", "Home", { it?.hasRoute<HomeRoute>() == true }, HomeRoute),
            CapsuleTab("lottie/calendar.json", "Calendar", { it?.hasRoute<CalendarRoute>() == true }, CalendarRoute),
            CapsuleTab("lottie/timetable.json", "Timetable", { it?.hasRoute<TimetableHubRoute>() == true }, TimetableHubRoute),
            CapsuleTab("lottie/insights.json", "Insights", { it?.hasRoute<InsightsRoute>() == true }, InsightsRoute),
            CapsuleTab("lottie/settings.json", "Settings", { it?.hasRoute<SettingsRoute>() == true }, SettingsRoute)
        )
    }
    // Falling back to index 0 (Home) whenever destination briefly matches no tab - e.g. mid pop-
    // transition returning from a subpage - caused a visible flash to Home before snapping to the
    // real tab. Instead keep showing whichever tab was last actually matched.
    var lastSelectedIndex by remember { mutableStateOf(0) }
    val matchedIndex = tabs.indexOfFirst { it.isSelected(destination) }
    if (matchedIndex >= 0) lastSelectedIndex = matchedIndex
    val selectedIndex = lastSelectedIndex

    val isSettings = destination?.hasRoute<SettingsRoute>() == true

    val surfaceTint = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
    val indicatorColor = MaterialTheme.colorScheme.primaryContainer
    val onIndicatorColor = MaterialTheme.colorScheme.onPrimaryContainer
    val unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val glassEffects: BackdropEffectScope.() -> Unit = {
        vibrancy()
        blur(4f)
        lens(16f, 32f, chromaticAberration = true)
    }

    Row(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
                .clip(CircleShape)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { CircleShape },
                    effects = glassEffects,
                    onDrawSurface = { drawRect(surfaceTint) }
                )
        ) {
            val itemWidth = maxWidth / tabs.size
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidth * selectedIndex,
                animationSpec = tween(260, easing = FastOutSlowInEasing),
                label = "capsuleIndicatorOffset"
            )

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(indicatorColor)
            )

            Row(modifier = Modifier.fillMaxSize()) {
                tabs.forEachIndexed { index, tab ->
                    val selected = index == selectedIndex
                    CapsuleTabItem(
                        tab = tab,
                        selected = selected,
                        selectedColor = onIndicatorColor,
                        unselectedColor = unselectedColor,
                        modifier = Modifier.width(itemWidth).fillMaxHeight(),
                        onClick = hapticClick { navController.navigateToTab(tab.route) }
                    )
                }
            }
        }

        Box {
            if (isSettings) {
                CapsuleFab(
                    lottieAsset = "lottie/heart.json",
                    selected = false,
                    contentDescription = "Support the developer",
                    backdrop = backdrop,
                    surfaceTint = surfaceTint,
                    glassEffects = glassEffects,
                    iconColor = Color.White,
                    onClick = hapticClick { DevSupportTrigger.requestOpen.value = true }
                )
            } else {
                CapsuleManageMenuFab(
                    navController = navController,
                    backdrop = backdrop,
                    surfaceTint = surfaceTint,
                    glassEffects = glassEffects,
                    iconColor = Color.White
                )
            }
        }
    }
}

@Composable
private fun CapsuleTabItem(
    tab: CapsuleTab,
    selected: Boolean,
    selectedColor: Color,
    unselectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier.clip(CircleShape)) {
        LottieMorphIcon(
            assetPath = tab.lottieAsset,
            selected = selected,
            tint = if (selected) selectedColor else unselectedColor,
            contentDescription = tab.label
        )
    }
}

@Composable
private fun CapsuleFab(
    lottieAsset: String,
    selected: Boolean,
    contentDescription: String,
    backdrop: Backdrop,
    surfaceTint: Color,
    iconColor: Color,
    glassEffects: BackdropEffectScope.() -> Unit,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = glassEffects,
                onDrawSurface = { drawRect(surfaceTint) }
            )
    ) {
        LottieMorphIcon(
            assetPath = lottieAsset,
            selected = selected,
            tint = iconColor,
            contentDescription = contentDescription
        )
    }
}

/** "+" FAB with a management dropdown, shared by every root tab except Settings. */
@Composable
private fun CapsuleManageMenuFab(
    navController: NavHostController,
    backdrop: Backdrop,
    surfaceTint: Color,
    iconColor: Color,
    glassEffects: BackdropEffectScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        CapsuleFab(
            lottieAsset = "lottie/plus_to_x.json",
            selected = expanded,
            contentDescription = if (expanded) "Close" else "Manage",
            backdrop = backdrop,
            surfaceTint = surfaceTint,
            iconColor = iconColor,
            glassEffects = glassEffects,
            onClick = hapticClick { expanded = !expanded }
        )

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
