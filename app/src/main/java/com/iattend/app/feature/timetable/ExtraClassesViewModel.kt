package com.iattend.app.feature.timetable

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.iattend.app.core.data.db.ClassOccurrence
import com.iattend.app.core.data.db.ClassOccurrenceDao
import com.iattend.app.core.data.db.ClassType
import com.iattend.app.core.data.db.OccurrenceSource
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.data.db.SubjectDao
import com.iattend.app.core.navigation.ExtraClassesRoute
import com.iattend.app.core.notifications.ClassReminderScheduler
import android.content.Context
import androidx.glance.appwidget.updateAll
import com.iattend.app.widget.UpcomingClassesWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class UpcomingExtra(val occurrence: ClassOccurrence, val subjectName: String)

data class ExtraClassForm(
    val date: LocalDate = LocalDate.now(),
    val subjectId: Long? = null,
    val startTime: LocalTime = LocalTime.of(9, 0),
    val endTime: LocalTime = LocalTime.of(10, 0),
    val room: String = "",
    val classType: ClassType = ClassType.LECTURE,
    val editingOccurrenceId: Long? = null
)

@HiltViewModel
class ExtraClassesViewModel @Inject constructor(
    private val classOccurrenceDao: ClassOccurrenceDao,
    private val subjectDao: SubjectDao,
    private val reminderScheduler: ClassReminderScheduler,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val route = savedStateHandle.toRoute<ExtraClassesRoute>()

    private val _form = MutableStateFlow(
        ExtraClassForm(date = route.prefillDate?.let(LocalDate::parse) ?: LocalDate.now())
    )
    val form: StateFlow<ExtraClassForm> = _form.asStateFlow()

    val subjects: StateFlow<List<Subject>> = subjectDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _editingOriginalForm = MutableStateFlow<ExtraClassForm?>(null)

    val isDirty: StateFlow<Boolean> = combine(_form, _editingOriginalForm) { form, original ->
        if (form.editingOccurrenceId == null || original == null) {
            false
        } else {
            form != original
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val upcomingExtras: StateFlow<List<UpcomingExtra>> = combine(
        classOccurrenceDao.getAll(),
        subjectDao.getAll()
    ) { occurrences, subjects ->
        val today = LocalDate.now()
        occurrences
            .filter { it.source == OccurrenceSource.EXTRA && !it.date.isBefore(today) }
            .sortedWith(compareBy({ it.date }, { it.startTime }))
            .map { occ -> UpcomingExtra(occ, subjects.find { it.id == occ.subjectId }?.name ?: "?") }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            if (route.editOccurrenceId != null) {
                val occ = classOccurrenceDao.getByIdOnce(route.editOccurrenceId)
                if (occ != null) {
                    startEdit(occ)
                }
            }
            subjects.collect { list ->
                if (_form.value.subjectId == null) _form.update { it.copy(subjectId = list.firstOrNull()?.id) }
            }
        }
    }

    fun onDateChange(date: LocalDate) { _form.update { it.copy(date = date) } }
    fun onSubjectChange(id: Long) { _form.update { it.copy(subjectId = id) } }
    fun onStartTimeChange(time: LocalTime) { _form.update { it.copy(startTime = time) } }
    fun onEndTimeChange(time: LocalTime) { _form.update { it.copy(endTime = time) } }
    fun onRoomChange(value: String) { _form.update { it.copy(room = value) } }
    fun onClassTypeChange(type: ClassType) { _form.update { it.copy(classType = type) } }

    private var editingOriginal: ClassOccurrence? = null

    fun startEdit(occurrence: ClassOccurrence) {
        editingOriginal = occurrence
        val editForm = ExtraClassForm(
            date = occurrence.date,
            subjectId = occurrence.subjectId,
            startTime = occurrence.startTime ?: LocalTime.of(9, 0),
            endTime = occurrence.endTime ?: LocalTime.of(10, 0),
            room = occurrence.roomNumber ?: "",
            classType = occurrence.classType,
            editingOccurrenceId = occurrence.id
        )
        _editingOriginalForm.value = editForm
        _form.value = editForm
    }

    fun cancelEdit() {
        editingOriginal = null
        _editingOriginalForm.value = null
        _form.update { it.copy(room = "", editingOccurrenceId = null) }
    }

    fun schedule() {
        val f = _form.value
        val subjectId = f.subjectId ?: return
        viewModelScope.launch {
            val original = editingOriginal
            if (original != null && original.id == f.editingOccurrenceId) {
                classOccurrenceDao.update(
                    original.copy(
                        subjectId = subjectId,
                        date = f.date,
                        startTime = f.startTime,
                        endTime = f.endTime,
                        roomNumber = f.room.trim().ifBlank { null },
                        classType = f.classType
                    )
                )
                editingOriginal = null
                _editingOriginalForm.value = null
            } else {
                classOccurrenceDao.insert(
                    ClassOccurrence(
                        subjectId = subjectId,
                        date = f.date,
                        startTime = f.startTime,
                        endTime = f.endTime,
                        status = OccurrenceStatus.UNMARKED,
                        source = OccurrenceSource.EXTRA,
                        timetableSlotId = null,
                        roomNumber = f.room.trim().ifBlank { null },
                        classType = f.classType
                    )
                )
            }
            _form.update { it.copy(room = "", editingOccurrenceId = null) }
            _editingOriginalForm.value = null
            reminderScheduler.scheduleTodayReminders()
            UpcomingClassesWidget().updateAll(context)
        }
    }

    fun delete(occurrence: ClassOccurrence) {
        viewModelScope.launch {
            classOccurrenceDao.delete(occurrence)
            reminderScheduler.cancelRemindersForOccurrence(occurrence.id)
            UpcomingClassesWidget().updateAll(context)
            if (editingOriginal?.id == occurrence.id) cancelEdit()
        }
    }

    fun deleteEditing() {
        val occurrenceId = _form.value.editingOccurrenceId ?: return
        viewModelScope.launch {
            val occ = editingOriginal ?: classOccurrenceDao.getByIdOnce(occurrenceId)
            if (occ != null) {
                delete(occ)
            }
        }
    }
}
