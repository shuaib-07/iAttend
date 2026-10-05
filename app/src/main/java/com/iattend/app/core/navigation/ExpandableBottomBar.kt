package com.iattend.app.core.navigation

import android.os.Build
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import com.iattend.app.core.ui.LottieMorphIcon
import com.iattend.app.core.ui.hapticClick
import com.iattend.app.feature.devsupport.DevSupportTrigger

/** Bottom-anchored toolbar that grows around the selected tab's label. */
@Composable
fun ExpandableBottomBar(navController: NavHostController, destination: NavDestination?, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelLarge
    val colors = MaterialTheme.colorScheme
    val easing = CubicBezierEasing(0.08f, 0.82f, 0.17f, 1f)
    BoxWithConstraints(modifier.navigationBarsPadding().padding(horizontal = 8.dp, vertical = 12.dp)) {
        // Reserve full touch targets and the separate FAB before allocating label space.
        val labelBudget = (maxWidth - 56.dp - 8.dp - 8.dp - 48.dp * bottomNavTabs.size).coerceAtLeast(0.dp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Row(
                    Modifier.clip(RoundedCornerShape(16.dp)).background(colors.surfaceContainer)
                        .padding(4.dp).selectableGroup(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bottomNavTabs.forEach { tab ->
                        val selected = tab.isSelected(destination)
                        val measuredWidth = with(density) { measurer.measure(tab.label, labelStyle).size.width.toDp() } + 10.dp
                        val labelWidth by animateDpAsState(
                            if (selected) minOf(measuredWidth, labelBudget) else 0.dp,
                            tween(300, easing = easing), label = "expandableLabelWidth"
                        )
                        val reveal by animateFloatAsState(if (selected) 1f else 0f, tween(200, easing = easing), label = "expandableLabelReveal")
                        val tint = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant
                        Row(
                            Modifier.width(48.dp + labelWidth).height(48.dp).clip(RoundedCornerShape(12.dp))
                                .background(if (selected) colors.primaryContainer else androidx.compose.ui.graphics.Color.Transparent)
                                .selectable(selected, role = Role.Tab, onClick = hapticClick { navController.navigateToTab(tab.route) })
                                .semantics(mergeDescendants = true) { contentDescription = tab.label }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LottieMorphIcon(tab.lottieAsset, selected, tint, size = 24.dp, contentDescription = null)
                            Box(Modifier.width(labelWidth).clip(RoundedCornerShape(0.dp))) {
                                Text(
                                    tab.label, color = tint, style = labelStyle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(start = 8.dp).graphicsLayer {
                                        alpha = reveal
                                        translationY = -8.dp.toPx() * (1f - reveal)
                                        if (Build.VERSION.SDK_INT >= 31) renderEffect = if (reveal < 1f) BlurEffect(8f * (1f - reveal), 8f * (1f - reveal)) else null
                                    }
                                )
                            }
                        }
                    }
                }
            }
            if (destination?.hasRoute<SettingsRoute>() == true) {
                QuickAddFab(onClick = hapticClick { DevSupportTrigger.requestOpen.value = true }, lottieAsset = "lottie/heart.json", contentDescription = "Support the developer")
            } else {
                ManageMenuFab(navController)
            }
        }
    }
}
