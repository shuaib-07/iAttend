package com.iattend.app.feature.assessment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.iattend.app.core.data.db.Assessment
import com.iattend.app.core.data.db.AssessmentDao
import com.iattend.app.core.data.db.AssessmentType
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
    val type: AssessmentType = AssessmentType.EXAM,
    val date: LocalDate = LocalDate.now(),
    val startTime: LocalTime = LocalTime.of(9, 0),
    val endTime: LocalTime = LocalTime.of(10, 0),
    val totalMarks: String = "",
    val portions: String = "",
    val title: String = "",
    val reminderOffsets: List<Int> = emptyList(),
    val saved: Boolean = false,
    val deleted: Boolean = false
)

@HiltViewModel
class AssessmentEditorViewModel @Inject constructor(
    private val assessmentDao: AssessmentDao,
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: AssessmentReminderScheduler,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val route = savedStateHandle.toRoute<AssessmentEditorRoute>()
    val isNew: Boolean get() = route.assessmentId == 0L

    private val _state = MutableStateFlow(AssessmentEditorState(subjectId = route.subjectId))
    val state: StateFlow<AssessmentEditorState> = _state.asStateFlow()

    /** Tracks the default offsets last applied automatically, so switching Test<->Exam re-applies
     * the new type's default only while the user hasn't customized it themselves. */
    private var lastAppliedDefault: List<Int> = emptyList()

    init {
        viewModelScope.launch {
            if (isNew) {
                val default = ReminderOffset.parse(settingsRepository.settings.first().examReminderDefaultOffsets)
                lastAppliedDefault = default
                _state.value = _state.value.copy(reminderOffsets = default)
            } else {
                assessmentDao.getByIdOnce(route.assessmentId)?.let { a ->
                    _state.value = AssessmentEditorState(
                        assessmentId = a.id,
                        subjectId = a.subjectId,
                        type = a.type,
                        date = a.date,
                        startTime = a.startTime,
                        endTime = a.endTime,
                        totalMarks = a.totalMarks?.toString() ?: "",
                        portions = a.portions ?: "",
                        title = a.title ?: "",
                        reminderOffsets = ReminderOffset.parse(a.reminderOffsets)
                    )
                }
            }
        }
    }

    fun onTypeChange(type: AssessmentType) {
        viewModelScope.launch {
            val stillDefault = _state.value.reminderOffsets == lastAppliedDefault
            _state.value = _state.value.copy(type = type)
            if (stillDefault) {
                // Test and Exam currently share one default offsets setting (Settings.examReminderDefaultOffsets) -
                // only exam defaults were specified when this was grilled.
                val default = ReminderOffset.parse(settingsRepository.settings.first().examReminderDefaultOffsets)
                lastAppliedDefault = default
                _state.value = _state.value.copy(reminderOffsets = default)
            }
        }
    }

    fun onDateChange(v: LocalDate) { _state.value = _state.value.copy(date = v) }

    /** Keeps endTime after startTime - bumping it forward by the same gap it had before, rather
     * than surfacing a validation error, when a new start would otherwise land on or after it. */
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

    fun onTotalMarksChange(v: String) { _state.value = _state.value.copy(totalMarks = v.filter(Char::isDigit)) }
    fun onPortionsChange(v: String) { _state.value = _state.value.copy(portions = v) }
    fun onTitleChange(v: String) { _state.value = _state.value.copy(title = v) }
    fun onReminderOffsetsChange(v: List<Int>) { _state.value = _state.value.copy(reminderOffsets = v) }

    fun save() {
        viewModelScope.launch {
            val s = _state.value
            val assessment = Assessment(
                id = s.assessmentId,
                subjectId = s.subjectId,
                type = s.type,
                date = s.date,
                startTime = s.startTime,
                endTime = s.endTime,
                totalMarks = s.totalMarks.toIntOrNull(),
                portions = s.portions.trim().ifBlank { null },
                reminderOffsets = ReminderOffset.format(s.reminderOffsets),
                title = s.title.trim().ifBlank { null }
            )
            val id = if (isNew) assessmentDao.insert(assessment) else { assessmentDao.update(assessment); assessment.id }
            reminderScheduler.reschedule(assessment.copy(id = id))
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
