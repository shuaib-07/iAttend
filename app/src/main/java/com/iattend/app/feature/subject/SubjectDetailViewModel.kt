package com.iattend.app.feature.subject

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.iattend.app.core.data.db.Assessment
import com.iattend.app.core.data.db.AssessmentDao
import com.iattend.app.core.data.db.ClassOccurrence
import com.iattend.app.core.data.db.ClassOccurrenceDao
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.data.db.SubjectDao
import com.iattend.app.core.datastore.CalendarViewMode
import com.iattend.app.core.datastore.SettingsRepository
import com.iattend.app.core.domain.assessment.AssessmentGradingCalculator
import com.iattend.app.core.domain.assessment.SubjectGradingSummary
import com.iattend.app.core.domain.occurrence.OccurrenceRepository
import com.iattend.app.core.domain.stats.AttendanceStatsCalculator
import com.iattend.app.core.navigation.SubjectDetailRoute
import com.iattend.app.core.notifications.ClassReminderScheduler
import com.iattend.app.widget.UpcomingClassesWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class SubjectDetailState(
    val subject: Subject? = null,
    val stats: AttendanceStatsCalculator.SubjectStats? = null,
    val history: List<ClassOccurrence> = emptyList(),
    val requiredPercentage: Float = 75f,
    val selectedDate: LocalDate = LocalDate.now(),
    val viewMode: CalendarViewMode = CalendarViewMode.MONTH,
    val calendarMonth: YearMonth = YearMonth.now(),
    val selectedDateOccurrences: List<ClassOccurrence> = emptyList(),
    val selectedDateAssessments: List<Assessment> = emptyList(),
    val linkedAssessmentsByOccurrenceId: Map<Long, List<Assessment>> = emptyMap(),
    val gradingSummary: SubjectGradingSummary? = null,
    val attendanceStatusByDate: Map<LocalDate, OccurrenceStatus> = emptyMap(),
    val assessmentDates: Set<LocalDate> = emptySet()
)

