package com.iattend.app.core.domain.assessment

import com.iattend.app.core.data.db.Assessment
import com.iattend.app.core.data.db.AssessmentAttemptStatus
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.data.db.WeightageSystem

data class AssessmentScoreResult(
    val assessment: Assessment,
    /** Earned contribution for this assessment (e.g. 15.0). Null if ungraded or unweighted. */
    val contribution: Double?,
    /** Configured weight for this assessment, or null if unweighted. */
    val weight: Double?,
    /** True if this assessment is graded (scored or missed with configured weight). */
    val isGraded: Boolean
)

data class SubjectGradingSummary(
    val system: WeightageSystem?,
    val totalConfiguredWeight: Double,
    val gradedWeight: Double,
    val remainingUngradedWeight: Double,
    val earnedContribution: Double,
    /**
     * For PERCENTAGE: earnedContribution (already percentage points).
     * For WEIGHTED_MARKS: if totalWeightedMarks is set, earnedContribution / totalWeightedMarks * 100; else null.
     */
    val earnedPercentageOfSubjectTotal: Double?,
    /** Performance strictly on graded assessments: earnedContribution / gradedWeight * 100. Null if gradedWeight == 0. */
    val gradedPerformancePercentage: Double?,
    val exceedsMaxWeight: Boolean,
    val maxWeightAllowed: Double?
)

object AssessmentGradingCalculator {

    fun computeAssessmentScore(assessment: Assessment): AssessmentScoreResult {
        val weight = assessment.weight?.takeIf { it > 0.0 }
        if (weight == null) {
            return AssessmentScoreResult(
                assessment = assessment,
                contribution = null,
                weight = null,
                isGraded = false
            )
        }

        return when (assessment.attemptStatus) {
            AssessmentAttemptStatus.MISSED -> {
                AssessmentScoreResult(
                    assessment = assessment,
                    contribution = 0.0,
                    weight = weight,
                    isGraded = true
                )
            }
            AssessmentAttemptStatus.ATTEMPTED -> {
                val obtained = assessment.obtainedMarks
                val total = assessment.totalMarks
                if (obtained != null && total != null && total > 0.0) {
                    val clampedObtained = obtained.coerceIn(0.0, total)
                    val contribution = (clampedObtained / total) * weight
                    AssessmentScoreResult(
                        assessment = assessment,
                        contribution = contribution,
                        weight = weight,
                        isGraded = true
                    )
                } else {
                    AssessmentScoreResult(
                        assessment = assessment,
                        contribution = null,
                        weight = weight,
                        isGraded = false
                    )
                }
            }
            AssessmentAttemptStatus.PENDING -> {
                AssessmentScoreResult(
                    assessment = assessment,
                    contribution = null,
                    weight = weight,
                    isGraded = false
                )
            }
        }
    }

    fun computeSubjectSummary(subject: Subject, assessments: List<Assessment>): SubjectGradingSummary {
        val system = subject.gradingSystem
        val scoreResults = assessments.map { computeAssessmentScore(it) }

        val totalConfiguredWeight = scoreResults.mapNotNull { it.weight }.sum()
        val gradedResults = scoreResults.filter { it.isGraded }
        val gradedWeight = gradedResults.mapNotNull { it.weight }.sum()
        val remainingUngradedWeight = (totalConfiguredWeight - gradedWeight).coerceAtLeast(0.0)
        val earnedContribution = gradedResults.mapNotNull { it.contribution }.sum()

        val maxWeightAllowed: Double? = when (system) {
            WeightageSystem.PERCENTAGE -> 100.0
            WeightageSystem.WEIGHTED_MARKS -> subject.totalWeightedMarks
            null -> null
        }

        val exceedsMaxWeight = when (system) {
            WeightageSystem.PERCENTAGE -> totalConfiguredWeight > 100.0001
            WeightageSystem.WEIGHTED_MARKS -> maxWeightAllowed != null && totalConfiguredWeight > maxWeightAllowed + 0.0001
            null -> false
        }

        val earnedPercentageOfSubjectTotal: Double? = when (system) {
            WeightageSystem.PERCENTAGE -> earnedContribution
            WeightageSystem.WEIGHTED_MARKS -> {
                val total = subject.totalWeightedMarks
                if (total != null && total > 0.0) {
                    (earnedContribution / total) * 100.0
                } else null
            }
            null -> null
        }

        val gradedPerformancePercentage: Double? = if (gradedWeight > 0.0) {
            (earnedContribution / gradedWeight) * 100.0
        } else null

        return SubjectGradingSummary(
            system = system,
            totalConfiguredWeight = totalConfiguredWeight,
            gradedWeight = gradedWeight,
            remainingUngradedWeight = remainingUngradedWeight,
            earnedContribution = earnedContribution,
            earnedPercentageOfSubjectTotal = earnedPercentageOfSubjectTotal,
            gradedPerformancePercentage = gradedPerformancePercentage,
            exceedsMaxWeight = exceedsMaxWeight,
            maxWeightAllowed = maxWeightAllowed
        )
    }
}
