package com.iattend.app.core.tutorial

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

private fun Rect.union(other: Rect) = Rect(
    left = minOf(left, other.left),
    top = minOf(top, other.top),
    right = maxOf(right, other.right),
    bottom = maxOf(bottom, other.bottom)
)

/** Tags a real widget as (part of) the current step's spotlight target - no-op when the tutorial
 * isn't active, so it's safe to leave on production widgets. When more than one widget shares the
 * same [id] (e.g. every row in a list, all tagged "mark today's classes"), their bounds are
 * unioned into one enclosing cutout rather than the last one winning - lets a single step spotlight
 * a whole group of cards instead of just one. */
fun Modifier.tutorialTarget(id: String): Modifier = composed {
    val controller = LocalTutorialController.current ?: return@composed this
    onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInRoot()
        controller.targetBoundsById[id] = controller.targetBoundsById[id]?.union(bounds) ?: bounds
    }
}
