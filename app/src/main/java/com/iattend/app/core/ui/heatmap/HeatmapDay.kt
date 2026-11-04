package com.iattend.app.core.ui.heatmap

import java.time.LocalDate

data class HeatmapDay(
    val date: LocalDate,
    val intensity: Int
)
