package com.iattend.app.core.domain.occurrence

import com.iattend.app.core.data.db.ClassOccurrence
import com.iattend.app.core.data.db.Holiday
import com.iattend.app.core.data.db.OccurrenceSource
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.core.data.db.RecurringHolidayMode
import com.iattend.app.core.data.db.RecurringHolidayRule
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.data.db.TimetableSlot
import com.iattend.app.core.data.db.TimetableVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class OccurrenceGeneratorTest {

    // --- isHoliday ---

    @Test
    fun `date inside a holiday range is a holiday`() {
        val holidays = listOf(Holiday(startDate = LocalDate.of(2026, 12, 20), endDate = LocalDate.of(2027, 1, 2), label = "Winter Break"))
        assertTrue(OccurrenceGenerator.isHoliday(LocalDate.of(2026, 12, 25), holidays, emptyList()))
        assertFalse(OccurrenceGenerator.isHoliday(LocalDate.of(2026, 12, 19), holidays, emptyList()))
    }

    @Test
    fun `ALWAYS recurring rule marks every occurrence of that weekday as holiday`() {
        val rules = listOf(RecurringHolidayRule(DayOfWeek.SUNDAY, RecurringHolidayMode.ALWAYS))
        // Aug 2, 2026 is a Sunday
        assertTrue(OccurrenceGenerator.isHoliday(LocalDate.of(2026, 8, 2), emptyList(), rules))
        assertFalse(OccurrenceGenerator.isHoliday(LocalDate.of(2026, 8, 3), emptyList(), rules))
    }

    @Test
    fun `PATTERN rule only holidays the selected nth weekdays`() {
        // 2nd and 4th Saturday off (bits 1 and 3, 0-indexed)
        val weeksMask = (1 shl 1) or (1 shl 3)
        val rules = listOf(RecurringHolidayRule(DayOfWeek.SATURDAY, RecurringHolidayMode.PATTERN, weeksMask))
        // August 2026 Saturdays: 1st(1st), 8th(2nd), 15th(3rd), 22nd(4th), 29th(5th)
        assertFalse(OccurrenceGenerator.isHoliday(LocalDate.of(2026, 8, 1), emptyList(), rules))
        assertTrue(OccurrenceGenerator.isHoliday(LocalDate.of(2026, 8, 8), emptyList(), rules))
        assertFalse(OccurrenceGenerator.isHoliday(LocalDate.of(2026, 8, 15), emptyList(), rules))
        assertTrue(OccurrenceGenerator.isHoliday(LocalDate.of(2026, 8, 22), emptyList(), rules))
        assertFalse(OccurrenceGenerator.isHoliday(LocalDate.of(2026, 8, 29), emptyList(), rules))
    }

    @Test
    fun `NONE mode never holidays`() {
        val rules = listOf(RecurringHolidayRule(DayOfWeek.MONDAY, RecurringHolidayMode.NONE))
        assertFalse(OccurrenceGenerator.isHoliday(LocalDate.of(2026, 8, 3), emptyList(), rules))
    }

    // --- activeVersionFor ---

    @Test
    fun `activeVersionFor picks the latest version not after the date`() {
        val v1 = TimetableVersion(id = 1, effectiveFrom = LocalDate.of(2026, 8, 1))
        val v2 = TimetableVersion(id = 2, effectiveFrom = LocalDate.of(2026, 10, 15))
        val versions = listOf(v1, v2)

        assertEquals(v1, OccurrenceGenerator.activeVersionFor(LocalDate.of(2026, 9, 1), versions))
        assertEquals(v2, OccurrenceGenerator.activeVersionFor(LocalDate.of(2026, 10, 15), versions))
        assertEquals(v2, OccurrenceGenerator.activeVersionFor(LocalDate.of(2026, 12, 1), versions))
        assertNull(OccurrenceGenerator.activeVersionFor(LocalDate.of(2026, 7, 31), versions))
    }

    // --- generateOccurrences ---

    @Test
    fun `generates one occurrence per matching weekly slot, skips holidays, switches version on effective date`() {
        val physics = 100L
        val v1 = TimetableVersion(id = 1, effectiveFrom = LocalDate.of(2026, 8, 3)) // a Monday
        val v2 = TimetableVersion(id = 2, effectiveFrom = LocalDate.of(2026, 8, 17)) // two Mondays later

        val v1Slots = listOf(
            TimetableSlot(id = 1, timetableVersionId = 1, subjectId = physics, dayOfWeek = DayOfWeek.MONDAY, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(10, 0))
        )
        val v2Slots = listOf(
            // schedule moves to Tuesday from v2 onward
            TimetableSlot(id = 2, timetableVersionId = 2, subjectId = physics, dayOfWeek = DayOfWeek.TUESDAY, startTime = LocalTime.of(14, 0), endTime = LocalTime.of(15, 0))
        )

        // Aug 10 (Monday) is a one-off holiday
        val holidays = listOf(Holiday(startDate = LocalDate.of(2026, 8, 10), endDate = LocalDate.of(2026, 8, 10), label = "Local Holiday"))

        val occurrences = OccurrenceGenerator.generateOccurrences(
            dateRange = LocalDate.of(2026, 8, 3)..LocalDate.of(2026, 8, 18),
            versions = listOf(v1, v2),
            slotsByVersion = mapOf(1L to v1Slots, 2L to v2Slots),
            holidays = holidays,
            recurringRules = emptyList()
        )

        // Mondays in range under v1: Aug 3 (present), Aug 10 (holiday, skipped)
        // Tuesdays in range under v2 (from Aug 17): Aug 18
        assertEquals(2, occurrences.size)
        assertEquals(LocalDate.of(2026, 8, 3), occurrences[0].date)
        assertEquals(1L, occurrences[0].timetableSlotId)
        assertEquals(LocalDate.of(2026, 8, 18), occurrences[1].date)
        assertEquals(2L, occurrences[1].timetableSlotId)
        assertTrue(occurrences.all { it.status == OccurrenceStatus.UNMARKED && it.source == OccurrenceSource.SCHEDULED })
    }

    @Test
    fun `multiple slots of the same subject on the same day both generate`() {
        val chem = 200L
        val version = TimetableVersion(id = 1, effectiveFrom = LocalDate.of(2026, 8, 3))
        val slots = listOf(
            TimetableSlot(id = 1, timetableVersionId = 1, subjectId = chem, dayOfWeek = DayOfWeek.MONDAY, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(10, 0)),
            TimetableSlot(id = 2, timetableVersionId = 1, subjectId = chem, dayOfWeek = DayOfWeek.MONDAY, startTime = LocalTime.of(14, 0), endTime = LocalTime.of(16, 0), classCount = 2)
        )

        val occurrences = OccurrenceGenerator.generateOccurrences(
            dateRange = LocalDate.of(2026, 8, 3)..LocalDate.of(2026, 8, 3),
            versions = listOf(version),
            slotsByVersion = mapOf(1L to slots),
            holidays = emptyList(),
            recurringRules = emptyList()
        )

        assertEquals(2, occurrences.size)
        assertEquals(2, occurrences[1].classCount)
    }

    @Test
    fun `occurrences inherit subject room unless a slot explicitly overrides or clears it`() {
        val subjectId = 300L
        val version = TimetableVersion(id = 1, effectiveFrom = LocalDate.of(2026, 8, 3))
        val slots = listOf(
            TimetableSlot(id = 1, timetableVersionId = 1, subjectId = subjectId, dayOfWeek = DayOfWeek.MONDAY, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(10, 0)),
            TimetableSlot(id = 2, timetableVersionId = 1, subjectId = subjectId, dayOfWeek = DayOfWeek.MONDAY, startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 0), roomNumber = "B-204", roomNumberOverridden = true),
            TimetableSlot(id = 3, timetableVersionId = 1, subjectId = subjectId, dayOfWeek = DayOfWeek.MONDAY, startTime = LocalTime.of(11, 0), endTime = LocalTime.of(12, 0), roomNumberOverridden = true)
        )

        val occurrences = OccurrenceGenerator.generateOccurrences(
            dateRange = LocalDate.of(2026, 8, 3)..LocalDate.of(2026, 8, 3),
            versions = listOf(version),
            slotsByVersion = mapOf(1L to slots),
            holidays = emptyList(),
            recurringRules = emptyList(),
            subjectsById = mapOf(subjectId to Subject(code = "PHY", name = "Physics", colorArgb = 0, defaultRoomNumber = "A-101"))
        ).associateBy { it.timetableSlotId }

        assertEquals("A-101", occurrences.getValue(1L).roomNumber)
        assertFalse(occurrences.getValue(1L).roomNumberOverridden)
        assertEquals("B-204", occurrences.getValue(2L).roomNumber)
        assertTrue(occurrences.getValue(2L).roomNumberOverridden)
        assertNull(occurrences.getValue(3L).roomNumber)
        assertTrue(occurrences.getValue(3L).roomNumberOverridden)
    }

    @Test
    fun `no version active before any effectiveFrom produces no occurrences`() {
        val version = TimetableVersion(id = 1, effectiveFrom = LocalDate.of(2026, 9, 1))
        val slots = listOf(
            TimetableSlot(id = 1, timetableVersionId = 1, subjectId = 1L, dayOfWeek = DayOfWeek.MONDAY, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(10, 0))
        )

        val occurrences = OccurrenceGenerator.generateOccurrences(
            dateRange = LocalDate.of(2026, 8, 1)..LocalDate.of(2026, 8, 31),
            versions = listOf(version),
            slotsByVersion = mapOf(1L to slots),
            holidays = emptyList(),
            recurringRules = emptyList()
        )

        assertTrue(occurrences.isEmpty())
    }

    // --- applyBackdatedFill ---

    private fun occurrence(date: LocalDate, subjectId: Long = 1L) = ClassOccurrence(
        subjectId = subjectId,
        date = date,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(10, 0),
        source = OccurrenceSource.SCHEDULED,
        timetableSlotId = 1L
    )

    @Test
    fun `PresentAll marks every occurrence present`() {
        val input = listOf(occurrence(LocalDate.of(2026, 8, 1)), occurrence(LocalDate.of(2026, 8, 3)))
        val result = OccurrenceGenerator.applyBackdatedFill(input, BackdatedFillStrategy.PresentAll)
        assertTrue(result.all { it.status == OccurrenceStatus.PRESENT })
    }

    @Test
    fun `AbsentAll marks every occurrence absent`() {
        val input = listOf(occurrence(LocalDate.of(2026, 8, 1)))
        val result = OccurrenceGenerator.applyBackdatedFill(input, BackdatedFillStrategy.AbsentAll)
        assertTrue(result.all { it.status == OccurrenceStatus.ABSENT })
    }

    @Test
    fun `LeaveUnmarked leaves everything unmarked`() {
        val input = listOf(occurrence(LocalDate.of(2026, 8, 1)))
        val result = OccurrenceGenerator.applyBackdatedFill(input, BackdatedFillStrategy.LeaveUnmarked)
        assertTrue(result.all { it.status == OccurrenceStatus.UNMARKED })
    }

    @Test
    fun `AggregateBaseline marks the earliest N dates present chronologically regardless of input order`() {
        val input = listOf(
            occurrence(LocalDate.of(2026, 8, 5)),
            occurrence(LocalDate.of(2026, 8, 1)),
            occurrence(LocalDate.of(2026, 8, 3)),
            occurrence(LocalDate.of(2026, 8, 8))
        )
        val result = OccurrenceGenerator.applyBackdatedFill(input, BackdatedFillStrategy.AggregateBaseline(attended = 2))

        val byDate = result.associateBy { it.date }
        assertEquals(OccurrenceStatus.PRESENT, byDate.getValue(LocalDate.of(2026, 8, 1)).status)
        assertEquals(OccurrenceStatus.PRESENT, byDate.getValue(LocalDate.of(2026, 8, 3)).status)
        assertEquals(OccurrenceStatus.ABSENT, byDate.getValue(LocalDate.of(2026, 8, 5)).status)
        assertEquals(OccurrenceStatus.ABSENT, byDate.getValue(LocalDate.of(2026, 8, 8)).status)
    }

    @Test
    fun `AggregateBaseline attended count beyond list size is clamped, not an error`() {
        val input = listOf(occurrence(LocalDate.of(2026, 8, 1)))
        val result = OccurrenceGenerator.applyBackdatedFill(input, BackdatedFillStrategy.AggregateBaseline(attended = 999))
        assertEquals(OccurrenceStatus.PRESENT, result.single().status)
    }
}
