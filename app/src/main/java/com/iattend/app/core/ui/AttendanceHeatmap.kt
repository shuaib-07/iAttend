package com.iattend.app.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.time.LocalDate

/**
 * Interactive GitHub-style Attendance Heatmap, adapted from the Sessions app design.
 * Supports day-level touch tooltips, month indicators, smooth auto-scroll, and intensity gradients.
 */
@Composable
fun AttendanceHeatmap(
    presentCountByDate: Map<LocalDate, Int>,
    modifier: Modifier = Modifier,
    filterStartDate: LocalDate? = null,
    filterEndDate: LocalDate? = null
) {
    com.iattend.app.core.ui.heatmap.AttendanceHeatmap(
        presentCountByDate = presentCountByDate,
        modifier = modifier,
        filterStartDate = filterStartDate,
        filterEndDate = filterEndDate
    )
}
