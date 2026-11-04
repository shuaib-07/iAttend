package com.iattend.app.feature.bunk

import com.iattend.app.core.data.db.Assessment
import com.iattend.app.core.data.db.ClassOccurrence
import com.iattend.app.core.data.db.Holiday
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.core.data.db.RecurringHolidayMode
import com.iattend.app.core.data.db.RecurringHolidayRule
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.floor

enum class BunkStatus {
    COMFORTABLY_SAFE,
    CAUTION_BORDERLINE,
    DANGER_ZONE
}

enum class BunkRecommendationType {
    RECOMMENDED,
    OPTIONAL_SAFE,
    HIGH_IMPACT,
    DO_NOT_BUNK
}

data class BunkCalculationResult(
    val currentPercentage: Float,
    val projectedPercentage: Float,
    val safeSkipsNow: Int,
    val classesNeededForTarget: Int,
    val endOfTermSkips: Int?,
    val targetPercentage: Float,
    val status: BunkStatus
)

data class DayBunkOpportunity(
    val date: LocalDate,
    val classCount: Int,
    val subjectIds: List<Long>,
    val hasAssessment: Boolean,
    val assessmentTitle: String?,
    val createsLongWeekend: Boolean,
    val isBridgeHoliday: Boolean,
    val recommendationType: BunkRecommendationType,
    val recommendationReason: String
)

object BunkCalculator {

    private const val EPSILON = 1e-5f

    fun calculate(
        attended: Int,
        totalSoFar: Int,
        totalOverall: Int?,
        targetPercentage: Float,
        simulateMiss: Int = 0,
        simulateAttend: Int = 0
    ): BunkCalculationResult {
        val currentPct = if (totalSoFar == 0) 100f else (attended * 100f / totalSoFar)

        val projAttended = attended + simulateAttend
        val projTotal = totalSoFar + simulateAttend + simulateMiss
        val projPct = if (projTotal == 0) 100f else (projAttended * 100f / projTotal)

        val target = targetPercentage.coerceIn(1f, 99f)

        // Consecutive safe skips from today: attended / (totalSoFar + k) >= target / 100
        val safeSkipsNow = if (currentPct < target) {
            0
        } else {
            val numerator = attended * 100f - target * totalSoFar
            floor((numerator / target) + EPSILON).toInt().coerceAtLeast(0)
        }

        // Consecutive classes needed to climb back: (attended + x) / (totalSoFar + x) >= target / 100
        val classesNeeded = if (currentPct >= target) {
            0
        } else {
            val numerator = target * totalSoFar - attended * 100f
            val denominator = 100f - target
            ceil((numerator / denominator) - EPSILON).toInt().coerceAtLeast(1)
        }

        // End-of-term skips if total overall classes are known
        val endOfTermSkips = if (totalOverall != null && totalOverall >= totalSoFar) {
            val remaining = totalOverall - totalSoFar
            val requiredAttendedTotal = ceil((target / 100f) * totalOverall - EPSILON).toInt()
            val totalNeeded = (requiredAttendedTotal - attended).coerceIn(0, remaining)
            (remaining - totalNeeded).coerceAtLeast(0)
        } else null

        val status = when {
            projPct < target -> BunkStatus.DANGER_ZONE
            safeSkipsNow >= 2 -> BunkStatus.COMFORTABLY_SAFE
            else -> BunkStatus.CAUTION_BORDERLINE
        }

        return BunkCalculationResult(
            currentPercentage = currentPct,
            projectedPercentage = projPct,
            safeSkipsNow = safeSkipsNow,
            classesNeededForTarget = classesNeeded,
            endOfTermSkips = endOfTermSkips,
            targetPercentage = target,
            status = status
        )
    }

