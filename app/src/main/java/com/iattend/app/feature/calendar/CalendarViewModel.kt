package com.iattend.app.feature.calendar

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iattend.app.core.data.db.Assessment
import com.iattend.app.core.data.db.AssessmentDao
import com.iattend.app.core.data.db.ClassOccurrence
import com.iattend.app.core.data.db.ClassOccurrenceDao
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.data.db.SubjectDao
import com.iattend.app.core.datastore.CalendarViewMode
import com.iattend.app.core.datastore.SettingsRepository
import com.iattend.app.core.domain.occurrence.OccurrenceRepository
import com.iattend.app.core.domain.stats.AttendanceStatsCalculator
import com.iattend.app.core.notifications.ClassReminderScheduler
import com.iattend.app.widget.UpcomingClassesWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import com.iattend.app.core.data.db.AssessmentAttemptStatus
import com.iattend.app.core.data.db.TimetableSlot
import com.iattend.app.core.data.db.TimetableSlotDao
import com.iattend.app.core.data.db.TimetableVersionDao
import javax.inject.Inject

data class TimetableEditTarget(
    val versionId: Long,
    val dayOfWeek: DayOfWeek,
    val slotId: Long?
)

data class DayOccurrenceRow(
    val occurrence: ClassOccurrence,
    val subject: Subject?,
    val stats: AttendanceStatsCalculator.SubjectStats?,
    val requiredPercentage: Float,
    val linkedTests: List<Assessment> = emptyList()
)

