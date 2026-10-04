package com.iattend.app.core.domain.assessment

import com.iattend.app.core.data.db.Assessment
import com.iattend.app.core.data.db.AssessmentAttemptStatus
import com.iattend.app.core.data.db.AssessmentType
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.data.db.WeightageSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class AssessmentGradingCalculatorTest {

    private fun testAssessment(
        totalMarks: Double? = 40.0,
        obtainedMarks: Double? = 30.0,
        weight: Double? = 20.0,
        attemptStatus: AssessmentAttemptStatus = AssessmentAttemptStatus.ATTEMPTED
    ) = Assessment(
        id = 1,
        subjectId = 1,
        type = AssessmentType.TEST,
        date = LocalDate.now(),
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(10, 0),
        totalMarks = totalMarks,
        obtainedMarks = obtainedMarks,
        weight = weight,
        attemptStatus = attemptStatus
    )

    @Test
    fun `scored assessment computes proportional contribution`() {
        // 30/40 weighted to 20 marks contributes 15 weighted marks
        val a = testAssessment(totalMarks = 40.0, obtainedMarks = 30.0, weight = 20.0)
        val res = AssessmentGradingCalculator.computeAssessmentScore(a)
        assertTrue(res.isGraded)
        assertEquals(15.0, res.contribution!!, 0.0001)
        assertEquals(20.0, res.weight!!, 0.0001)
    }

    @Test
    fun `missed assessment contributes zero for its configured weight`() {
        val a = testAssessment(attemptStatus = AssessmentAttemptStatus.MISSED, weight = 15.0)
        val res = AssessmentGradingCalculator.computeAssessmentScore(a)
        assertTrue(res.isGraded)
        assertEquals(0.0, res.contribution!!, 0.0001)
        assertEquals(15.0, res.weight!!, 0.0001)
    }

    @Test
    fun `attempted assessment with unknown marks remains ungraded`() {
        val a = testAssessment(obtainedMarks = null, weight = 20.0)
        val res = AssessmentGradingCalculator.computeAssessmentScore(a)
        assertFalse(res.isGraded)
        assertNull(res.contribution)
        assertEquals(20.0, res.weight!!, 0.0001)
    }

    @Test
    fun `pending assessment remains ungraded`() {
        val a = testAssessment(attemptStatus = AssessmentAttemptStatus.PENDING, weight = 20.0)
        val res = AssessmentGradingCalculator.computeAssessmentScore(a)
        assertFalse(res.isGraded)
        assertNull(res.contribution)
    }

    @Test
    fun `unweighted assessment is excluded from calculations`() {
        val a = testAssessment(weight = null)
        val res = AssessmentGradingCalculator.computeAssessmentScore(a)
        assertFalse(res.isGraded)
        assertNull(res.contribution)
        assertNull(res.weight)
    }

    @Test
    fun `percentage system computes total, graded and remaining weights correctly`() {
        val subject = Subject(
            id = 1,
            code = "MATH",
            name = "Mathematics",
            colorArgb = 0,
            gradingSystem = WeightageSystem.PERCENTAGE
        )
        val a1 = testAssessment(totalMarks = 50.0, obtainedMarks = 40.0, weight = 25.0) // 20%
        val a2 = testAssessment(attemptStatus = AssessmentAttemptStatus.MISSED, weight = 15.0) // 0%
        val a3 = testAssessment(attemptStatus = AssessmentAttemptStatus.PENDING, weight = 30.0) // ungraded
        val a4 = testAssessment(weight = null) // unweighted

        val summary = AssessmentGradingCalculator.computeSubjectSummary(subject, listOf(a1, a2, a3, a4))
        assertEquals(70.0, summary.totalConfiguredWeight, 0.0001)
        assertEquals(40.0, summary.gradedWeight, 0.0001) // 25 + 15
        assertEquals(30.0, summary.remainingUngradedWeight, 0.0001)
        assertEquals(20.0, summary.earnedContribution, 0.0001) // 20 + 0
        assertEquals(20.0, summary.earnedPercentageOfSubjectTotal!!, 0.0001)
        assertEquals(50.0, summary.gradedPerformancePercentage!!, 0.0001) // 20 / 40 * 100
        assertFalse(summary.exceedsMaxWeight)
    }

    @Test
    fun `weighted marks system computes percentage when total weighted marks configured`() {
        val subject = Subject(
            id = 1,
            code = "PHY",
            name = "Physics",
            colorArgb = 0,
            gradingSystem = WeightageSystem.WEIGHTED_MARKS,
            totalWeightedMarks = 100.0
        )
        val a1 = testAssessment(totalMarks = 50.0, obtainedMarks = 50.0, weight = 20.0) // 20 marks
        val summary = AssessmentGradingCalculator.computeSubjectSummary(subject, listOf(a1))
        assertEquals(20.0, summary.earnedContribution, 0.0001)
        assertEquals(20.0, summary.earnedPercentageOfSubjectTotal!!, 0.0001)
        assertEquals(100.0, summary.gradedPerformancePercentage!!, 0.0001)
    }

    @Test
    fun `exceedsMaxWeight detects when combined weights exceed threshold`() {
        val subject = Subject(
            id = 1,
            code = "ENG",
            name = "English",
            colorArgb = 0,
            gradingSystem = WeightageSystem.PERCENTAGE
        )
        val a1 = testAssessment(weight = 60.0)
        val a2 = testAssessment(weight = 50.0)
        val summary = AssessmentGradingCalculator.computeSubjectSummary(subject, listOf(a1, a2))
        assertTrue(summary.exceedsMaxWeight)
    }
}
