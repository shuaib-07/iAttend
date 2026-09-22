package com.iattend.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iattend.app.core.data.db.ClassOccurrence
import com.iattend.app.core.data.db.ClassOccurrenceDao
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.data.db.SubjectDao
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.core.datastore.SettingsRepository
import com.iattend.app.core.domain.stats.AttendanceStatsCalculator
import com.iattend.app.feature.calendar.DaySummary
import com.iattend.app.feature.calendar.isPastOccurrence
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
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
    val thisSemesterPercent: Float = 0f,
    val semesterLabel: String = "Last 120 days",
    val semesterFallbackNotice: String? = "Tracking dates aren’t set; using the last 120 days.",
    val semesterOccurred: Int = 0,
    val semesterTotalClasses: Int? = null,
    val semesterPresent: Int = 0,
    val semesterAbsent: Int = 0,
    val semesterUnmarked: Int = 0,
    val pastUnmarkedClasses: List<ClassOccurrence> = emptyList()
) {
    /** Projected total when any subject has one (Known/Computed), else falls back to marked-so-far. */
    val summaryDenominator: Int get() = if (projectedTotal > 0) projectedTotal else overallTotal
    val overallPercentage: Float get() = if (summaryDenominator == 0) 0f else overallAttended * 100f / summaryDenominator
    val overallMarkedPercentage: Float get() = if (overallTotal == 0) 0f else overallAttended * 100f / overallTotal
    val overallPercentageCaption: String get() = if (projectedTotal > 0) "of all classes" else "of marked classes"
}

internal data class AttendancePeriod(val start: LocalDate, val end: LocalDate, val label: String, val notice: String?)

internal fun attendancePeriod(start: LocalDate?, end: LocalDate?, today: LocalDate): AttendancePeriod {
    if (start != null && end != null && !start.isAfter(end)) {
        return AttendancePeriod(start, end, "Tracking period", null)
    }
    if (start != null && end == null) {
        return AttendancePeriod(start, today, "Since start date", null)
    }
    val fallbackEnd = end ?: today
    val notice = when {
        start != null && end != null -> "Tracking dates conflict; using a 120-day window."
        end != null -> "Tracking start isn’t set; using 120 days through the configured end date."
        else -> "Tracking dates aren’t set; using the last 120 days."
    }
    return AttendancePeriod(fallbackEnd.minusDays(119), fallbackEnd, "Last 120 days", notice)
}

internal data class PeriodAttendance(
    val present: Int,
    val absent: Int,
    val pastUnmarkedClasses: List<ClassOccurrence>
) {
    val occurred: Int get() = present + absent
    val unmarked: Int get() = pastUnmarkedClasses.sumOf { it.classCount }
}

internal fun summarizePeriod(
    occurrences: List<ClassOccurrence>,
    period: AttendancePeriod,
    today: LocalDate,
    now: LocalTime
): PeriodAttendance {
    val inPeriod = occurrences.filter { it.date in period.start..period.end }
    val marked = inPeriod.filter { it.status == OccurrenceStatus.PRESENT || it.status == OccurrenceStatus.ABSENT }
    val present = marked.filter { it.status == OccurrenceStatus.PRESENT }.sumOf { it.classCount }
    val absent = marked.filter { it.status == OccurrenceStatus.ABSENT }.sumOf { it.classCount }
    val unmarked = inPeriod.filter {
        it.status == OccurrenceStatus.UNMARKED && isPastOccurrence(it, today, now)
    }.sortedWith(compareByDescending<ClassOccurrence> { it.date }.thenByDescending { it.startTime })
    return PeriodAttendance(present, absent, unmarked)
}

private val refreshMinute = flow {
    while (true) {
        emit(Unit)
        delay(60_000)
    }
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
        settingsRepository.settings,
        refreshMinute
    ) { subjects, occurrences, settings, _ ->
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
        val now = LocalTime.now()
        val period = attendancePeriod(settings.trackingStartDate, settings.trackingEndDate, today)
        val periodAttendance = summarizePeriod(occurrences, period, today, now)

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
            thisMonthPercent = run {
                val monthRows = markedOccurrences.filter { it.date >= today.withDayOfMonth(1) }
                val total = monthRows.sumOf { it.classCount }
                if (total == 0) 0f else monthRows.filter { it.status == OccurrenceStatus.PRESENT }.sumOf { it.classCount } * 100f / total
            },
            thisSemesterPercent = if (periodAttendance.occurred == 0) 0f else periodAttendance.present * 100f / periodAttendance.occurred,
            semesterLabel = period.label,
            semesterFallbackNotice = period.notice,
            semesterOccurred = periodAttendance.occurred,
            semesterTotalClasses = if (
                settings.trackingEndDate != null || subjects.any { it.trackingEndDateOverride != null }
            ) summaries.sumOf { it.stats.totalOverall ?: 0 }.takeIf { it > 0 } else null,
            semesterPresent = periodAttendance.present,
            semesterAbsent = periodAttendance.absent,
            semesterUnmarked = periodAttendance.unmarked,
            pastUnmarkedClasses = periodAttendance.pastUnmarkedClasses
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())
}
