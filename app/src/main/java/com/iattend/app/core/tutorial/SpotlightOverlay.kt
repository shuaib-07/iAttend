package com.iattend.app.core.tutorial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.iattend.app.R
import com.iattend.app.core.navigation.CalendarRoute
import com.iattend.app.core.navigation.HomeRoute
import com.iattend.app.core.navigation.InsightsRoute
import com.iattend.app.core.navigation.SettingsRoute
import com.iattend.app.core.navigation.TimetableHubRoute
import com.iattend.app.core.navigation.navigateToTab

/** The bottom nav's own tabs - forcing these via plain navigate() (instead of the popUpTo/
 * saveState/restoreState dance [navigateToTab] uses) corrupts NavController's per-destination
 * saved-state map, so the bottom bar later restores/highlights the wrong tab until process death. */
private val TAB_ROUTES = setOf(HomeRoute, CalendarRoute(), TimetableHubRoute, InsightsRoute, SettingsRoute)

/** Drives the tour's navigation and renders the spotlight overlay - mounted once as a sibling of
 * NavHost (same layering as ModalSheet/UpdateAvailableModal in NavGraph.kt). */
@Composable
fun TutorialHost(navController: NavHostController) {
    val controller: TutorialController = hiltViewModel()
    val stepIndex by controller.stepIndex.collectAsState()
    val step = TUTORIAL_STEPS.getOrNull(stepIndex) ?: return
    val backStackEntry by navController.currentBackStackEntryAsState()

    // Keyed on stepIndex AND backStackEntry: fires once when the step starts, and again any time
    // the user drifts off the step's screen afterwards (e.g. the system back button) - a single
    // one-shot forced-navigate on stepIndex alone left the tour permanently stranded on whatever
    // screen the user backed into, since nothing re-checked after that first navigate landed.
    androidx.compose.runtime.LaunchedEffect(stepIndex, backStackEntry) {
        val dest = backStackEntry?.destination
        val completion = step.completion
        if (completion is TutorialCompletion.OnRoute && completion.matches(dest)) {
            controller.next()
            return@LaunchedEffect
        }
        val route = step.navigateTo
        if (route != null && !step.onTargetScreen(dest)) {
            // The target is almost always already on the back stack (e.g. TimetableHubRoute from
            // an earlier step) - popping back to it is simple and reliable. navigate() with
            // popUpTo+restoreState (navigateToTab) is a documented no-op in this exact situation
            // (target mid-stack, current entry on top): it silently leaves the back stack
            // untouched instead of restoring, which stranded the tour indefinitely. Only fall back
            // to a real navigate() for a route we've genuinely never visited.
            val popped = navController.popBackStack(route = route, inclusive = false)
            if (!popped) {
                if (route in TAB_ROUTES) navController.navigateToTab(route) else navController.navigate(route) { launchSingleTop = true }
            }
        }
    }

    SpotlightOverlay(controller = controller, step = step)
}

@Composable
private fun SpotlightOverlay(controller: TutorialController, step: TutorialStep) {
    // Read directly off the snapshot map (not the controller.targetBounds convenience getter,
    // which internally reads a StateFlow.value that Compose can't observe on its own) so this
    // recomposes both when the map is written AND when the step itself changes.
    val bounds = step.targetId.takeIf { it.isNotEmpty() }?.let { controller.targetBoundsById[it] }
    val density = LocalDensity.current

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenHeightPx = with(density) { maxHeight.toPx() }
        // Never let the tooltip sit on top of the real target (it'd both hide it and eat its
        // taps) - anchor to whichever half of the screen the target ISN'T in. Corner FABs get a
        // centered tooltip + doodle arrow instead - the target's "opposite half" is nearly the
        // whole screen away from a corner, leaving a huge, disconnected-looking gap.
        val tooltipAlignment = when {
            step.calloutArrow -> Alignment.Center
            bounds == null || bounds.center.y < screenHeightPx / 2f -> Alignment.BottomCenter
            else -> Alignment.TopCenter
        }

        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            drawRect(Color.Black.copy(alpha = 0.65f))
            if (bounds != null) {
                val pad = 8.dp.toPx()
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(bounds.left - pad, bounds.top - pad),
                    size = Size(bounds.width + pad * 2, bounds.height + pad * 2),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Fill,
                    blendMode = BlendMode.Clear
                )
            }
        }

        TooltipCard(
            step = step,
            controller = controller,
            modifier = Modifier
                .align(tooltipAlignment)
                .let {
                    when (tooltipAlignment) {
                        Alignment.BottomCenter -> it.navigationBarsPadding()
                        Alignment.TopCenter -> it.statusBarsPadding()
                        else -> it
                    }
                }
                .fillMaxWidth()
                .padding(16.dp)
        )

        if (step.calloutArrow && bounds != null) {
            // Icon's own doodle is drawn corner-to-corner (top-left tail to bottom-right head) -
            // anchor its bottom-right corner on the target so the head lands on the button.
            val arrowSize = 56.dp
            val arrowSizePx = with(density) { arrowSize.toPx() }
            Image(
                painter = painterResource(R.drawable.ic_curly_arrow),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .offset(
                        x = with(density) { (bounds.center.x - arrowSizePx).toDp() },
                        y = with(density) { (bounds.center.y - arrowSizePx).toDp() }
                    )
                    .size(arrowSize)
            )
        }
    }
}

@Composable
private fun TooltipCard(step: TutorialStep, controller: TutorialController, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Step ${step.id} of ${TUTORIAL_STEPS.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(step.title, style = MaterialTheme.typography.titleMedium)
            Text(step.body, style = MaterialTheme.typography.bodyMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = controller::skipAll) { Text("Skip tour") }
                when (step.completion) {
                    is TutorialCompletion.ManualNext -> Button(onClick = controller::next) { Text("Next") }
                    else -> Text(
                        "Waiting for you…",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
