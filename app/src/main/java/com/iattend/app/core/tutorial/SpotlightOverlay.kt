package com.iattend.app.core.tutorial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

/** Drives the tour's navigation and renders the spotlight overlay - mounted once as a sibling of
 * NavHost (same layering as ModalSheet/UpdateAvailableModal in NavGraph.kt). */
@Composable
fun TutorialHost(navController: NavHostController) {
    val controller: TutorialController = hiltViewModel()
    val stepIndex by controller.stepIndex.collectAsState()
    val step = TUTORIAL_STEPS.getOrNull(stepIndex) ?: return
    val backStackEntry by navController.currentBackStackEntryAsState()

    androidx.compose.runtime.LaunchedEffect(stepIndex, step.navigateTo) {
        step.navigateTo?.let { route ->
            navController.navigate(route) { launchSingleTop = true }
        }
    }
    androidx.compose.runtime.LaunchedEffect(backStackEntry) {
        val completion = step.completion
        if (completion is TutorialCompletion.OnRoute && completion.matches(backStackEntry?.destination)) {
            controller.next()
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
        // taps) - anchor to whichever half of the screen the target ISN'T in.
        val tooltipAtBottom = bounds == null || bounds.center.y < screenHeightPx / 2f

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
                .align(if (tooltipAtBottom) Alignment.BottomCenter else Alignment.TopCenter)
                .let { if (tooltipAtBottom) it.navigationBarsPadding() else it.statusBarsPadding() }
                .fillMaxWidth()
                .padding(16.dp)
        )
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
