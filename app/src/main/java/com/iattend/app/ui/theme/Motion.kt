package com.iattend.app.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/** Shared motion vocabulary so every spring/tween in the app feels like one system. */
object Motion {
    /** Playful, physical feel: taps, chip selection, dialog entrance. */
    fun <T> bouncy(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** Quick, minimal overshoot: press feedback, color transitions. */
    fun <T> snappy(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Slow, settled: progress rings, large content swaps. */
    fun <T> gentle(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    fun <T> shortTween(): FiniteAnimationSpec<T> = tween(durationMillis = 200)
    fun <T> mediumTween(): FiniteAnimationSpec<T> = tween(durationMillis = 350)
}
