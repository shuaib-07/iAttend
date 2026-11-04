package com.iattend.app.core.domain.occurrence

import com.iattend.app.core.data.db.Holiday
import com.iattend.app.core.data.db.OccurrenceSource
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.core.data.db.RecurringHolidayMode
import com.iattend.app.core.data.db.RecurringHolidayRule
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.data.db.TimetableSlot
import com.iattend.app.core.data.db.TimetableVersion
import com.iattend.app.feature.holidays.HolidayFormState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

class HolidayFlowTest {

    @Test
    fun `calendar date selection updates form date so saving does not target today`() {
        // Today is Nov 5, 2026
        val simulatedToday = LocalDate.of(2026, 11, 5)

        var selectedDate = simulatedToday
        var displayedMonth = YearMonth.from(simulatedToday)
        var form = HolidayFormState(
            date = simulatedToday,
            fromDate = simulatedToday,
            toDate = simulatedToday
        )

        // User clicks Nov 9 on the calendar
        val clickedDate = LocalDate.of(2026, 11, 9)
        selectedDate = clickedDate
        displayedMonth = YearMonth.from(clickedDate)
        form = form.copy(
            date = clickedDate,
            fromDate = clickedDate,
            toDate = if (form.isRange && !form.toDate.isBefore(clickedDate)) form.toDate else clickedDate
        )

        // Verify form has Nov 9
        assertEquals(clickedDate, form.date)
        assertEquals(clickedDate, form.fromDate)
        assertEquals(clickedDate, form.toDate)

        // User clicks Save ("Add Single Holiday")
        val start = if (form.isRange) form.fromDate else form.date
        val end = if (form.isRange) maxOf(form.fromDate, form.toDate) else form.date

        assertEquals(LocalDate.of(2026, 11, 9), start)
        assertEquals(LocalDate.of(2026, 11, 9), end)
    }

    @Test
    fun `range toggle properly synchronizes from and to dates`() {
        val nov9 = LocalDate.of(2026, 11, 9)
        var form = HolidayFormState(date = nov9, fromDate = nov9, toDate = nov9)

        // Toggle range ON
        val start = form.date
        val end = if (form.toDate.isBefore(start)) start else form.toDate
        form = form.copy(isRange = true, fromDate = start, toDate = end)

        assertEquals(nov9, form.fromDate)
        assertEquals(nov9, form.toDate)

        // Set toDate to Nov 12
        val nov12 = LocalDate.of(2026, 11, 12)
        form = form.copy(toDate = nov12)

        // Toggle range OFF
        form = form.copy(isRange = false, date = form.fromDate)
        assertEquals(nov9, form.date)
    }

    @Test
    fun `holiday on Nov 9 does not delete Nov 5 and Nov 6 occurrences`() {
        val subject = Subject(id = 1, code = "CS101", name = "Data Structures", colorArgb = 0xFF0000)
        val version = TimetableVersion(id = 1, effectiveFrom = LocalDate.of(2026, 8, 1))

        // Slots on Thursday (Nov 5) and Friday (Nov 6) and Monday (Nov 9)
        val slots = listOf(
            TimetableSlot(id = 1, timetableVersionId = 1, subjectId = 1, dayOfWeek = DayOfWeek.THURSDAY, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(10, 0)),
            TimetableSlot(id = 2, timetableVersionId = 1, subjectId = 1, dayOfWeek = DayOfWeek.FRIDAY, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(10, 0)),
            TimetableSlot(id = 3, timetableVersionId = 1, subjectId = 1, dayOfWeek = DayOfWeek.MONDAY, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(10, 0))
        )

        val saturdayRule = RecurringHolidayRule(dayOfWeek = DayOfWeek.SATURDAY, mode = RecurringHolidayMode.ALWAYS)
        val holidayNov9 = Holiday(id = 1, startDate = LocalDate.of(2026, 11, 9), endDate = LocalDate.of(2026, 11, 9), label = "Nov 9 Holiday")

        val generated = OccurrenceGenerator.generateOccurrences(
            dateRange = LocalDate.of(2026, 11, 5)..LocalDate.of(2026, 11, 10),
            versions = listOf(version),
            slotsByVersion = mapOf(1L to slots),
            holidays = listOf(holidayNov9),
            recurringRules = listOf(saturdayRule),
            subjectsById = mapOf(1L to subject)
        )

        // Nov 5 (Thursday) should be generated
        assertTrue("Nov 5 should have classes", generated.any { it.date == LocalDate.of(2026, 11, 5) })
        // Nov 6 (Friday) should be generated
        assertTrue("Nov 6 should have classes", generated.any { it.date == LocalDate.of(2026, 11, 6) })
        // Nov 9 (Monday holiday) should NOT be generated
        assertFalse("Nov 9 should NOT have classes", generated.any { it.date == LocalDate.of(2026, 11, 9) })
    }
}
