package com.iattend.app.feature.devsupport

import androidx.compose.runtime.mutableStateOf

/** Lets the bottom nav FAB (outside SettingsScreen) request opening the Developer Support sheet. */
object DevSupportTrigger {
    val requestOpen = mutableStateOf(false)
}
