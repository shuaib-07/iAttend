package com.iattend.app.feature.assessment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.iattend.app.core.data.db.Assessment
import com.iattend.app.core.data.db.AssessmentAttemptStatus
import com.iattend.app.core.data.db.AssessmentDao
import com.iattend.app.core.data.db.AssessmentType
import com.iattend.app.core.data.db.ClassOccurrence
import com.iattend.app.core.data.db.ClassOccurrenceDao
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.core.data.db.SubjectDao
import com.iattend.app.core.data.db.WeightageSystem
import com.iattend.app.core.datastore.SettingsRepository
import com.iattend.app.core.navigation.AssessmentEditorRoute
import com.iattend.app.core.notifications.AssessmentReminderScheduler
import com.iattend.app.core.notifications.ReminderOffset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class AssessmentEditorState(
    val assessmentId: Long = 0L,
    val subjectId: Long = 0L,
    val subjectName: String = "",
    val subjectGradingSystem: WeightageSystem? = null,
    val subjectTotalWeightedMarks: Double? = null,
    val type: AssessmentType = AssessmentType.EXAM,
    val date: LocalDate = LocalDate.now(),
    val startTime: LocalTime = LocalTime.of(9, 0),
    val endTime: LocalTime = LocalTime.of(10, 0),
    val totalMarks: String = "",
    val obtainedMarks: String = "",
    val weight: String = "",
    val portions: String = "",
    val title: String = "",
    val reminderOffsets: List<Int> = emptyList(),
    val attemptStatus: AssessmentAttemptStatus = AssessmentAttemptStatus.PENDING,
    val isAttemptOverridden: Boolean = false,
    val linkedOccurrenceId: Long? = null,
    val needsRescheduling: Boolean = false,
    val candidateClasses: List<ClassOccurrence> = emptyList(),
    val showScoreClearConfirm: Boolean = false,
    val pendingAttemptStatus: AssessmentAttemptStatus? = null,
    val validationError: String? = null,
    val weightWarning: String? = null,
    val saved: Boolean = false,
    val deleted: Boolean = false
)

private fun Double?.formatDecimal(): String = this?.let {
    if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
} ?: ""

