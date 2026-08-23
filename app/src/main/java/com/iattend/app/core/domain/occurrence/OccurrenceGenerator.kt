package com.iattend.app.core.domain.occurrence

import com.iattend.app.core.data.db.ClassOccurrence
import com.iattend.app.core.data.db.Holiday
import com.iattend.app.core.data.db.OccurrenceSource
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.core.data.db.RecurringHolidayMode
import com.iattend.app.core.data.db.RecurringHolidayRule
import com.iattend.app.core.data.db.TimetableSlot
import com.iattend.app.core.data.db.TimetableVersion
import java.time.LocalDate

/** Backdated-fill strategies for a subject's occurrences in a past date range (Product Plan §3.5). */
sealed class BackdatedFillStrategy {
    data object PresentAll : BackdatedFillStrategy()
    data object AbsentAll : BackdatedFillStrategy()
    data object LeaveUnmarked : BackdatedFillStrategy()
    data class AggregateBaseline(val attended: Int) : BackdatedFillStrategy()
}

/**
 * Pure domain logic for turning a timetable + holiday calendar into concrete dated
 * class occurrences. No Room/Android dependency, so this is fully unit-testable on the JVM.
 */
object OccurrenceGenerator {

    /** Which occurrence of [date]'s day-of-week within its month this is (1st, 2nd, ... 5th). */
    fun nthWeekdayOfMonth(date: LocalDate): Int = (date.dayOfMonth - 1) / 7 + 1

    fun isHoliday(
        date: LocalDate,
        holidays: List<Holiday>,
        recurringRules: List<RecurringHolidayRule>
    ): Boolean {
        if (holidays.any { date in it.startDate..it.endDate }) return true

        val rule = recurringRules.firstOrNull { it.dayOfWeek == date.dayOfWeek } ?: return false
        return when (rule.mode) {
            RecurringHolidayMode.NONE -> false
            RecurringHolidayMode.ALWAYS -> true
            RecurringHolidayMode.PATTERN -> {
                val bit = 1 shl (nthWeekdayOfMonth(date) - 1)
                rule.weeksOfMonth and bit != 0
            }
        }
    }

    /** The timetable version in effect on [date]: the latest version whose effectiveFrom <= date. */
    fun activeVersionFor(date: LocalDate, versions: List<TimetableVersion>): TimetableVersion? =
        versions.filter { !it.effectiveFrom.isAfter(date) }.maxByOrNull { it.effectiveFrom }

    /**
     * Generates SCHEDULED, UNMARKED occurrences for every date in [dateRange] that isn't a
     * holiday, using whichever timetable version is active on each date. Does not touch the
     * database — callers own persisting the result and de-duplicating against what already exists.
     */
    fun generateOccurrences(
        dateRange: ClosedRange<LocalDate>,
        versions: List<TimetableVersion>,
        slotsByVersion: Map<Long, List<TimetableSlot>>,
        holidays: List<Holiday>,
        recurringRules: List<RecurringHolidayRule>
    ): List<ClassOccurrence> {
        if (dateRange.isEmpty()) return emptyList()

        val result = mutableListOf<ClassOccurrence>()
        var date = dateRange.start
        while (!date.isAfter(dateRange.endInclusive)) {
            if (!isHoliday(date, holidays, recurringRules)) {
                val version = activeVersionFor(date, versions)
                val slots = version?.let { slotsByVersion[it.id] }.orEmpty()
                for (slot in slots) {
                    if (slot.dayOfWeek == date.dayOfWeek) {
                        result += ClassOccurrence(
                            subjectId = slot.subjectId,
                            date = date,
                            startTime = slot.startTime,
                            endTime = slot.endTime,
                            status = OccurrenceStatus.UNMARKED,
                            source = OccurrenceSource.SCHEDULED,
                            timetableSlotId = slot.id,
                            classCount = slot.classCount,
                            roomNumber = slot.roomNumber,
                            classType = slot.classType
                        )
                    }
                }
            }
            date = date.plusDays(1)
        }
        return result
    }

    /**
     * Applies a backdated-fill strategy to a single subject's occurrences (already generated,
     * all UNMARKED, sorted by date ascending). AggregateBaseline distributes the given attended
     * count chronologically: the earliest N occurrences become PRESENT, the rest ABSENT.
     */
    fun applyBackdatedFill(
        occurrences: List<ClassOccurrence>,
        strategy: BackdatedFillStrategy
    ): List<ClassOccurrence> {
        val sorted = occurrences.sortedBy { it.date }
        return when (strategy) {
            is BackdatedFillStrategy.LeaveUnmarked -> sorted
            is BackdatedFillStrategy.PresentAll -> sorted.map { it.copy(status = OccurrenceStatus.PRESENT) }
            is BackdatedFillStrategy.AbsentAll -> sorted.map { it.copy(status = OccurrenceStatus.ABSENT) }
            is BackdatedFillStrategy.AggregateBaseline -> {
                val presentCount = strategy.attended.coerceIn(0, sorted.size)
                sorted.mapIndexed { index, occurrence ->
                    occurrence.copy(
                        status = if (index < presentCount) OccurrenceStatus.PRESENT else OccurrenceStatus.ABSENT
                    )
                }
            }
        }
    }
}
