package com.iattend.app.feature.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.data.db.SubjectDao
import com.iattend.app.core.domain.occurrence.BackdatedFillStrategy
import com.iattend.app.core.domain.occurrence.OccurrenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FillChoice { PRESENT_ALL, ABSENT_ALL, LEAVE_UNMARKED, AGGREGATE_BASELINE }

data class SubjectFillState(
    val subject: Subject,
    val occurrenceCount: Int,
    val choice: FillChoice = FillChoice.LEAVE_UNMARKED,
    val aggregateAttended: String = ""
)

data class BackdatedFillState(
    val subjects: List<SubjectFillState> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val done: Boolean = false
)

@HiltViewModel
class BackdatedFillViewModel @Inject constructor(
    private val occurrenceRepository: OccurrenceRepository,
    private val subjectDao: SubjectDao
) : ViewModel() {
    private val _state = MutableStateFlow(BackdatedFillState())
    val state: StateFlow<BackdatedFillState> = _state.asStateFlow()

    private val _done = Channel<Unit>(Channel.BUFFERED)
    val doneEvents = _done.receiveAsFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val bySubject = occurrenceRepository.backdatedUnmarkedBySubject()
            val subjects = subjectDao.getAll().first()
            _state.value = BackdatedFillState(
                subjects = bySubject.mapNotNull { (subjectId, occurrences) ->
                    subjects.find { it.id == subjectId }?.let { SubjectFillState(it, occurrences.size) }
                },
                isLoading = false
            )
        }
    }

    fun applyPresetToAll(choice: FillChoice) {
        val label = when (choice) {
            FillChoice.PRESENT_ALL -> "Present All"
            FillChoice.ABSENT_ALL -> "Absent All"
            FillChoice.LEAVE_UNMARKED -> "Leave Unmarked"
            FillChoice.AGGREGATE_BASELINE -> "Baseline"
        }
        _state.value = _state.value.copy(subjects = _state.value.subjects.map { it.copy(choice = choice) })
        viewModelScope.launch {
            _toastEvent.emit("Applied '$label' to all subjects")
        }
    }

    fun setChoice(subjectId: Long, choice: FillChoice) {
        _state.value = _state.value.copy(
            subjects = _state.value.subjects.map { if (it.subject.id == subjectId) it.copy(choice = choice) else it }
        )
    }

    fun setAggregateAttended(subjectId: Long, value: String) {
        _state.value = _state.value.copy(
            subjects = _state.value.subjects.map { if (it.subject.id == subjectId) it.copy(aggregateAttended = value) else it }
        )
    }

    fun confirm() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            _state.value.subjects.forEach { sf ->
                val strategy = when (sf.choice) {
                    FillChoice.PRESENT_ALL -> BackdatedFillStrategy.PresentAll
                    FillChoice.ABSENT_ALL -> BackdatedFillStrategy.AbsentAll
                    FillChoice.LEAVE_UNMARKED -> BackdatedFillStrategy.LeaveUnmarked
                    FillChoice.AGGREGATE_BASELINE -> BackdatedFillStrategy.AggregateBaseline(sf.aggregateAttended.toIntOrNull() ?: 0)
                }
                occurrenceRepository.applyBackdatedFill(sf.subject.id, strategy)
            }
            _state.value = _state.value.copy(isSaving = false)
            _done.send(Unit)
        }
    }
}
