package com.iattend.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iattend.app.core.data.db.ClassOccurrenceDao
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.data.db.SubjectDao
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.core.datastore.SettingsRepository
import com.iattend.app.core.domain.stats.AttendanceStatsCalculator
import com.iattend.app.feature.calendar.DaySummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class SubjectSummary(
    val subject: Subject,
    val stats: AttendanceStatsCalculator.SubjectStats,
    val requiredPercentage: Float
) {
    val attended get() = stats.attended
    val total get() = stats.totalSoFar
    val percentage get() = stats.percentage
}

data class AttendancePoint(val date: LocalDate, val cumulativePercent: Float)

data class WeeklyAttendancePoint(val weekStart: LocalDate, val percent: Float)

data class HomeState(
    val overallAttended: Int = 0,
    val overallTotal: Int = 0,
    val projectedTotal: Int = 0,
    val requiredPercentageDefault: Float = 75f,
    val subjects: List<SubjectSummary> = emptyList(),
    val heatmap: Map<LocalDate, Int> = emptyMap(),
    val attendanceTrend: List<AttendancePoint> = emptyList(),
    val weeklyTrend: List<WeeklyAttendancePoint> = emptyList(),
    val thisMonthPercent: Float = 0f,
    val thisSemesterPercent: Float = 0f
) {
    /** Projected total when any subject has one (Known/Computed), else falls back to marked-so-far. */
    val summaryDenominator: Int get() = if (projectedTotal > 0) projectedTotal else overallTotal
    val overallPercentage: Float get() = if (summaryDenominator == 0) 0f else overallAttended * 100f / summaryDenominator
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    subjectDao: SubjectDao,
    classOccurrenceDao: ClassOccurrenceDao,
    settingsRepository: SettingsRepository
) : ViewModel() {
    val state: StateFlow<HomeState> = combine(
        subjectDao.getAll(),
        classOccurrenceDao.getAll(),
        settingsRepository.settings
    ) { subjects, occurrences, settings ->
        val bySubject = occurrences.groupBy { it.subjectId }
        val summaries = subjects.map { subject ->
            val requiredPercentage = subject.requiredPercentageOverride ?: settings.requiredPercentageDefault
            val stats = AttendanceStatsCalculator.compute(
                subject, bySubject[subject.id].orEmpty(), requiredPercentage, settings.trackingEndDate
            )
            SubjectSummary(subject, stats, requiredPercentage)
        }
        // Present-count intensity per spec #4 (Cashiro style: 0/1/2/3-4/5+), replaces DaySummary status coloring
        val heatmap = occurrences.groupBy { it.date }.mapValues { (_, dayOccurrences) ->
            dayOccurrences.filter { it.status == OccurrenceStatus.PRESENT }.sumOf { it.classCount }
        }

        // Same attended/totalSoFar rule as AttendanceStatsCalculator (PRESENT+ABSENT counted,
        // UNMARKED/CANCELLED excluded) so the trend's last point matches overallPercentage.
        val markedOccurrences = occurrences.filter {
            it.status == OccurrenceStatus.PRESENT || it.status == OccurrenceStatus.ABSENT
        }
        var runningAttended = 0
        var runningTotal = 0
        val attendanceTrend = markedOccurrences.groupBy { it.date }.toSortedMap().map { (date, dayOccurrences) ->
            runningAttended += dayOccurrences.filter { it.status == OccurrenceStatus.PRESENT }.sumOf { it.classCount }
            runningTotal += dayOccurrences.sumOf { it.classCount }
            AttendancePoint(date, if (runningTotal == 0) 0f else runningAttended * 100f / runningTotal)
        }

        val today = LocalDate.now()
        fun percentSince(cutoff: LocalDate?): Float {
            val window = if (cutoff == null) markedOccurrences else markedOccurrences.filter { !it.date.isBefore(cutoff) }
            val attended = window.filter { it.status == OccurrenceStatus.PRESENT }.sumOf { it.classCount }
            val total = window.sumOf { it.classCount }
            return if (total == 0) 0f else attended * 100f / total
        }

        // Weekly avg % for BalanceChart (spec #5): group marked occurrences by ISO week, compute weekly % and smooth
        val weeklyTrend = markedOccurrences.groupBy { it.date.with(java.time.DayOfWeek.MONDAY) }
            .toSortedMap()
            .map { (weekStart, weekOccs) ->
                val att = weekOccs.filter { it.status == OccurrenceStatus.PRESENT }.sumOf { it.classCount }
                val tot = weekOccs.sumOf { it.classCount }
                WeeklyAttendancePoint(weekStart, if (tot == 0) 0f else att * 100f / tot)
            }

        HomeState(
            overallAttended = summaries.sumOf { it.attended },
            overallTotal = summaries.sumOf { it.total },
            projectedTotal = summaries.sumOf { it.stats.totalOverall ?: 0 },
            requiredPercentageDefault = settings.requiredPercentageDefault,
            subjects = summaries,
            heatmap = heatmap,
            attendanceTrend = attendanceTrend,
            weeklyTrend = weeklyTrend,
            thisMonthPercent = percentSince(today.withDayOfMonth(1)),
            thisSemesterPercent = percentSince(today.minusDays(120))
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())
}
