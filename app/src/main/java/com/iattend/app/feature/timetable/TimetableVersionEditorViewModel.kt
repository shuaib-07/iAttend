package com.iattend.app.feature.timetable

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.SavedStateHandle
import androidx.room.withTransaction
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.iattend.app.core.data.db.ClassType
import com.iattend.app.core.data.db.AppDatabase
import com.iattend.app.core.data.db.RecurringHolidayMode
import com.iattend.app.core.data.db.RecurringHolidayRuleDao
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.data.db.SubjectDao
import com.iattend.app.core.data.db.TimetableSlot
import com.iattend.app.core.data.db.TimetableSlotDao
import com.iattend.app.core.data.db.TimetableVersion
import com.iattend.app.core.data.db.TimetableVersionDao
import com.iattend.app.core.domain.occurrence.OccurrenceRepository
import com.iattend.app.core.navigation.TimetableVersionEditorRoute
import com.iattend.app.widget.UpcomingClassesWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class DraftSlot(
    val localId: Int,
    val subjectId: Long?,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val classCount: Int,
    val roomNumber: String? = null,
    val classType: ClassType = ClassType.LECTURE,
    val roomNumberOverridden: Boolean = false,
    val setSubjectDefaultRoom: Boolean = false
)

sealed class EditorNavEvent {
    data object Back : EditorNavEvent()
    data class ToBackdatedFill(val versionId: Long) : EditorNavEvent()
}

/** A slot's start/end time was edited - old occurrences generated from it (any status) get retroactively corrected. */
data class PendingTimeCorrection(
    val subjectId: Long,
    val dayOfWeek: DayOfWeek,
    val oldStart: LocalTime,
    val oldEnd: LocalTime,
    val newStart: LocalTime,
    val newEnd: LocalTime
)

data class TimetableVersionEditorState(
    val label: String = "",
    val effectiveFrom: LocalDate = LocalDate.now(),
    val slots: List<DraftSlot> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val hasOverlap: Boolean = false,
    val holidayDays: Set<DayOfWeek> = emptySet(),
    val knownRooms: List<String> = emptyList(),
    val message: String? = null,
    val pendingCorrections: List<PendingTimeCorrection> = emptyList()
)

