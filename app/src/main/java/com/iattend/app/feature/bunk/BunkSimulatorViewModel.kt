package com.iattend.app.feature.bunk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iattend.app.core.data.db.Assessment
import com.iattend.app.core.data.db.AssessmentDao
import com.iattend.app.core.data.db.ClassOccurrence
import com.iattend.app.core.data.db.ClassOccurrenceDao
import com.iattend.app.core.data.db.Holiday
import com.iattend.app.core.data.db.HolidayDao
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.core.data.db.RecurringHolidayRule
import com.iattend.app.core.data.db.RecurringHolidayRuleDao
import com.iattend.app.core.data.db.SemesterDao
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.data.db.SubjectDao
import com.iattend.app.core.datastore.AppSettings
import com.iattend.app.core.datastore.SettingsRepository
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

data class BunkSimulatorUiState(
    val selectedSubjectId: Long? = null, // null = Overall Semester
    val availableSubjects: List<Subject> = emptyList(),
    val targetPercentage: Float = 75f,
    val attendedClasses: Int = 0,
    val totalClassesSoFar: Int = 0,
    val totalClassesOverall: Int? = null,
    val simulateMissCount: Int = 0,
    val simulateAttendCount: Int = 0,
    val result: BunkCalculationResult = BunkCalculator.calculate(0, 0, null, 75f),
    val upcomingOpportunities: List<DayBunkOpportunity> = emptyList(),
    val plannedBunkDates: Set<LocalDate> = emptySet(),
    val plannedBunkImpact: BunkCalculationResult? = null,
    val selectedTab: Int = 0 // 0 = What-If Stepper, 1 = Day-by-Day Planner
)

