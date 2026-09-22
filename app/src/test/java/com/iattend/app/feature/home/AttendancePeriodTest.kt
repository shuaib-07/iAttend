package com.iattend.app.feature.home

import com.iattend.app.core.data.db.ClassOccurrence
import com.iattend.app.core.data.db.OccurrenceSource
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.feature.calendar.attendanceStatusForDay
import com.iattend.app.feature.calendar.isPastOccurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class AttendancePeriodTest {
    private val today = LocalDate.of(2026, 9, 23)

    @Test
    fun `configured boundaries are honored and a lone start runs through today`() {
        assertEquals(
            AttendancePeriod(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 10, 1), "Tracking period", null),
            attendancePeriod(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 10, 1), today)
        )
        assertEquals(
            AttendancePeriod(LocalDate.of(2026, 8, 1), today, "Since start date", null),
            attendancePeriod(LocalDate.of(2026, 8, 1), null, today)
        )
    }

    @Test
    fun `missing tracking boundaries use a noticed 120 day fallback`() {
        assertEquals(LocalDate.of(2026, 5, 27), attendancePeriod(null, null, today).start)
        val endOnly = attendancePeriod(null, LocalDate.of(2026, 10, 1), today)
        assertEquals(LocalDate.of(2026, 6, 4), endOnly.start)
        assertEquals(LocalDate.of(2026, 10, 1), endOnly.end)
        assertTrue(endOnly.notice!!.contains("Tracking start"))
        assertTrue(attendancePeriod(null, null, today).notice!!.contains("120 days"))
    }

    @Test
    fun `only completed unmarked classes are past and actionable`() {
        val completed = occurrence(today, LocalTime.of(9, 0), LocalTime.of(10, 0))
        val upcoming = occurrence(today, LocalTime.of(11, 0), LocalTime.of(12, 0))
        assertTrue(isPastOccurrence(completed, today, LocalTime.of(10, 1)))
        assertFalse(isPastOccurrence(upcoming, today, LocalTime.of(10, 1)))
        assertTrue(isPastOccurrence(occurrence(today.minusDays(1), null, null), today, LocalTime.NOON))
    }

    @Test
    fun `date color prioritizes unmarked then absent then present then cancelled`() {
        val present = occurrence(today.minusDays(1), null, null, OccurrenceStatus.PRESENT)
        val absent = occurrence(today.minusDays(1), null, null, OccurrenceStatus.ABSENT)
        val cancelled = occurrence(today.minusDays(1), null, null, OccurrenceStatus.CANCELLED)
        val unmarked = occurrence(today.minusDays(1), null, null)
        assertEquals(OccurrenceStatus.UNMARKED, attendanceStatusForDay(listOf(present, absent, unmarked), today, LocalTime.NOON))
        assertEquals(OccurrenceStatus.ABSENT, attendanceStatusForDay(listOf(present, absent, cancelled), today, LocalTime.NOON))
        assertEquals(OccurrenceStatus.PRESENT, attendanceStatusForDay(listOf(present, cancelled), today, LocalTime.NOON))
        assertEquals(OccurrenceStatus.CANCELLED, attendanceStatusForDay(listOf(cancelled), today, LocalTime.NOON))
        assertEquals(null, attendanceStatusForDay(listOf(occurrence(today, LocalTime.NOON, LocalTime.of(13, 0))), today, LocalTime.NOON))
    }

    @Test
    fun `period totals include marked classes and only past unmarked classes`() {
        val period = AttendancePeriod(today.minusDays(5), today, "Tracking period", null)
        val rows = listOf(
            occurrence(today.minusDays(1), null, null, OccurrenceStatus.PRESENT).copy(classCount = 2),
            occurrence(today.minusDays(1), null, null, OccurrenceStatus.ABSENT),
            occurrence(today.minusDays(1), null, null, OccurrenceStatus.CANCELLED).copy(classCount = 4),
            occurrence(today.minusDays(1), null, null).copy(classCount = 3),
            occurrence(today, LocalTime.of(14, 0), LocalTime.of(15, 0)).copy(classCount = 5),
            occurrence(today.minusDays(8), null, null, OccurrenceStatus.PRESENT)
        )

        val summary = summarizePeriod(rows, period, today, LocalTime.NOON)
        assertEquals(3, summary.occurred)
        assertEquals(2, summary.present)
        assertEquals(1, summary.absent)
        assertEquals(3, summary.unmarked)
        assertEquals(1, summary.pastUnmarkedClasses.size)
    }

    private fun occurrence(
        date: LocalDate,
        start: LocalTime?,
        end: LocalTime?,
        status: OccurrenceStatus = OccurrenceStatus.UNMARKED
    ) = ClassOccurrence(
        subjectId = 1,
        date = date,
        startTime = start,
        endTime = end,
        status = status,
        source = OccurrenceSource.SCHEDULED,
        timetableSlotId = 1
    )
}
