package com.iattend.app.feature.calendar

import com.iattend.app.core.data.db.ClassOccurrence
import com.iattend.app.core.data.db.OccurrenceSource
import com.iattend.app.core.data.db.OccurrenceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarMonthAttendanceTest {
    @Test
    fun `month percentage uses present and absent classes only`() {
        val september = YearMonth.of(2026, 9)
        val rows = listOf(
            occurrence(LocalDate.of(2026, 9, 1), OccurrenceStatus.PRESENT, classCount = 3),
            occurrence(LocalDate.of(2026, 9, 2), OccurrenceStatus.ABSENT),
            occurrence(LocalDate.of(2026, 9, 3), OccurrenceStatus.UNMARKED, classCount = 4),
            occurrence(LocalDate.of(2026, 9, 4), OccurrenceStatus.CANCELLED, classCount = 2),
            occurrence(LocalDate.of(2026, 10, 1), OccurrenceStatus.ABSENT, classCount = 8)
        )

        assertEquals(75f, attendancePercentageForMonth(rows, september)!!, 0.01f)
    }

    @Test
    fun `month percentage is absent when the month has no marked classes`() {
        val rows = listOf(occurrence(LocalDate.of(2026, 9, 1), OccurrenceStatus.UNMARKED))

        assertNull(attendancePercentageForMonth(rows, YearMonth.of(2026, 9)))
    }

    private fun occurrence(date: LocalDate, status: OccurrenceStatus, classCount: Int = 1) = ClassOccurrence(
        subjectId = 1,
        date = date,
        startTime = null,
        endTime = null,
        status = status,
        source = OccurrenceSource.SCHEDULED,
        timetableSlotId = 1,
        classCount = classCount
    )
}
