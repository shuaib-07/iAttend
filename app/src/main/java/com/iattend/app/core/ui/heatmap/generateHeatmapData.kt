package com.iattend.app.core.ui.heatmap

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

fun generateHeatmapData(
    activeDates: Map<LocalDate, Int>,
    customStartDate: LocalDate? = null,
    customEndDate: LocalDate? = null,
    weeksCount: Int = 52
): List<HeatmapWeek> {
    val today = LocalDate.now()
    val rawEndDate = customEndDate ?: (activeDates.keys.maxOrNull()?.let { maxOf(it, today) } ?: today)
    // Align to the end of the week (Sunday) so columns are full 7-day sets
    val daysUntilSunday = (7 - rawEndDate.dayOfWeek.value) % 7
    val endDate = rawEndDate.plusDays(daysUntilSunday.toLong())

    val startDate = customStartDate?.with(DayOfWeek.MONDAY)
        ?: endDate.minusWeeks(weeksCount.toLong()).with(DayOfWeek.MONDAY)

    val weeks = mutableListOf<HeatmapWeek>()
    val daysBetween = ChronoUnit.DAYS.between(startDate, endDate)
    if (daysBetween < 0) return emptyList()

    var currentWeekIndex = 0
    var currentWeekDays = mutableListOf<HeatmapDay>()

    for (i in 0..daysBetween) {
        val date = startDate.plusDays(i)
        val intensity = activeDates[date] ?: 0
        currentWeekDays.add(HeatmapDay(date = date, intensity = intensity))

        if (currentWeekDays.size == 7 || i == daysBetween) {
            val firstDayOfMonthLabel = currentWeekDays.firstOrNull { it.date.dayOfMonth == 1 }
                ?.date?.format(DateTimeFormatter.ofPattern("MMM"))

            weeks.add(
                HeatmapWeek(
                    weekIndex = currentWeekIndex,
                    days = ArrayList(currentWeekDays),
                    firstDayOfMonth = firstDayOfMonthLabel
                )
            )

            currentWeekIndex++
            currentWeekDays = mutableListOf()
        }
    }
    return weeks
}
