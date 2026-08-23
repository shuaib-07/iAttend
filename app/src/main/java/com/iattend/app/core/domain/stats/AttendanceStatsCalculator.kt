package com.iattend.app.core.domain.stats

import com.iattend.app.core.data.db.ClassOccurrence
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.data.db.TotalMode
import java.time.LocalDate
import kotlin.math.ceil

private const val EPSILON = 1e-6f

/**
 * Pure domain logic turning a subject's occurrences into the dashboard numbers
 * (Product Plan §9.2): attended/total-so-far/percentage always available; the
 * needed/can-skip/remaining projections only when the subject has a known or
 * computable total (null = N/A, shown for OPEN_ENDED subjects and for COMPUTED
 * subjects until a tracking end date is set).
 */
object AttendanceStatsCalculator {

    data class SubjectStats(
        val attended: Int,
        val totalSoFar: Int,
        val totalOverall: Int?,
        val remaining: Int?,
        val classesNeeded: Int?,
        val classesCanSkip: Int?
    ) {
        val percentage: Float get() = if (totalSoFar == 0) 0f else attended * 100f / totalSoFar
    }

    fun compute(
        subject: Subject,
        occurrences: List<ClassOccurrence>,
        requiredPercentage: Float,
        trackingEndDate: LocalDate?
    ): SubjectStats {
        // A subject's own end date (set on the subject) wins over the global Settings default,
        // matching how requiredPercentageOverride already works.
        val effectiveEndDate = subject.trackingEndDateOverride ?: trackingEndDate

        val relevant = occurrences
            .filter { it.status != OccurrenceStatus.CANCELLED }
            .let { rows ->
                if (subject.totalMode == TotalMode.COMPUTED && effectiveEndDate != null) {
                    rows.filter { !it.date.isAfter(effectiveEndDate) }
                } else {
                    rows
                }
            }
        val attended = relevant.filter { it.status == OccurrenceStatus.PRESENT }.sumOf { it.classCount }
        val totalSoFar = relevant
            .filter { it.status == OccurrenceStatus.PRESENT || it.status == OccurrenceStatus.ABSENT }
            .sumOf { it.classCount }

        val totalOverall = when (subject.totalMode) {
            TotalMode.KNOWN -> subject.knownTotalClasses
            // Occurrences are already generated out to the effective end date (OccurrenceRepository
            // walks the horizon out to cover every subject's own override too), so summing the
            // ones on/before it gives the walked-forward total. No end date yet = nothing to walk to.
            TotalMode.COMPUTED -> if (effectiveEndDate != null) relevant.sumOf { it.classCount }.takeIf { it > 0 } else null
            TotalMode.OPEN_ENDED -> null
        }

        if (totalOverall == null || totalOverall <= 0) {
            return SubjectStats(attended, totalSoFar, null, null, null, null)
        }

        val remaining = (totalOverall - totalSoFar).coerceAtLeast(0)
        val requiredFraction = requiredPercentage / 100f
        // Minimum total presents needed by term end to finish at/above the threshold (inclusive >=).
        val minAttendedNeeded = ceil(requiredFraction * totalOverall - EPSILON).toInt().coerceAtLeast(0)
        val classesNeeded = (minAttendedNeeded - attended).coerceIn(0, remaining)
        val classesCanSkip = remaining - classesNeeded

        return SubjectStats(attended, totalSoFar, totalOverall, remaining, classesNeeded, classesCanSkip)
    }
}
