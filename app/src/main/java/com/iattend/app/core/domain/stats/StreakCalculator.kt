package com.iattend.app.core.domain.stats

import com.iattend.app.core.data.db.ClassOccurrence
import com.iattend.app.core.data.db.OccurrenceStatus

data class StreakStats(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0
)

object StreakCalculator {
    fun compute(occurrences: List<ClassOccurrence>): StreakStats {
        val marked = occurrences
            .filter { it.status == OccurrenceStatus.PRESENT || it.status == OccurrenceStatus.ABSENT }
            .sortedWith(compareBy({ it.date }, { it.startTime }))

        var current = 0
        var best = 0

        for (occ in marked) {
            if (occ.status == OccurrenceStatus.PRESENT) {
                current += occ.classCount
                if (current > best) {
                    best = current
                }
            } else if (occ.status == OccurrenceStatus.ABSENT) {
                current = 0
            }
        }

        return StreakStats(currentStreak = current, bestStreak = best)
    }
}