data class DayAssessmentRow(val assessment: Assessment, val subject: Subject?)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val classOccurrenceDao: ClassOccurrenceDao,
    private val assessmentDao: AssessmentDao,
    private val timetableSlotDao: TimetableSlotDao,
    private val timetableVersionDao: TimetableVersionDao,
    private val occurrenceRepository: OccurrenceRepository,
    private val reminderScheduler: ClassReminderScheduler,
    @ApplicationContext private val context: Context,
    subjectDao: SubjectDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val viewMode: StateFlow<CalendarViewMode> = settingsRepository.calendarViewMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarViewMode.WEEK)

    private val minuteTicks = flow {
        while (true) {
            emit(Unit)
            delay(60_000)
        }
    }

    val attendanceStatusByDate: StateFlow<Map<LocalDate, OccurrenceStatus>> = combine(
        classOccurrenceDao.getAll(), minuteTicks
    ) { occurrences, _ ->
            val today = LocalDate.now()
            val now = LocalTime.now()
            occurrences.groupBy { it.date }.mapNotNull { (date, rows) ->
                attendanceStatusForDay(rows, today, now)?.let { date to it }
            }.toMap()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val monthAttendancePercentage: StateFlow<Float?> = combine(_selectedDate, classOccurrenceDao.getAll()) { date, occurrences ->
        attendancePercentageForMonth(occurrences, YearMonth.from(date))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isPast: StateFlow<Boolean> = selectedDate
        .map { !it.isAfter(LocalDate.now()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    /** Sunday-start week containing [selectedDate], matching the app's existing Sunday=0 convention. */
    val visibleWeek: StateFlow<List<LocalDate>> = selectedDate
        .map { date ->
            val weekStart = date.minusDays((date.dayOfWeek.value % 7).toLong())
            (0..6).map { weekStart.plusDays(it.toLong()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rows: StateFlow<List<DayOccurrenceRow>> = combine(
        _selectedDate,
        classOccurrenceDao.getAll(),
        subjectDao.getAll(),
        assessmentDao.getAll(),
        settingsRepository.settings
    ) { date, occurrences, subjects, assessments, settings ->
        val subjectsById = subjects.associateBy { it.id }
        occurrences
            .filter { it.date == date }
            .sortedWith(compareBy({ it.startTime }, { it.id }))
            .map { occ ->
                val subject = subjectsById[occ.subjectId]
                val req = subject?.requiredPercentageOverride ?: settings.requiredPercentageDefault
                val stats = subject?.let {
                    val allForSubject = occurrences.filter { o -> o.subjectId == it.id }
                    AttendanceStatsCalculator.compute(it, allForSubject, req, settings.trackingEndDate)
                }
                val linked = assessments.filter { it.linkedOccurrenceId == occ.id }
                DayOccurrenceRow(occ, subject, stats, req, linked)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assessmentsForSelectedDate: StateFlow<List<DayAssessmentRow>> = combine(
        _selectedDate,
        assessmentDao.getAll(),
        subjectDao.getAll()
    ) { date, allAssessments, subjects ->
        val subjectsById = subjects.associateBy { it.id }
        allAssessments
            .filter { it.date == date && it.linkedOccurrenceId == null }
            .sortedWith(compareBy({ it.startTime }, { it.id }))
            .map { DayAssessmentRow(it, subjectsById[it.subjectId]) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assessmentDates: StateFlow<Set<LocalDate>> = assessmentDao.getAll()
        .map { all -> all.map { it.date }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val assessmentDatesInWeek: StateFlow<Set<LocalDate>> = combine(
        visibleWeek,
        assessmentDates
    ) { week, allDates ->
        allDates.filter { it in week }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun selectDate(date: LocalDate) { _selectedDate.value = date }
    fun setViewMode(mode: CalendarViewMode) {
        viewModelScope.launch { settingsRepository.setCalendarViewMode(mode) }
    }
    fun previousWeek() { _selectedDate.value = _selectedDate.value.minusWeeks(1) }
    fun nextWeek() { _selectedDate.value = _selectedDate.value.plusWeeks(1) }
    fun previousMonth() { shiftMonth(-1) }
    fun nextMonth() { shiftMonth(1) }

    private fun shiftMonth(amount: Long) {
        val date = _selectedDate.value
        val month = date.plusMonths(amount)
        _selectedDate.value = month.withDayOfMonth(minOf(date.dayOfMonth, month.lengthOfMonth()))
    }

    suspend fun getLinkedTests(occurrenceId: Long): List<Assessment> =
        occurrenceRepository.getLinkedTests(occurrenceId)

    suspend fun findScoredLinkedTestConflicts(occurrenceId: Long, status: OccurrenceStatus): List<Assessment> {
        val targetStatus = if (status == OccurrenceStatus.UNMARKED) OccurrenceStatus.UNMARKED else status
        return occurrenceRepository.findScoredLinkedTestConflicts(occurrenceId, targetStatus)
    }

    fun mark(occurrence: ClassOccurrence, status: OccurrenceStatus, forceClearScores: Boolean = false) {
        if (occurrence.date.isAfter(LocalDate.now())) return
        val newStatus = if (occurrence.status == status) OccurrenceStatus.UNMARKED else status
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

    suspend fun getTimetableSlot(slotId: Long): TimetableSlot? =
        timetableSlotDao.getByIdOnce(slotId)

    suspend fun findTimetableEditTarget(occurrence: ClassOccurrence): TimetableEditTarget? {
        val dayOfWeek = occurrence.date.dayOfWeek
        // 1. Direct slot lookup if timetableSlotId exists and is valid
        val directSlot = occurrence.timetableSlotId?.let { timetableSlotDao.getByIdOnce(it) }
        if (directSlot != null) {
            return TimetableEditTarget(directSlot.timetableVersionId, directSlot.dayOfWeek, directSlot.id)
        }

        // 2. Otherwise find the active version for the occurrence date
        val versions = timetableVersionDao.getAllOnce()
        if (versions.isEmpty()) return null
        val activeVersion = versions
            .filter { !it.effectiveFrom.isAfter(occurrence.date) }
            .maxByOrNull { it.effectiveFrom }
            ?: versions.maxByOrNull { it.effectiveFrom }
            ?: return null

        val slots = timetableSlotDao.getForVersionOnce(activeVersion.id)
        val matchingSlot = slots.firstOrNull { slot ->
            slot.dayOfWeek == dayOfWeek &&
                slot.subjectId == occurrence.subjectId &&
                (occurrence.startTime == null || slot.startTime == occurrence.startTime)
        } ?: slots.firstOrNull { slot ->
            slot.dayOfWeek == dayOfWeek && slot.subjectId == occurrence.subjectId
        }

        return TimetableEditTarget(
            versionId = activeVersion.id,
            dayOfWeek = dayOfWeek,
            slotId = matchingSlot?.id
        )
    }

    suspend fun findScoredAssessmentConflict(assessmentId: Long): Boolean {
        val assessment = assessmentDao.getByIdOnce(assessmentId) ?: return false
        return assessment.obtainedMarks != null
    }

    fun updateAssessmentAttemptStatus(
        assessmentId: Long,
        newStatus: AssessmentAttemptStatus,
        clearScore: Boolean = false
    ) {
        viewModelScope.launch {
            val assessment = assessmentDao.getByIdOnce(assessmentId) ?: return@launch
            val updated = assessment.copy(
                attemptStatus = newStatus,
                obtainedMarks = if (clearScore) null else assessment.obtainedMarks,
                isAttemptOverridden = true,
                needsRescheduling = if (newStatus == AssessmentAttemptStatus.ATTEMPTED) false else assessment.needsRescheduling
            )
            assessmentDao.update(updated)
        }
    }
}
