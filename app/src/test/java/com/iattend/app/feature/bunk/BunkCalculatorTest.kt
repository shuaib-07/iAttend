package com.iattend.app.feature.bunk

import com.iattend.app.core.data.db.Assessment
import com.iattend.app.core.data.db.AssessmentType
import com.iattend.app.core.data.db.ClassOccurrence
import com.iattend.app.core.data.db.Holiday
import com.iattend.app.core.data.db.OccurrenceSource
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.core.data.db.RecurringHolidayMode
import com.iattend.app.core.data.db.RecurringHolidayRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class BunkCalculatorTest {

    @Test
    fun `calculate accurate safe skips and projections when above target`() {
        // 9 attended out of 10 occurred, target = 75%
        // Safe skips: floor((900 - 750) / 75) = 2 skips
        val result = BunkCalculator.calculate(
            attended = 9,
            totalSoFar = 10,
            totalOverall = 20,
            targetPercentage = 75f
        )

        assertEquals(90.0f, result.currentPercentage, 0.01f)
        assertEquals(90.0f, result.projectedPercentage, 0.01f)
        assertEquals(2, result.safeSkipsNow)
        assertEquals(0, result.classesNeededForTarget)
        assertEquals(BunkStatus.COMFORTABLY_SAFE, result.status)
        // Total term misses allowed = 20 * (1 - 0.75) = 5. 1 already missed, so 4 remaining.
        assertEquals(4, result.endOfTermSkips)
    }

    @Test
    fun `calculate what-if simulation when bunking classes`() {
        // 9 attended out of 10 occurred. If we bunk 2 classes:
        // Projected attended = 9, projected total = 12 -> 9/12 = 75.0%
        val result = BunkCalculator.calculate(
            attended = 9,
            totalSoFar = 10,
            totalOverall = 20,
            targetPercentage = 75f,
            simulateMiss = 2
        )

        assertEquals(90.0f, result.currentPercentage, 0.01f)
        assertEquals(75.0f, result.projectedPercentage, 0.01f)
        assertEquals(2, result.safeSkipsNow)
    }

    @Test
    fun `calculate recovery classes needed when below target`() {
        // 6 attended out of 10 occurred (60%), target = 75%
        // Classes needed: ceil((750 - 600) / 25) = 6 classes
        val result = BunkCalculator.calculate(
            attended = 6,
            totalSoFar = 10,
            totalOverall = 20,
            targetPercentage = 75f
        )

        assertEquals(60.0f, result.currentPercentage, 0.01f)
        assertEquals(0, result.safeSkipsNow)
        assertEquals(6, result.classesNeededForTarget)
        assertEquals(BunkStatus.DANGER_ZONE, result.status)

        // If we attend 6 classes, projected: 12/16 = 75.0%
        val simulated = BunkCalculator.calculate(
            attended = 6,
            totalSoFar = 10,
            totalOverall = 20,
            targetPercentage = 75f,
            simulateAttend = 6
        )
        assertEquals(75.0f, simulated.projectedPercentage, 0.01f)
    }

    @Test
    fun `analyzeUpcomingDays identifies long weekend bridges and assessment blocks`() {
        val today = LocalDate.of(2026, 11, 6) // Friday
        val saturday = today.plusDays(1)
        val sunday = today.plusDays(2)
        val monday = today.plusDays(3)

        val occurrences = listOf(
            ClassOccurrence(
                id = 1,
                date = today,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(10, 0),
                subjectId = 1,
                timetableSlotId = null,
                status = OccurrenceStatus.UNMARKED,
                source = OccurrenceSource.SCHEDULED,
                classCount = 1
            ),
            ClassOccurrence(
                id = 2,
                date = monday,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(10, 0),
                subjectId = 1,
                timetableSlotId = null,
                status = OccurrenceStatus.UNMARKED,
                source = OccurrenceSource.SCHEDULED,
                classCount = 1
            )
        )

        val recurringRules = listOf(
            RecurringHolidayRule(DayOfWeek.SATURDAY, RecurringHolidayMode.ALWAYS),
            RecurringHolidayRule(DayOfWeek.SUNDAY, RecurringHolidayMode.ALWAYS)
        )

        val assessments = listOf(
            Assessment(
                id = 1,
                subjectId = 1,
                type = AssessmentType.EXAM,
                date = monday,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(12, 0),
                title = "Midterm Exam"
            )
        )

        val opportunities = BunkCalculator.analyzeUpcomingDays(
            futureOccurrences = occurrences,
            holidays = emptyList(),
            recurringHolidayRules = recurringRules,
            assessments = assessments,
            safeSkipsAvailable = 3,
            currentPercentage = 85f,
            targetPercentage = 75f,
            startDate = today
        )

        // Friday should be a recommended long weekend bridge
        val fridayOpp = opportunities.find { it.date == today }
        assertTrue(fridayOpp != null)
        assertTrue(fridayOpp!!.createsLongWeekend)
        assertEquals(BunkRecommendationType.RECOMMENDED, fridayOpp.recommendationType)

        // Monday has an assessment and must NOT be bunked
        val mondayOpp = opportunities.find { it.date == monday }
        assertTrue(mondayOpp != null)
        assertTrue(mondayOpp!!.hasAssessment)
        assertEquals(BunkRecommendationType.DO_NOT_BUNK, mondayOpp.recommendationType)
    }
}