    /**
     * Inspects upcoming class dates (up to horizonDays, e.g. 14 days), checks assessments,
     * bulk classes, and adjacency to weekends/holidays to generate smart bunk recommendations.
     */
    fun analyzeUpcomingDays(
        futureOccurrences: List<ClassOccurrence>,
        holidays: List<Holiday>,
        recurringHolidayRules: List<RecurringHolidayRule>,
        assessments: List<Assessment>,
        safeSkipsAvailable: Int,
        currentPercentage: Float,
        targetPercentage: Float,
        startDate: LocalDate = LocalDate.now(),
        horizonDays: Long = 14
    ): List<DayBunkOpportunity> {
        val endDate = startDate.plusDays(horizonDays)

        // Set of declared single/multi-day holidays
        val holidayDates = mutableSetOf<LocalDate>()
        for (h in holidays) {
            var cur = h.startDate
            while (!cur.isAfter(h.endDate) && !cur.isAfter(endDate)) {
                holidayDates.add(cur)
                cur = cur.plusDays(1)
            }
        }

        // Helper to check if a date is an off-day (holiday or recurring weekend off)
        fun isOffDay(date: LocalDate): Boolean {
            if (date in holidayDates) return true
            val rule = recurringHolidayRules.find { it.dayOfWeek == date.dayOfWeek }
            return when (rule?.mode) {
                RecurringHolidayMode.ALWAYS -> true
                RecurringHolidayMode.PATTERN -> {
                    val weekOfMonth = ((date.dayOfMonth - 1) / 7)
                    (rule.weeksOfMonth and (1 shl weekOfMonth)) != 0
                }
                else -> date.dayOfWeek == DayOfWeek.SUNDAY // default weekend
            }
        }

        // Group future occurrences by date
        val occurrencesByDate = futureOccurrences
            .filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) && it.status != OccurrenceStatus.CANCELLED }
            .groupBy { it.date }

        val assessmentsByDate = assessments
            .filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) }
            .groupBy { it.date }

        val opportunities = mutableListOf<DayBunkOpportunity>()

        for ((date, occs) in occurrencesByDate.toSortedMap()) {
            val classCount = occs.sumOf { it.classCount }
            val subjectIds = occs.map { it.subjectId }.distinct()
            val dayAssessments = assessmentsByDate[date].orEmpty()
            val hasAssessment = dayAssessments.isNotEmpty()
            val assessmentTitle = dayAssessments.firstOrNull()?.let { "${it.type}: ${it.title ?: "Exam/Test"}" }

            val nextDayIsOff = isOffDay(date.plusDays(1))
            val prevDayIsOff = isOffDay(date.minusDays(1))

            // Long weekend check: e.g. Friday before weekend, or Monday after weekend
            val isFridayBeforeWeekend = date.dayOfWeek == DayOfWeek.FRIDAY && nextDayIsOff
            val isMondayAfterWeekend = date.dayOfWeek == DayOfWeek.MONDAY && prevDayIsOff
            val isBetweenOffDays = prevDayIsOff && nextDayIsOff

            val createsLongWeekend = isFridayBeforeWeekend || isMondayAfterWeekend
            val isBridge = isBetweenOffDays || (prevDayIsOff && isOffDay(date.plusDays(2)))

            val canAfford = currentPercentage >= targetPercentage && safeSkipsAvailable >= classCount

            val (recType, reason) = when {
                hasAssessment -> {
                    BunkRecommendationType.DO_NOT_BUNK to "Assessment scheduled today (${dayAssessments.first().type}). Mandatory attendance."
                }
                !canAfford -> {
                    BunkRecommendationType.DO_NOT_BUNK to "Debarment risk: missing $classCount class${if (classCount > 1) "es" else ""} drops you below your ${targetPercentage.toInt()}% goal."
                }
                createsLongWeekend && canAfford -> {
                    val breakDays = if (isFridayBeforeWeekend) "3-day weekend" else "long weekend"
                    BunkRecommendationType.RECOMMENDED to "Great Long Weekend Bridge: safe to skip $classCount class${if (classCount > 1) "es" else ""} for a continuous $breakDays!"
                }
                isBridge && canAfford -> {
                    BunkRecommendationType.RECOMMENDED to "Holiday Bridge: connect surrounding off-days into an extended vacation safely."
                }
                classCount >= 3 -> {
                    BunkRecommendationType.HIGH_IMPACT to "Heavy Class Day: $classCount classes scheduled. High attendance penalty if skipped."
                }
                classCount == 1 && canAfford -> {
                    BunkRecommendationType.OPTIONAL_SAFE to "Low Impact Day: only 1 class scheduled. Safe to bunk if you need time off."
                }
                else -> {
                    BunkRecommendationType.OPTIONAL_SAFE to "Safe to bunk: within your available bunk margin."
                }
            }

            opportunities.add(
                DayBunkOpportunity(
                    date = date,
                    classCount = classCount,
                    subjectIds = subjectIds,
                    hasAssessment = hasAssessment,
                    assessmentTitle = assessmentTitle,
                    createsLongWeekend = createsLongWeekend,
                    isBridgeHoliday = isBridge,
                    recommendationType = recType,
                    recommendationReason = reason
                )
            )
        }

        return opportunities
    }
}
