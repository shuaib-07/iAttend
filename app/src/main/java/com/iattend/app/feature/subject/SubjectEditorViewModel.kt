package com.iattend.app.feature.subject

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.data.db.SubjectDao
import com.iattend.app.core.data.db.TotalMode
import com.iattend.app.core.domain.occurrence.OccurrenceRepository
import com.iattend.app.core.navigation.SubjectEditorRoute
import com.iattend.app.core.ui.nextSubjectColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class SubjectEditorState(
    val subjectId: Long = 0L,
    val code: String = "",
    val name: String = "",
    val colorArgb: Int = 0,
    val teacherName: String = "",
    val requiredPercentageOverride: String = "", // empty = use global default
    val totalMode: TotalMode = TotalMode.OPEN_ENDED,
    val knownTotalClasses: String = "",
    val trackingEndDateOverride: LocalDate? = null, // null = use global Settings end date (COMPUTED mode)
    val reminderOffsetsOverride: String? = null, // null = use Settings.classReminderDefaultOffsets
    val saved: Boolean = false
)

@HiltViewModel
class SubjectEditorViewModel @Inject constructor(
    private val subjectDao: SubjectDao,
    private val occurrenceRepository: OccurrenceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val route = savedStateHandle.toRoute<SubjectEditorRoute>()
    private val _state = MutableStateFlow(SubjectEditorState(subjectId = route.subjectId))
    val state: StateFlow<SubjectEditorState> = _state.asStateFlow()

    val isNew: Boolean get() = route.subjectId == 0L

    private var initialState = _state.value
    val isDirty: StateFlow<Boolean> = state
        .map { it.copy(saved = false) != initialState.copy(saved = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            if (isNew) {
                val existingCount = subjectDao.getAll().first().size
                _state.value = _state.value.copy(colorArgb = nextSubjectColor(existingCount))
            } else {
                subjectDao.getAll().first().find { it.id == route.subjectId }?.let { s ->
                    _state.value = SubjectEditorState(
                        subjectId = s.id,
                        code = s.code,
                        name = s.name,
                        colorArgb = s.colorArgb,
                        teacherName = s.teacherName ?: "",
                        requiredPercentageOverride = s.requiredPercentageOverride?.toString() ?: "",
                        totalMode = s.totalMode,
                        knownTotalClasses = s.knownTotalClasses?.toString() ?: "",
                        trackingEndDateOverride = s.trackingEndDateOverride,
                        reminderOffsetsOverride = s.reminderOffsetsOverride
                    )
                }
            }
            initialState = _state.value
        }
    }

    fun onCodeChange(v: String) { _state.value = _state.value.copy(code = v) }
    fun onNameChange(v: String) { _state.value = _state.value.copy(name = v) }
    fun onColorChange(v: Int) { _state.value = _state.value.copy(colorArgb = v) }
    fun onTeacherNameChange(v: String) { _state.value = _state.value.copy(teacherName = v) }
    fun onRequiredOverrideChange(v: String) { _state.value = _state.value.copy(requiredPercentageOverride = v) }
    fun onTotalModeChange(v: TotalMode) { _state.value = _state.value.copy(totalMode = v) }
    fun onKnownTotalChange(v: String) { _state.value = _state.value.copy(knownTotalClasses = v) }
    fun onTrackingEndDateOverrideChange(v: LocalDate?) { _state.value = _state.value.copy(trackingEndDateOverride = v) }
    fun onReminderOffsetsOverrideChange(v: String?) { _state.value = _state.value.copy(reminderOffsetsOverride = v) }

    fun save() {
        viewModelScope.launch {
            val s = _state.value
            val subject = Subject(
                id = s.subjectId,
                code = s.code.trim(),
                name = s.name.trim(),
                colorArgb = s.colorArgb,
                teacherName = s.teacherName.trim().ifBlank { null },
                requiredPercentageOverride = s.requiredPercentageOverride.toFloatOrNull(),
                totalMode = s.totalMode,
                knownTotalClasses = if (s.totalMode == TotalMode.KNOWN) s.knownTotalClasses.toIntOrNull() else null,
                trackingEndDateOverride = if (s.totalMode == TotalMode.COMPUTED) s.trackingEndDateOverride else null,
                reminderOffsetsOverride = s.reminderOffsetsOverride
            )
            val savedId = if (isNew) subjectDao.insert(subject) else { subjectDao.update(subject); subject.id }
            occurrenceRepository.regenerateUnmarkedWindow()
            _state.value = _state.value.copy(saved = true, subjectId = savedId)
        }
    }

    fun delete() {
        if (isNew) return
        viewModelScope.launch {
            subjectDao.getAll().first().find { it.id == route.subjectId }?.let { s ->
                subjectDao.delete(s)
            }
            occurrenceRepository.regenerateUnmarkedWindow()
            _state.value = _state.value.copy(saved = true)
        }
    }
}
