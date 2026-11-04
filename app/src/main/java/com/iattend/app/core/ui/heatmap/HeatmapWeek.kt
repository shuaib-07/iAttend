package com.iattend.app.core.ui.heatmap

data class HeatmapWeek(
    val weekIndex: Int,
    val days: List<HeatmapDay>,
    val firstDayOfMonth: String?
)
