package com.iattend.app.core.domain.stats

import com.iattend.app.core.data.db.ClassOccurrence
import com.iattend.app.core.data.db.OccurrenceSource
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.data.db.TotalMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class AttendanceStatsCalculatorTest {

    private fun occurrence(date: LocalDate, status: OccurrenceStatus, classCount: Int = 1) = ClassOccurrence(
        subjectId = 1,
        date = date,
        startTime = null,
        endTime = null,
        status = status,
        source = OccurrenceSource.SCHEDULED,
        timetableSlotId = null,
        classCount = classCount
    )

    @Test
    fun `KNOWN mode computes needed and can-skip against the entered total`() {
        val subject = Subject(id = 1, code = "PHY", name = "Physics", colorArgb = 0, totalMode = TotalMode.KNOWN, knownTotalClasses = 40)
        val day = LocalDate.of(2026, 1, 1)
        val occurrences = (1..28).map { occurrence(day.plusDays(it.toLong()), OccurrenceStatus.PRESENT) } +
            (1..7).map { occurrence(day.plusDays(100L + it), OccurrenceStatus.ABSENT) }

        val stats = AttendanceStatsCalculator.compute(subject, occurrences, requiredPercentage = 75f, trackingEndDate = null)

        assertEquals(28, stats.attended)
        assertEquals(35, stats.totalSoFar)
        assertEquals(80f, stats.percentage, 0.01f)
        assertEquals(40, stats.totalOverall)
        assertEquals(5, stats.remaining)
        assertEquals(2, stats.classesNeeded)
        assertEquals(3, stats.classesCanSkip)
    }

    @Test
    fun `exactly hitting the threshold counts as meeting it (inclusive)`() {
        val subject = Subject(id = 1, code = "X", name = "X", colorArgb = 0, totalMode = TotalMode.KNOWN, knownTotalClasses = 4)
        val day = LocalDate.of(2026, 1, 1)
        // 3 attended, 1 remaining -> need exactly 3/4 = 75% which already meets a 75% requirement.
        val occurrences = listOf(
            occurrence(day, OccurrenceStatus.PRESENT),
            occurrence(day.plusDays(1), OccurrenceStatus.PRESENT),
            occurrence(day.plusDays(2), OccurrenceStatus.PRESENT)
        )

        val stats = AttendanceStatsCalculator.compute(subject, occurrences, requiredPercentage = 75f, trackingEndDate = null)

        assertEquals(0, stats.classesNeeded)
        assertEquals(1, stats.classesCanSkip)
    }

    @Test
    fun `per-subject end date override wins over the global setting and bounds the total`() {
        val subject = Subject(
            id = 1, code = "X", name = "X", colorArgb = 0, totalMode = TotalMode.COMPUTED,
            trackingEndDateOverride = LocalDate.of(2026, 2, 1)
        )
        // One occurrence within the subject's own end date, one past it (but still within the global end date).
        val occurrences = listOf(
            occurrence(LocalDate.of(2026, 1, 15), OccurrenceStatus.PRESENT),
            occurrence(LocalDate.of(2026, 3, 1), OccurrenceStatus.PRESENT)
        )

        val stats = AttendanceStatsCalculator.compute(
            subject, occurrences, requiredPercentage = 75f, trackingEndDate = LocalDate.of(2026, 5, 1)
        )

        assertEquals(1, stats.attended)
        assertEquals(1, stats.totalOverall)
    }

    @Test
    fun `OPEN_ENDED subjects report no projections`() {
        val subject = Subject(id = 1, code = "X", name = "X", colorArgb = 0, totalMode = TotalMode.OPEN_ENDED)
        val occurrences = listOf(occurrence(LocalDate.of(2026, 1, 1), OccurrenceStatus.PRESENT))

        val stats = AttendanceStatsCalculator.compute(subject, occurrences, requiredPercentage = 75f, trackingEndDate = null)

        assertEquals(1, stats.attended)
        assertNull(stats.totalOverall)
        assertNull(stats.classesNeeded)
        assertNull(stats.classesCanSkip)
    }

    @Test
    fun `COMPUTED mode is N-A until a tracking end date exists`() {
        val subject = Subject(id = 1, code = "X", name = "X", colorArgb = 0, totalMode = TotalMode.COMPUTED)
        val occurrences = listOf(occurrence(LocalDate.of(2026, 1, 1), OccurrenceStatus.PRESENT))

        val withoutEndDate = AttendanceStatsCalculator.compute(subject, occurrences, requiredPercentage = 75f, trackingEndDate = null)
        assertNull(withoutEndDate.totalOverall)

        val withEndDate = AttendanceStatsCalculator.compute(subject, occurrences, requiredPercentage = 75f, trackingEndDate = LocalDate.of(2026, 5, 1))
        assertEquals(1, withEndDate.totalOverall)
    }

    @Test
    fun `cancelled occurrences are excluded from every count`() {
        val subject = Subject(id = 1, code = "X", name = "X", colorArgb = 0, totalMode = TotalMode.COMPUTED)
        val occurrences = listOf(
            occurrence(LocalDate.of(2026, 1, 1), OccurrenceStatus.PRESENT),
            occurrence(LocalDate.of(2026, 1, 2), OccurrenceStatus.CANCELLED)
        )

        val stats = AttendanceStatsCalculator.compute(subject, occurrences, requiredPercentage = 75f, trackingEndDate = LocalDate.of(2026, 5, 1))

        assertEquals(1, stats.attended)
        assertEquals(1, stats.totalOverall)
    }

    @Test
    fun `needed clamps to remaining when the threshold is unreachable`() {
        val subject = Subject(id = 1, code = "X", name = "X", colorArgb = 0, totalMode = TotalMode.KNOWN, knownTotalClasses = 10)
        val day = LocalDate.of(2026, 1, 1)
        val occurrences = (1..8).map { occurrence(day.plusDays(it.toLong()), OccurrenceStatus.ABSENT) }

        val stats = AttendanceStatsCalculator.compute(subject, occurrences, requiredPercentage = 75f, trackingEndDate = null)

        assertEquals(2, stats.remaining)
        assertEquals(2, stats.classesNeeded)
        assertEquals(0, stats.classesCanSkip)
    }
}
