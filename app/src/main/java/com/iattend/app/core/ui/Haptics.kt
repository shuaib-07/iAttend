package com.iattend.app.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** Wraps [onClick] with a light haptic tick, for primary actions (Save, FAB, nav taps). */
@Composable
fun hapticClick(onClick: () -> Unit): () -> Unit {
    val haptics = LocalHapticFeedback.current
    return {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onClick()
    }
}