@HiltViewModel
class TimetableVersionEditorViewModel @Inject constructor(
    private val db: AppDatabase,
    private val timetableVersionDao: TimetableVersionDao,
    private val timetableSlotDao: TimetableSlotDao,
    private val subjectDao: SubjectDao,
    private val recurringHolidayRuleDao: RecurringHolidayRuleDao,
    private val occurrenceRepository: OccurrenceRepository,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val route = savedStateHandle.toRoute<TimetableVersionEditorRoute>()
    val isNew: Boolean get() = route.versionId == 0L

    private val _state = MutableStateFlow(TimetableVersionEditorState())
    val state: StateFlow<TimetableVersionEditorState> = _state.asStateFlow()

    private var nextLocalId = 0
    private val _navEvents = Channel<EditorNavEvent>(Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

    private data class Snapshot(val label: String, val effectiveFrom: LocalDate, val slots: List<DraftSlot>)
    private var initialSnapshot = Snapshot(_state.value.label, _state.value.effectiveFrom, _state.value.slots)
    val isDirty: StateFlow<Boolean> = state
        .map { Snapshot(it.label, it.effectiveFrom, it.slots) != initialSnapshot }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            val subjects = subjectDao.getAll().first()
            var label = ""
            var effectiveFrom = LocalDate.now()
            var slots = emptyList<DraftSlot>()
            if (!isNew) {
                timetableVersionDao.getById(route.versionId)?.let {
                    label = it.label ?: ""
                    effectiveFrom = it.effectiveFrom
                }
                slots = timetableSlotDao.getForVersionOnce(route.versionId).map {
                    DraftSlot(nextLocalId++, it.subjectId, it.dayOfWeek, it.startTime, it.endTime, it.classCount, it.roomNumber, it.classType, it.roomNumberOverridden)
                }
            }
            _state.value = _state.value.copy(
                label = label,
                effectiveFrom = effectiveFrom,
                slots = slots,
                subjects = subjects
            )
            initialSnapshot = Snapshot(label, effectiveFrom, slots)
        }
        viewModelScope.launch {
            recurringHolidayRuleDao.getAll().collect { rules ->
                val always = rules.filter { it.mode == RecurringHolidayMode.ALWAYS }.map { it.dayOfWeek }.toSet()
                _state.value = _state.value.copy(holidayDays = always)
            }
        }
        viewModelScope.launch {
            timetableSlotDao.getDistinctRoomNumbers().collect { rooms ->
                _state.value = _state.value.copy(knownRooms = rooms)
            }
        }
    }

    fun onLabelChange(v: String) { _state.value = _state.value.copy(label = v) }
    fun onEffectiveFromChange(v: LocalDate) { _state.value = _state.value.copy(effectiveFrom = v) }

    fun addSlot(subjectId: Long, dayOfWeek: DayOfWeek, startTime: LocalTime, endTime: LocalTime, classCount: Int, roomNumber: String?, classType: ClassType, roomNumberOverridden: Boolean, setSubjectDefaultRoom: Boolean) {
        val slot = DraftSlot(nextLocalId++, subjectId, dayOfWeek, startTime, endTime, classCount, roomNumber?.trim()?.ifBlank { null }.takeIf { roomNumberOverridden }, classType, roomNumberOverridden, setSubjectDefaultRoom)
        val existing = _state.value.slots.map {
            if (setSubjectDefaultRoom && it.subjectId == subjectId) it.copy(setSubjectDefaultRoom = false) else it
        }
        val updated = existing + slot
        _state.value = _state.value.copy(slots = updated, hasOverlap = computeOverlap(updated))
    }

    fun updateSlot(
        localId: Int,
        subjectId: Long,
        dayOfWeek: DayOfWeek,
        startTime: LocalTime,
        endTime: LocalTime,
        classCount: Int,
        roomNumber: String?,
        classType: ClassType,
        roomNumberOverridden: Boolean,
        setSubjectDefaultRoom: Boolean
    ) {
        val old = _state.value.slots.find { it.localId == localId } ?: return
        val oldSubjectId = old.subjectId ?: return
        val updated = old.copy(
            subjectId = subjectId,
            dayOfWeek = dayOfWeek,
            startTime = startTime,
            endTime = endTime,
            classCount = classCount,
            roomNumber = roomNumber?.trim()?.ifBlank { null }.takeIf { roomNumberOverridden },
            classType = classType,
            roomNumberOverridden = roomNumberOverridden,
            setSubjectDefaultRoom = setSubjectDefaultRoom
        )
        val updatedSlots = _state.value.slots.map {
            when {
                it.localId == localId -> updated
                setSubjectDefaultRoom && it.subjectId == subjectId -> it.copy(setSubjectDefaultRoom = false)
                else -> it
            }
        }
        val corrections = if (old.startTime != startTime || old.endTime != endTime) {
            _state.value.pendingCorrections + PendingTimeCorrection(oldSubjectId, old.dayOfWeek, old.startTime, old.endTime, startTime, endTime)
        } else {
            _state.value.pendingCorrections
        }
        _state.value = _state.value.copy(slots = updatedSlots, hasOverlap = computeOverlap(updatedSlots), pendingCorrections = corrections)
    }

    fun removeSlot(localId: Int) {
        val updated = _state.value.slots.filterNot { it.localId == localId }
        _state.value = _state.value.copy(slots = updated, hasOverlap = computeOverlap(updated))
    }

    fun notifyHolidayDay() {
        _state.value = _state.value.copy(message = "Update or remove this day from Recurring Holidays to add slots")
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }

    fun duplicateDay(from: DayOfWeek, to: Set<DayOfWeek>) {
        val sourceSlots = _state.value.slots.filter { it.dayOfWeek == from }
        if (sourceSlots.isEmpty() || to.isEmpty()) return
        val copies = to.flatMap { day -> sourceSlots.map { it.copy(localId = nextLocalId++, dayOfWeek = day, setSubjectDefaultRoom = false) } }
        val updated = _state.value.slots + copies
        _state.value = _state.value.copy(slots = updated, hasOverlap = computeOverlap(updated))
    }

    fun duplicateSlot(slot: DraftSlot, to: Set<DayOfWeek>) {
        if (to.isEmpty()) return
        val copies = to.map { day -> slot.copy(localId = nextLocalId++, dayOfWeek = day, setSubjectDefaultRoom = false) }
        val updated = _state.value.slots + copies
        _state.value = _state.value.copy(slots = updated, hasOverlap = computeOverlap(updated))
    }

    private fun computeOverlap(slots: List<DraftSlot>): Boolean {
        val byDay = slots.groupBy { it.dayOfWeek }
        return byDay.values.any { daySlots ->
            daySlots.sortedBy { it.startTime }.zipWithNext().any { (a, b) -> a.endTime > b.startTime }
        }
    }

    fun save() {
        viewModelScope.launch {
            val s = _state.value
            val changedDefaultRooms = mutableMapOf<Long, String?>()
            val versionId = db.withTransaction {
                val id = if (isNew) {
                    timetableVersionDao.insert(TimetableVersion(label = s.label.ifBlank { null }, effectiveFrom = s.effectiveFrom))
                } else {
                    timetableVersionDao.update(TimetableVersion(id = route.versionId, label = s.label.ifBlank { null }, effectiveFrom = s.effectiveFrom))
                    route.versionId
                }
                s.slots.filter { it.setSubjectDefaultRoom && it.subjectId != null }.forEach { slot ->
                    val subject = subjectDao.getByIdOnce(slot.subjectId!!) ?: return@forEach
                    val newDefault = if (slot.roomNumberOverridden) slot.roomNumber else subject.defaultRoomNumber
                    if (subject.defaultRoomNumber != newDefault) {
                        subjectDao.update(subject.copy(defaultRoomNumber = newDefault))
                        changedDefaultRooms[subject.id] = newDefault
                    }
                }
                timetableSlotDao.deleteAllForVersion(id)
                s.slots.forEach { slot ->
                    if (slot.subjectId != null) timetableSlotDao.insert(
                        TimetableSlot(
                            timetableVersionId = id,
                            subjectId = slot.subjectId,
                            dayOfWeek = slot.dayOfWeek,
                            startTime = slot.startTime,
                            endTime = slot.endTime,
                            classCount = slot.classCount,
                            roomNumber = slot.roomNumber,
                            classType = slot.classType,
                            roomNumberOverridden = slot.roomNumberOverridden
                        )
                    )
                }
                id
            }

            changedDefaultRooms.forEach { (subjectId, room) -> occurrenceRepository.refreshInheritedRooms(subjectId, room) }

            s.pendingCorrections.forEach { c ->
                occurrenceRepository.fixOccurrenceTimes(c.subjectId, c.dayOfWeek, c.oldStart, c.oldEnd, c.newStart, c.newEnd)
            }
            occurrenceRepository.regenerateUnmarkedWindow()
            UpcomingClassesWidget().updateAll(context)

            _navEvents.send(
                if (s.effectiveFrom.isBefore(LocalDate.now())) {
                    EditorNavEvent.ToBackdatedFill(versionId)
                } else {
                    EditorNavEvent.Back
                }
            )
        }
    }
}