@HiltViewModel
class AssessmentEditorViewModel @Inject constructor(
    private val assessmentDao: AssessmentDao,
    private val subjectDao: SubjectDao,
    private val classOccurrenceDao: ClassOccurrenceDao,
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: AssessmentReminderScheduler,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val route = savedStateHandle.toRoute<AssessmentEditorRoute>()
    val isNew: Boolean get() = route.assessmentId == 0L

    private val _state = MutableStateFlow(AssessmentEditorState(subjectId = route.subjectId))
    val state: StateFlow<AssessmentEditorState> = _state.asStateFlow()

    private var lastAppliedDefault: List<Int> = emptyList()
    private var otherAssessmentsWeightSum: Double = 0.0

    init {
        viewModelScope.launch {
            val subject = subjectDao.getByIdOnce(route.subjectId)
            val candidateClasses = classOccurrenceDao.getForSubjectOnce(route.subjectId)
                .sortedWith(compareBy({ it.date }, { it.startTime }))

            val allSubjectAssessments = assessmentDao.getAllOnce().filter { it.subjectId == route.subjectId }
            otherAssessmentsWeightSum = allSubjectAssessments
                .filter { it.id != route.assessmentId }
                .mapNotNull { it.weight }
                .sum()

            if (isNew) {
                val default = ReminderOffset.parse(settingsRepository.settings.first().examReminderDefaultOffsets)
                lastAppliedDefault = default
                _state.value = _state.value.copy(
                    subjectName = subject?.name ?: "",
                    subjectGradingSystem = subject?.gradingSystem,
                    subjectTotalWeightedMarks = subject?.totalWeightedMarks,
                    candidateClasses = candidateClasses,
                    reminderOffsets = default
                )
            } else {
                assessmentDao.getByIdOnce(route.assessmentId)?.let { a ->
                    _state.value = AssessmentEditorState(
                        assessmentId = a.id,
                        subjectId = a.subjectId,
                        subjectName = subject?.name ?: "",
                        subjectGradingSystem = subject?.gradingSystem,
                        subjectTotalWeightedMarks = subject?.totalWeightedMarks,
                        type = a.type,
                        date = a.date,
                        startTime = a.startTime,
                        endTime = a.endTime,
                        totalMarks = a.totalMarks.formatDecimal(),
                        obtainedMarks = a.obtainedMarks.formatDecimal(),
                        weight = a.weight.formatDecimal(),
                        portions = a.portions ?: "",
                        title = a.title ?: "",
                        reminderOffsets = ReminderOffset.parse(a.reminderOffsets),
                        attemptStatus = a.attemptStatus,
                        isAttemptOverridden = a.isAttemptOverridden,
                        linkedOccurrenceId = a.linkedOccurrenceId,
                        needsRescheduling = a.needsRescheduling,
                        candidateClasses = candidateClasses
                    )
                }
            }
            checkWeightWarning(_state.value.weight)
        }
    }

    fun onTypeChange(type: AssessmentType) {
        viewModelScope.launch {
            val stillDefault = _state.value.reminderOffsets == lastAppliedDefault
            val updatedLinkedId = if (type == AssessmentType.EXAM) null else _state.value.linkedOccurrenceId
            _state.value = _state.value.copy(
                type = type,
                linkedOccurrenceId = updatedLinkedId,
                needsRescheduling = if (type == AssessmentType.EXAM) false else _state.value.needsRescheduling
            )
            if (stillDefault) {
                val default = ReminderOffset.parse(settingsRepository.settings.first().examReminderDefaultOffsets)
                lastAppliedDefault = default
                _state.value = _state.value.copy(reminderOffsets = default)
            }
        }
    }

    fun onDateChange(v: LocalDate) { _state.value = _state.value.copy(date = v) }

    fun onStartTimeChange(v: LocalTime) {
        val s = _state.value
        val gapMinutes = java.time.Duration.between(s.startTime, s.endTime).toMinutes().let { if (it > 0) it else 60L }
        val newEnd = if (!v.isBefore(s.endTime)) v.plusMinutes(gapMinutes) else s.endTime
        _state.value = s.copy(startTime = v, endTime = newEnd)
    }

    fun onEndTimeChange(v: LocalTime) {
        val s = _state.value
        _state.value = s.copy(endTime = if (v.isAfter(s.startTime)) v else s.startTime.plusMinutes(30))
    }

    fun onTotalMarksChange(v: String) {
        val filtered = v.filter { it.isDigit() || it == '.' }
        _state.value = _state.value.copy(totalMarks = filtered, validationError = null)
    }

    fun onObtainedMarksChange(v: String) {
        val filtered = v.filter { it.isDigit() || it == '.' }
        _state.value = _state.value.copy(obtainedMarks = filtered, validationError = null)
    }

    fun onWeightChange(v: String) {
        val filtered = v.filter { it.isDigit() || it == '.' }
        _state.value = _state.value.copy(weight = filtered)
        checkWeightWarning(filtered)
    }

    private fun checkWeightWarning(weightStr: String) {
        val w = weightStr.toDoubleOrNull() ?: 0.0
        val total = otherAssessmentsWeightSum + w
        val s = _state.value
        val warning = when (s.subjectGradingSystem) {
            WeightageSystem.PERCENTAGE -> {
                if (total > 100.0) "Combined weights (${total.formatDecimal()}%) exceed 100% (saving permitted)." else null
            }
            WeightageSystem.WEIGHTED_MARKS -> {
                val max = s.subjectTotalWeightedMarks
                if (max != null && total > max) "Combined weights (${total.formatDecimal()}) exceed configured total (${max.formatDecimal()}) (saving permitted)." else null
            }
            null -> null
        }
        _state.value = _state.value.copy(weightWarning = warning)
    }

    fun onLinkOccurrenceChange(occurrenceId: Long?) {
        val s = _state.value
        if (occurrenceId == null) {
            _state.value = s.copy(
                linkedOccurrenceId = null,
                needsRescheduling = false,
                isAttemptOverridden = true // Decoupled: keep current status as manual
            )
            return
        }

        val occ = s.candidateClasses.find { it.id == occurrenceId } ?: return
        val autoStatus = when (occ.status) {
            OccurrenceStatus.PRESENT -> AssessmentAttemptStatus.ATTEMPTED
            OccurrenceStatus.ABSENT -> AssessmentAttemptStatus.MISSED
            OccurrenceStatus.UNMARKED -> AssessmentAttemptStatus.PENDING
            OccurrenceStatus.CANCELLED -> AssessmentAttemptStatus.PENDING
        }
        val needsResched = (occ.status == OccurrenceStatus.CANCELLED)

        _state.value = s.copy(
            linkedOccurrenceId = occ.id,
            date = occ.date,
            startTime = occ.startTime ?: s.startTime,
            endTime = occ.endTime ?: s.endTime,
            attemptStatus = if (!s.isAttemptOverridden) autoStatus else s.attemptStatus,
            needsRescheduling = needsResched
        )
    }

    fun onAttemptStatusChange(newStatus: AssessmentAttemptStatus) {
        val s = _state.value
        if (s.obtainedMarks.isNotBlank() && (newStatus == AssessmentAttemptStatus.MISSED || newStatus == AssessmentAttemptStatus.PENDING)) {
            _state.value = s.copy(showScoreClearConfirm = true, pendingAttemptStatus = newStatus)
        } else {
            _state.value = s.copy(
                attemptStatus = newStatus,
                isAttemptOverridden = s.linkedOccurrenceId != null
            )
        }
    }

    fun confirmScoreClear() {
        val s = _state.value
        _state.value = s.copy(
            showScoreClearConfirm = false,
            obtainedMarks = "",
            attemptStatus = s.pendingAttemptStatus ?: s.attemptStatus,
            isAttemptOverridden = s.linkedOccurrenceId != null,
            pendingAttemptStatus = null
        )
    }

    fun dismissScoreClear() {
        _state.value = _state.value.copy(showScoreClearConfirm = false, pendingAttemptStatus = null)
    }

    fun resetToAutomaticStatus() {
        val s = _state.value
        val linkedId = s.linkedOccurrenceId ?: return
        val occ = s.candidateClasses.find { it.id == linkedId } ?: return

        val autoStatus = when (occ.status) {
            OccurrenceStatus.PRESENT -> AssessmentAttemptStatus.ATTEMPTED
            OccurrenceStatus.ABSENT -> AssessmentAttemptStatus.MISSED
            OccurrenceStatus.UNMARKED -> AssessmentAttemptStatus.PENDING
            OccurrenceStatus.CANCELLED -> AssessmentAttemptStatus.PENDING
        }

        if (s.obtainedMarks.isNotBlank() && (autoStatus == AssessmentAttemptStatus.MISSED || autoStatus == AssessmentAttemptStatus.PENDING)) {
            _state.value = s.copy(showScoreClearConfirm = true, pendingAttemptStatus = autoStatus)
        } else {
            _state.value = s.copy(
                attemptStatus = autoStatus,
                isAttemptOverridden = false,
                needsRescheduling = (occ.status == OccurrenceStatus.CANCELLED)
            )
        }
    }

    fun onPortionsChange(v: String) { _state.value = _state.value.copy(portions = v) }
    fun onTitleChange(v: String) { _state.value = _state.value.copy(title = v) }
    fun onReminderOffsetsChange(v: List<Int>) { _state.value = _state.value.copy(reminderOffsets = v) }

    fun save() {
        viewModelScope.launch {
            val s = _state.value

            val total = s.totalMarks.toDoubleOrNull()
            if (s.totalMarks.isNotBlank() && (total == null || total <= 0.0)) {
                _state.value = s.copy(validationError = "Total marks must be a positive number.")
                return@launch
            }

            val obtained = s.obtainedMarks.toDoubleOrNull()
            if (s.obtainedMarks.isNotBlank()) {
                if (total == null) {
                    _state.value = s.copy(validationError = "Obtained marks require total marks.")
                    return@launch
                }
                if (obtained == null || obtained < 0.0 || obtained > total) {
                    _state.value = s.copy(validationError = "Obtained marks must be between 0 and $total.")
                    return@launch
                }
                if (s.attemptStatus != AssessmentAttemptStatus.ATTEMPTED) {
                    _state.value = s.copy(validationError = "Entered scores require Attempted status.")
                    return@launch
                }
            }

            val weight = s.weight.toDoubleOrNull()
            if (s.weight.isNotBlank() && (weight == null || weight < 0.0)) {
                _state.value = s.copy(validationError = "Weight must be non-negative.")
                return@launch
            }

            val assessment = Assessment(
                id = s.assessmentId,
                subjectId = s.subjectId,
                type = s.type,
                date = s.date,
                startTime = s.startTime,
                endTime = s.endTime,
                totalMarks = total,
                obtainedMarks = obtained,
                weight = weight,
                attemptStatus = s.attemptStatus,
                isAttemptOverridden = s.isAttemptOverridden,
                linkedOccurrenceId = if (s.type == AssessmentType.EXAM) null else s.linkedOccurrenceId,
                needsRescheduling = if (s.type == AssessmentType.EXAM) false else s.needsRescheduling,
                portions = s.portions.trim().ifBlank { null },
                reminderOffsets = ReminderOffset.format(s.reminderOffsets),
                title = s.title.trim().ifBlank { null }
            )

            val id = if (isNew) assessmentDao.insert(assessment) else { assessmentDao.update(assessment); assessment.id }
            if (!assessment.needsRescheduling) {
                reminderScheduler.reschedule(assessment.copy(id = id))
            } else {
                reminderScheduler.cancel(id)
            }
            _state.value = _state.value.copy(saved = true)
        }
    }

    fun delete() {
        viewModelScope.launch {
            assessmentDao.getByIdOnce(_state.value.assessmentId)?.let { assessmentDao.delete(it) }
            reminderScheduler.cancel(_state.value.assessmentId)
            _state.value = _state.value.copy(deleted = true)
        }
    }
}
