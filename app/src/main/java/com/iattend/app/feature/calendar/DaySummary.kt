package com.iattend.app.feature.calendar

import com.iattend.app.core.data.db.ClassOccurrence
import com.iattend.app.core.data.db.OccurrenceStatus
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/** Used by the Home heatmap plus calendar-day attendance summaries. */
enum class DaySummary { NONE, PENDING, ALL_PRESENT, HAS_ABSENT }

/** Attendance rate for the selected month, excluding unmarked and cancelled classes. */
internal fun attendancePercentageForMonth(occurrences: List<ClassOccurrence>, month: YearMonth): Float? {
    val marked = occurrences.filter {
        YearMonth.from(it.date) == month &&
            (it.status == OccurrenceStatus.PRESENT || it.status == OccurrenceStatus.ABSENT)
    }
    val total = marked.sumOf { it.classCount }
    if (total == 0) return null
    val present = marked.filter { it.status == OccurrenceStatus.PRESENT }.sumOf { it.classCount }
    return present * 100f / total
}

internal fun isPastOccurrence(occurrence: ClassOccurrence, today: LocalDate, now: LocalTime): Boolean =
    occurrence.date.isBefore(today) ||
        (occurrence.date == today && (occurrence.endTime ?: occurrence.startTime)?.isBefore(now) == true)

internal fun attendanceStatusForDay(
    rows: List<ClassOccurrence>,
    today: LocalDate,
    now: LocalTime
): OccurrenceStatus? = when {
    rows.any { it.status == OccurrenceStatus.UNMARKED && isPastOccurrence(it, today, now) } -> OccurrenceStatus.UNMARKED
    rows.any { it.status == OccurrenceStatus.ABSENT } -> OccurrenceStatus.ABSENT
    rows.any { it.status == OccurrenceStatus.PRESENT } -> OccurrenceStatus.PRESENT
    rows.any { it.status == OccurrenceStatus.CANCELLED } -> OccurrenceStatus.CANCELLED
    else -> null
}
