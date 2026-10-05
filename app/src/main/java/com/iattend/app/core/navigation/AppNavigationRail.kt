package com.iattend.app.core.navigation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import com.iattend.app.core.ui.LottieMorphIcon
import com.iattend.app.core.ui.hapticClick
import com.iattend.app.feature.devsupport.DevSupportTrigger

val LocalNavigationContentPadding = compositionLocalOf { 150.dp }

/** Give wide windows a stable navigation edge and return vertical space to their content. */
@Composable
fun AppNavigationRail(navController: NavHostController, destination: NavDestination?, modifier: Modifier = Modifier) {
    NavigationRail(modifier, containerColor = MaterialTheme.colorScheme.surfaceContainer, header = {
        if (destination?.hasRoute<SettingsRoute>() == true) {
            QuickAddFab(onClick = hapticClick { DevSupportTrigger.requestOpen.value = true }, lottieAsset = "lottie/heart.json", contentDescription = "Support the developer")
        } else ManageMenuFab(navController)
        Spacer(Modifier.height(16.dp))
    }) {
        bottomNavTabs.forEach { tab ->
            val selected = tab.isSelected(destination)
            NavigationRailItem(selected = selected, onClick = hapticClick { navController.navigateToTab(tab.route) },
                icon = { LottieMorphIcon(tab.lottieAsset, selected, if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, contentDescription = null) },
                label = { Text(tab.label) })
        }
    }
}
