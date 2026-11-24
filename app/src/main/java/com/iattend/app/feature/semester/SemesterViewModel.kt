package com.iattend.app.feature.semester

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iattend.app.core.data.db.AssessmentDao
import com.iattend.app.core.data.db.Holiday
import com.iattend.app.core.data.db.HolidayDao
import com.iattend.app.core.data.db.RecurringHolidayRule
import com.iattend.app.core.data.db.RecurringHolidayRuleDao
import com.iattend.app.core.data.db.Semester
import com.iattend.app.core.data.db.SemesterDao
import com.iattend.app.core.data.db.SubjectDao
import com.iattend.app.core.data.db.TimetableVersionDao
import com.iattend.app.core.datastore.SettingsRepository
import com.iattend.app.core.domain.occurrence.OccurrenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class SemesterUiState(
    val activeSemester: Semester? = null,
    val semesters: List<Semester> = emptyList(),
    val isViewingArchived: Boolean = false,
    val isViewingInactive: Boolean = false,
    val showWelcomePrompt: Boolean = false,
    val examPeriodSemesterIds: Set<Long> = emptySet(),
    val currentSemesterId: Long? = null
)

@HiltViewModel
class SemesterViewModel @Inject constructor(
    private val semesterDao: SemesterDao,
    private val settingsRepository: SettingsRepository,
    private val holidayDao: HolidayDao,
    private val recurringHolidayRuleDao: RecurringHolidayRuleDao,
    private val subjectDao: SubjectDao,
    private val timetableVersionDao: TimetableVersionDao,
    private val assessmentDao: AssessmentDao,
    private val occurrenceRepository: OccurrenceRepository
) : ViewModel() {

    private val _welcomeDismissed = MutableStateFlow(false)

    val uiState: StateFlow<SemesterUiState> = combine(
        semesterDao.getAll(),
        settingsRepository.settings,
        subjectDao.getAll(),
        assessmentDao.getAll(),
        _welcomeDismissed
    ) { semesters, settings, subjects, assessments, dismissed ->
        val today = LocalDate.now()
        val nonArchived = semesters.filter { !it.isArchived }
        val currentPrimary = nonArchived.firstOrNull() ?: semesters.firstOrNull()

        val active = semesters.find { it.id == settings.activeSemesterId }
            ?: currentPrimary

        // If settings didn't have activeSemesterId, initialize it
        if (active != null && settings.activeSemesterId == null) {
            settingsRepository.setActiveSemesterId(active.id)
        }

        val subjectsBySemester = subjects.groupBy { it.semesterId }
        val upcomingExamDatesBySubject = assessments.filter { !it.date.isBefore(today) }.groupBy { it.subjectId }

        val examPeriodIds = semesters.filter { sem ->
            val semSubjects = subjectsBySemester[sem.id].orEmpty()
            val hasExamsUpcoming = semSubjects.any { sub -> upcomingExamDatesBySubject[sub.id]?.isNotEmpty() == true }
            today.isAfter(sem.endDate) && hasExamsUpcoming
        }.map { it.id }.toSet()

        val isArchived = active?.isArchived == true
        val isInactive = active != null && !isArchived && currentPrimary != null && active.id != currentPrimary.id
        val showPrompt = !dismissed && semesters.size == 1 && semesters.first().name == "Semester 1" && !settings.onboardingComplete

        SemesterUiState(
            activeSemester = active,
            semesters = semesters,
            isViewingArchived = isArchived,
            isViewingInactive = isInactive,
            showWelcomePrompt = showPrompt,
            examPeriodSemesterIds = examPeriodIds,
            currentSemesterId = currentPrimary?.id
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SemesterUiState())

    fun switchSemester(semesterId: Long) {
        viewModelScope.launch {
            settingsRepository.setActiveSemesterId(semesterId)
            occurrenceRepository.regenerateUnmarkedWindow(targetSemesterId = semesterId)
        }
    }

    fun createSemester(
        name: String,
        startDate: LocalDate,
        endDate: LocalDate,
        targetPercentage: Float = 75f,
        copyRecurringRulesFrom: Long? = null,
        copyHolidaysFrom: Long? = null
    ) {
        viewModelScope.launch {
            val newId = semesterDao.insert(
                Semester(
                    name = name.trim(),
                    startDate = startDate,
                    endDate = endDate,
                    targetPercentage = targetPercentage,
                    isArchived = false,
                    createdAt = System.currentTimeMillis()
                )
            )

            // Copy recurring rules if selected
            if (copyRecurringRulesFrom != null) {
                val rules = recurringHolidayRuleDao.getForSemesterOnce(copyRecurringRulesFrom)
                val newRules = rules.map { it.copy(semesterId = newId) }
                recurringHolidayRuleDao.upsertAll(newRules)
            } else {
                // Initialize default weekend (Sunday)
                recurringHolidayRuleDao.upsert(
                    RecurringHolidayRule(
                        dayOfWeek = java.time.DayOfWeek.SUNDAY,
                        mode = com.iattend.app.core.data.db.RecurringHolidayMode.ALWAYS,
                        semesterId = newId
                    )
                )
            }

            // Copy specific holidays if selected
            if (copyHolidaysFrom != null) {
                val holidays = holidayDao.getForSemesterOnce(copyHolidaysFrom)
                val newHolidays = holidays.map { it.copy(id = 0, semesterId = newId) }
                holidayDao.insertAll(newHolidays)
            }

            // Automatically switch to the newly created semester
            settingsRepository.setActiveSemesterId(newId)
            occurrenceRepository.regenerateUnmarkedWindow(targetSemesterId = newId)
        }
    }

    fun updateSemester(semester: Semester) {
        viewModelScope.launch {
            semesterDao.update(semester)
            occurrenceRepository.regenerateUnmarkedWindow(targetSemesterId = semester.id)
        }
    }

    fun toggleArchive(semester: Semester) {
        viewModelScope.launch {
            semesterDao.update(semester.copy(isArchived = !semester.isArchived))
        }
    }

    fun deleteSemester(semesterId: Long) {
        viewModelScope.launch {
            val semester = semesterDao.getByIdOnce(semesterId) ?: return@launch
            // Clean up semester entities
            subjectDao.deleteForSemester(semesterId)
            timetableVersionDao.deleteForSemester(semesterId)
            holidayDao.deleteForSemester(semesterId)
            recurringHolidayRuleDao.deleteForSemester(semesterId)
            semesterDao.delete(semester)

            // Switch to remaining active semester
            val remaining = semesterDao.getAllOnce()
            val nextActive = remaining.firstOrNull { !it.isArchived } ?: remaining.firstOrNull()
            settingsRepository.setActiveSemesterId(nextActive?.id)
            if (nextActive != null) {
                occurrenceRepository.regenerateUnmarkedWindow(targetSemesterId = nextActive.id)
            }
        }
    }

    fun dismissWelcomePrompt() {
        _welcomeDismissed.value = true
    }
}