@HiltViewModel
class SubjectDetailViewModel @Inject constructor(
    private val subjectDao: SubjectDao,
    private val settingsRepository: SettingsRepository,
    private val classOccurrenceDao: ClassOccurrenceDao,
    private val assessmentDao: AssessmentDao,
    private val occurrenceRepository: OccurrenceRepository,
    private val reminderScheduler: ClassReminderScheduler,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val subjectId = savedStateHandle.toRoute<SubjectDetailRoute>().subjectId

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _calendarMonth = MutableStateFlow(YearMonth.now())
    val calendarMonth: StateFlow<YearMonth> = _calendarMonth.asStateFlow()

    private val _viewMode = MutableStateFlow(CalendarViewMode.MONTH)
    val viewMode: StateFlow<CalendarViewMode> = _viewMode.asStateFlow()

    val state: StateFlow<SubjectDetailState> = combine(
        subjectDao.getById(subjectId),
        classOccurrenceDao.getForSubject(subjectId),
        assessmentDao.getForSubject(subjectId),
        settingsRepository.settings,
        _selectedDate,
        _calendarMonth,
        _viewMode
    ) { params ->
        val subject = params[0] as? Subject
        @Suppress("UNCHECKED_CAST")
        val occurrences = (params[1] as? List<ClassOccurrence>) ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val assessments = (params[2] as? List<Assessment>) ?: emptyList()
        val settings = params[3] as com.iattend.app.core.datastore.AppSettings
        val selDate = params[4] as LocalDate
        val calMonth = params[5] as YearMonth
        val mode = params[6] as CalendarViewMode

        val history = occurrences.sortedBy { it.date }
        val requiredPercentage = subject?.requiredPercentageOverride ?: settings.requiredPercentageDefault
        val stats = if (subject != null) {
            AttendanceStatsCalculator.compute(subject, occurrences, requiredPercentage, settings.trackingEndDate)
        } else null

        val selectedOccurrences = occurrences.filter { it.date == selDate }
            .sortedBy { it.startTime }

        val linkedMap = occurrences.associate { occ ->
            occ.id to assessments.filter { it.linkedOccurrenceId == occ.id }
        }

        // Independent assessments on selected date
        val independentAssessmentsOnDate = assessments.filter {
            it.date == selDate && it.linkedOccurrenceId == null
        }.sortedBy { it.startTime }

        val gradingSummary = if (subject?.gradingSystem != null) {
            AssessmentGradingCalculator.computeSubjectSummary(
                subject = subject,
                assessments = assessments
            )
        } else null

        val attendanceByDate = occurrences.groupBy { it.date }.mapNotNull { (date, rows) ->
            val priority = when {
                rows.any { it.status == OccurrenceStatus.ABSENT } -> OccurrenceStatus.ABSENT
                rows.any { it.status == OccurrenceStatus.PRESENT } -> OccurrenceStatus.PRESENT
                rows.any { it.status == OccurrenceStatus.CANCELLED } -> OccurrenceStatus.CANCELLED
                rows.any { it.status == OccurrenceStatus.UNMARKED } -> OccurrenceStatus.UNMARKED
                else -> null
            }
            priority?.let { date to it }
        }.toMap()

        val assessDates = assessments.map { it.date }.toSet()

        SubjectDetailState(
            subject = subject,
            stats = stats,
            history = history,
            requiredPercentage = requiredPercentage,
            selectedDate = selDate,
            viewMode = mode,
            calendarMonth = calMonth,
            selectedDateOccurrences = selectedOccurrences,
            selectedDateAssessments = independentAssessmentsOnDate,
            linkedAssessmentsByOccurrenceId = linkedMap,
            gradingSummary = gradingSummary,
            attendanceStatusByDate = attendanceByDate,
            assessmentDates = assessDates
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SubjectDetailState())

    private val _deleted = Channel<Unit>(Channel.BUFFERED)
    val deleted = _deleted.receiveAsFlow()

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        _calendarMonth.value = YearMonth.from(date)
    }

    fun previousPeriod() {
        if (_viewMode.value == CalendarViewMode.MONTH) {
            val prevMonth = _calendarMonth.value.minusMonths(1)
            _calendarMonth.value = prevMonth
            _selectedDate.value = prevMonth.atDay(1)
        } else {
            val prevWeek = _selectedDate.value.minusWeeks(1)
            _selectedDate.value = prevWeek
            _calendarMonth.value = YearMonth.from(prevWeek)
        }
    }

    fun nextPeriod() {
        if (_viewMode.value == CalendarViewMode.MONTH) {
            val nextMonth = _calendarMonth.value.plusMonths(1)
            _calendarMonth.value = nextMonth
            _selectedDate.value = nextMonth.atDay(1)
        } else {
            val nextWeek = _selectedDate.value.plusWeeks(1)
            _selectedDate.value = nextWeek
            _calendarMonth.value = YearMonth.from(nextWeek)
        }
    }

    fun setViewMode(mode: CalendarViewMode) {
        _viewMode.value = mode
    }

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == CalendarViewMode.MONTH) CalendarViewMode.WEEK else CalendarViewMode.MONTH
    }

    fun goToToday() {
        val today = LocalDate.now()
        _selectedDate.value = today
        _calendarMonth.value = YearMonth.from(today)
    }

    fun delete() {
        viewModelScope.launch {
            state.value.subject?.let { subjectDao.delete(it) }
            _deleted.send(Unit)
        }
    }

    suspend fun getLinkedTests(occurrenceId: Long): List<Assessment> =
        occurrenceRepository.getLinkedTests(occurrenceId)

    fun cycleStatus(
        occurrence: ClassOccurrence,
        onConflict: (List<Assessment>, OccurrenceStatus) -> Unit
    ) {
        if (occurrence.date.isAfter(LocalDate.now())) return
        val next = when (occurrence.status) {
            OccurrenceStatus.UNMARKED -> OccurrenceStatus.PRESENT
            OccurrenceStatus.PRESENT -> OccurrenceStatus.ABSENT
            OccurrenceStatus.ABSENT -> OccurrenceStatus.CANCELLED
            OccurrenceStatus.CANCELLED -> OccurrenceStatus.UNMARKED
        }

        viewModelScope.launch {
            if (next == OccurrenceStatus.ABSENT || next == OccurrenceStatus.UNMARKED) {
                val conflicts = occurrenceRepository.findScoredLinkedTestConflicts(occurrence.id, next)
                if (conflicts.isNotEmpty()) {
                    onConflict(conflicts, next)
                    return@launch
                }
            }
            occurrenceRepository.updateOccurrenceAttendance(occurrence, next, clearScoresForTests = false)
            UpcomingClassesWidget().updateAll(context)
        }
    }

    fun updateOccurrenceStatus(
        occurrence: ClassOccurrence,
        newStatus: OccurrenceStatus,
        forceClearScores: Boolean = false
    ) {
        viewModelScope.launch {
            occurrenceRepository.updateOccurrenceAttendance(occurrence, newStatus, clearScoresForTests = forceClearScores)
            UpcomingClassesWidget().updateAll(context)
        }
    }

    fun setCancelReason(occurrence: ClassOccurrence, reason: String?) {
        viewModelScope.launch {
            classOccurrenceDao.update(occurrence.copy(cancelReason = reason?.ifBlank { null }))
            UpcomingClassesWidget().updateAll(context)
        }
    }

    fun deleteOccurrence(occurrence: ClassOccurrence, deleteLinkedTests: Boolean = false) {
        viewModelScope.launch {
            occurrenceRepository.deleteSingleOccurrence(occurrence, deleteLinkedTests)
            UpcomingClassesWidget().updateAll(context)
        }
    }
}
