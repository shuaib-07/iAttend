package com.iattend.app.feature.holidays

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iattend.app.core.data.db.Holiday
import com.iattend.app.core.data.db.HolidayDao
import com.iattend.app.core.domain.occurrence.OccurrenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HolidayFormState(
    val editingId: Long? = null,
    val isRange: Boolean = false,
    val date: LocalDate = LocalDate.now(),
    val fromDate: LocalDate = LocalDate.now(),
    val toDate: LocalDate = LocalDate.now(),
    val reason: String = ""
)

@HiltViewModel
class HolidayListViewModel @Inject constructor(
    private val holidayDao: HolidayDao,
    private val occurrenceRepository: OccurrenceRepository
) : ViewModel() {
    val holidays: StateFlow<List<Holiday>> = holidayDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _form = MutableStateFlow(HolidayFormState())
    val form: StateFlow<HolidayFormState> = _form.asStateFlow()
    val isDirty: StateFlow<Boolean> = form
        .map { it != HolidayFormState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun onRangeToggle(isRange: Boolean) { _form.update { it.copy(isRange = isRange) } }
    fun onDateChange(date: LocalDate) { _form.update { it.copy(date = date) } }
    fun onFromDateChange(date: LocalDate) {
        _form.update { it.copy(fromDate = date, toDate = if (date.isAfter(it.toDate)) date else it.toDate) }
    }
    fun onToDateChange(date: LocalDate) { _form.update { it.copy(toDate = date) } }
    fun onReasonChange(value: String) { _form.update { it.copy(reason = value) } }

    fun startEdit(holiday: Holiday) {
        _form.value = HolidayFormState(
            editingId = holiday.id,
            isRange = holiday.startDate != holiday.endDate,
            date = holiday.startDate,
            fromDate = holiday.startDate,
            toDate = holiday.endDate,
            reason = holiday.label
        )
    }

    fun cancelEdit() { _form.value = HolidayFormState() }

    fun save() {
        viewModelScope.launch {
            val f = _form.value
            val start = if (f.isRange) f.fromDate else f.date
            val end = if (f.isRange) f.toDate else f.date
            val holiday = Holiday(id = f.editingId ?: 0, startDate = start, endDate = end, label = f.reason.trim())
            if (f.editingId == null) holidayDao.insert(holiday) else holidayDao.update(holiday)
            occurrenceRepository.regenerateUnmarkedWindow()
            _form.value = HolidayFormState()
        }
    }

    fun delete(holiday: Holiday) {
        viewModelScope.launch {
            holidayDao.delete(holiday)
            occurrenceRepository.regenerateUnmarkedWindow()
            if (_form.value.editingId == holiday.id) _form.value = HolidayFormState()
        }
    }
}