@HiltViewModel
class BunkSimulatorViewModel @Inject constructor(
    private val subjectDao: SubjectDao,
    private val classOccurrenceDao: ClassOccurrenceDao,
    private val holidayDao: HolidayDao,
    private val recurringHolidayRuleDao: RecurringHolidayRuleDao,
    private val assessmentDao: AssessmentDao,
    private val settingsRepository: SettingsRepository,
    private val semesterDao: SemesterDao
) : ViewModel() {

    private val _selectedSubjectId = MutableStateFlow<Long?>(null)
    private val _targetPercentage = MutableStateFlow<Float?>(null)
    private val _simulateMiss = MutableStateFlow(0)
    private val _simulateAttend = MutableStateFlow(0)
    private val _plannedBunkDates = MutableStateFlow<Set<LocalDate>>(emptySet())
    private val _selectedTab = MutableStateFlow(0)

    private val coreDbFlow = combine(
        settingsRepository.settings,
        subjectDao.getAll(),
        classOccurrenceDao.getAll()
    ) { settings, subjects, occurrences ->
        CoreDbData(settings, subjects, occurrences)
    }

    private val calendarDbFlow = combine(
        holidayDao.getAll(),
        recurringHolidayRuleDao.getAll(),
        assessmentDao.getAll()
    ) { holidays, recurringRules, assessments ->
        CalendarDbData(holidays, recurringRules, assessments)
    }

    private val paramsFlow = combine(
        combine(_selectedSubjectId, _targetPercentage, _simulateMiss) { subjId, target, miss ->
            Triple(subjId, target, miss)
        },
        combine(_simulateAttend, _plannedBunkDates, _selectedTab) { attend, planned, tab ->
            Triple(attend, planned, tab)
        }
    ) { (subjId, target, miss), (attend, planned, tab) ->
        SimulationParams(subjId, target, miss, attend, planned, tab)
    }

    val uiState: StateFlow<BunkSimulatorUiState> = combine(
        coreDbFlow,
        calendarDbFlow,
        paramsFlow
    ) { coreDb, calendarDb, params ->
        val (settings, subjects, occurrences) = coreDb
        val (holidays, recurringRules, assessments) = calendarDb
        val activeSemId = settings.activeSemesterId
        val semesterSubjects = if (activeSemId != null) {
            subjects.filter { it.semesterId == activeSemId }
        } else subjects

        val currentSubject = semesterSubjects.find { it.id == params.subjectId }
        val targetPct = params.targetOverride
            ?: currentSubject?.requiredPercentageOverride
            ?: settings.requiredPercentageDefault

        val relevantOccurrences = when {
            params.subjectId != null -> occurrences.filter { it.subjectId == params.subjectId }
            activeSemId != null -> {
                val semSubjIds = semesterSubjects.map { it.id }.toSet()
                occurrences.filter { it.subjectId in semSubjIds }
            }
            else -> occurrences
        }

        val nonCancelled = relevantOccurrences.filter { it.status != OccurrenceStatus.CANCELLED }
        val attended = nonCancelled.filter { it.status == OccurrenceStatus.PRESENT }.sumOf { it.classCount }
        val totalSoFar = nonCancelled.filter { it.status == OccurrenceStatus.PRESENT || it.status == OccurrenceStatus.ABSENT }.sumOf { it.classCount }

        val totalOverall = when {
            params.subjectId != null -> {
                when (currentSubject?.totalMode) {
                    com.iattend.app.core.data.db.TotalMode.KNOWN -> currentSubject.knownTotalClasses
                    else -> nonCancelled.sumOf { it.classCount }.takeIf { it > 0 }
                }
            }
            else -> nonCancelled.sumOf { it.classCount }.takeIf { it > 0 }
        }

        val result = BunkCalculator.calculate(
            attended = attended,
            totalSoFar = totalSoFar,
            totalOverall = totalOverall,
            targetPercentage = targetPct,
            simulateMiss = params.simulateMiss,
            simulateAttend = params.simulateAttend
        )

        val relevantAssessments = if (params.subjectId != null) {
            assessments.filter { it.subjectId == params.subjectId }
        } else {
            val semSubjIds = semesterSubjects.map { it.id }.toSet()
            assessments.filter { it.subjectId in semSubjIds }
        }

        val upcomingOpps = BunkCalculator.analyzeUpcomingDays(
            futureOccurrences = nonCancelled,
            holidays = holidays,
            recurringHolidayRules = recurringRules,
            assessments = relevantAssessments,
            safeSkipsAvailable = result.safeSkipsNow,
            currentPercentage = result.currentPercentage,
            targetPercentage = targetPct
        )

        val plannedMissClasses = upcomingOpps.filter { it.date in params.plannedDates }.sumOf { it.classCount }
        val plannedImpact = if (params.plannedDates.isNotEmpty()) {
            BunkCalculator.calculate(
                attended = attended,
                totalSoFar = totalSoFar,
                totalOverall = totalOverall,
                targetPercentage = targetPct,
                simulateMiss = plannedMissClasses,
                simulateAttend = 0
            )
        } else null

        BunkSimulatorUiState(
            selectedSubjectId = params.subjectId,
            availableSubjects = semesterSubjects,
            targetPercentage = targetPct,
            attendedClasses = attended,
            totalClassesSoFar = totalSoFar,
            totalClassesOverall = totalOverall,
            simulateMissCount = params.simulateMiss,
            simulateAttendCount = params.simulateAttend,
            result = result,
            upcomingOpportunities = upcomingOpps,
            plannedBunkDates = params.plannedDates,
            plannedBunkImpact = plannedImpact,
            selectedTab = params.selectedTab
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BunkSimulatorUiState())

    fun selectSubject(subjectId: Long?) {
        _selectedSubjectId.value = subjectId
        _targetPercentage.value = null
        _simulateMiss.value = 0
        _simulateAttend.value = 0
        _plannedBunkDates.value = emptySet()
    }

    fun setTargetPercentage(pct: Float) {
        _targetPercentage.value = pct
    }

    fun setSimulateMiss(count: Int) {
        _simulateMiss.value = count.coerceAtLeast(0)
    }

    fun setSimulateAttend(count: Int) {
        _simulateAttend.value = count.coerceAtLeast(0)
    }

    fun togglePlannedBunkDate(date: LocalDate) {
        val current = _plannedBunkDates.value
        _plannedBunkDates.value = if (date in current) current - date else current + date
    }

    fun clearPlannedBunks() {
        _plannedBunkDates.value = emptySet()
    }

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun resetSimulation() {
        _simulateMiss.value = 0
        _simulateAttend.value = 0
        _plannedBunkDates.value = emptySet()
    }
}

private data class SimulationParams(
    val subjectId: Long?,
    val targetOverride: Float?,
    val simulateMiss: Int,
    val simulateAttend: Int,
    val plannedDates: Set<LocalDate>,
    val selectedTab: Int
)

private data class CoreDbData(
    val settings: AppSettings,
    val subjects: List<Subject>,
    val occurrences: List<ClassOccurrence>
)

private data class CalendarDbData(
    val holidays: List<Holiday>,
    val recurringRules: List<RecurringHolidayRule>,
    val assessments: List<Assessment>
)
