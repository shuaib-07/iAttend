package com.iattend.app.core.domain.stats

import com.iattend.app.core.data.db.ClassOccurrence
import com.iattend.app.core.data.db.OccurrenceSource
import com.iattend.app.core.data.db.OccurrenceStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class StreakCalculatorTest {

    private fun occ(
        date: LocalDate,
        time: LocalTime,
        status: OccurrenceStatus,
        classCount: Int = 1
    ): ClassOccurrence {
        return ClassOccurrence(
            id = 0,
            subjectId = 1,
            date = date,
            startTime = time,
            endTime = time.plusHours(1),
            status = status,
            source = OccurrenceSource.SCHEDULED,
            timetableSlotId = null,
            classCount = classCount
        )
    }

    @Test
    fun `empty occurrences returns zero streaks`() {
        val result = StreakCalculator.compute(emptyList())
        assertEquals(0, result.currentStreak)
        assertEquals(0, result.bestStreak)
    }

    @Test
    fun `all present increments current and best streaks`() {
        val baseDate = LocalDate.of(2026, 11, 1)
        val occurrences = listOf(
            occ(baseDate, LocalTime.of(9, 0), OccurrenceStatus.PRESENT),
            occ(baseDate, LocalTime.of(10, 0), OccurrenceStatus.PRESENT),
            occ(baseDate.plusDays(1), LocalTime.of(9, 0), OccurrenceStatus.PRESENT)
        )
        val result = StreakCalculator.compute(occurrences)
        assertEquals(3, result.currentStreak)
        assertEquals(3, result.bestStreak)
    }

    @Test
    fun `multi-hour classes count towards streak`() {
        val baseDate = LocalDate.of(2026, 11, 1)
        val occurrences = listOf(
            occ(baseDate, LocalTime.of(9, 0), OccurrenceStatus.PRESENT, classCount = 2),
            occ(baseDate.plusDays(1), LocalTime.of(9, 0), OccurrenceStatus.PRESENT, classCount = 3)
        )
        val result = StreakCalculator.compute(occurrences)
        assertEquals(5, result.currentStreak)
        assertEquals(5, result.bestStreak)
    }

    @Test
    fun `absence resets current streak but preserves best streak`() {
        val baseDate = LocalDate.of(2026, 11, 1)
        val occurrences = listOf(
            occ(baseDate, LocalTime.of(9, 0), OccurrenceStatus.PRESENT),
            occ(baseDate, LocalTime.of(10, 0), OccurrenceStatus.PRESENT),
            occ(baseDate.plusDays(1), LocalTime.of(9, 0), OccurrenceStatus.ABSENT),
            occ(baseDate.plusDays(2), LocalTime.of(9, 0), OccurrenceStatus.PRESENT)
        )
        val result = StreakCalculator.compute(occurrences)
        assertEquals(1, result.currentStreak)
        assertEquals(2, result.bestStreak)
    }

    @Test
    fun `unmarked and cancelled occurrences do not break streak`() {
        val baseDate = LocalDate.of(2026, 11, 1)
        val occurrences = listOf(
            occ(baseDate, LocalTime.of(9, 0), OccurrenceStatus.PRESENT),
            occ(baseDate.plusDays(1), LocalTime.of(9, 0), OccurrenceStatus.CANCELLED),
            occ(baseDate.plusDays(2), LocalTime.of(9, 0), OccurrenceStatus.UNMARKED),
            occ(baseDate.plusDays(3), LocalTime.of(9, 0), OccurrenceStatus.PRESENT)
        )
        val result = StreakCalculator.compute(occurrences)
        assertEquals(2, result.currentStreak)
        assertEquals(2, result.bestStreak)
    }

    @Test
    fun `occurrences are sorted chronologically by date and start time`() {
        val baseDate = LocalDate.of(2026, 11, 1)
        // Passed in reverse order
        val occurrences = listOf(
            occ(baseDate.plusDays(2), LocalTime.of(9, 0), OccurrenceStatus.PRESENT),
            occ(baseDate, LocalTime.of(11, 0), OccurrenceStatus.ABSENT),
            occ(baseDate, LocalTime.of(9, 0), OccurrenceStatus.PRESENT)
        )
        val result = StreakCalculator.compute(occurrences)
        assertEquals(1, result.currentStreak)
        assertEquals(1, result.bestStreak)
    }
}
