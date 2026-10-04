package com.iattend.app.feature.holidays

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iattend.app.core.data.db.Holiday
import com.iattend.app.core.data.db.HolidayDao
import com.iattend.app.core.data.db.RecurringHolidayMode
import com.iattend.app.core.data.db.RecurringHolidayRule
import com.iattend.app.core.data.db.RecurringHolidayRuleDao
import com.iattend.app.core.domain.occurrence.OccurrenceGenerator
import com.iattend.app.core.domain.occurrence.OccurrenceRepository
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
import java.time.YearMonth
import javax.inject.Inject

data class HolidayFormState(
    val editingId: Long? = null,
    val isRange: Boolean = false,
    val date: LocalDate = LocalDate.now(),
    val fromDate: LocalDate = LocalDate.now(),
    val toDate: LocalDate = LocalDate.now(),
    val reason: String = ""
)

sealed interface DayHolidayInfo {
    data class CustomHoliday(val holiday: Holiday, val isRange: Boolean) : DayHolidayInfo
    data class Recurring(val rule: RecurringHolidayRule) : DayHolidayInfo
    data object None : DayHolidayInfo
}

@HiltViewModel
class HolidayListViewModel @Inject constructor(
    private val holidayDao: HolidayDao,
    private val recurringHolidayRuleDao: RecurringHolidayRuleDao,
    private val occurrenceRepository: OccurrenceRepository
) : ViewModel() {
    val holidays: StateFlow<List<Holiday>> = holidayDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringRules: StateFlow<List<RecurringHolidayRule>> = recurringHolidayRuleDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _displayedMonth = MutableStateFlow(YearMonth.now())
    val displayedMonth: StateFlow<YearMonth> = _displayedMonth.asStateFlow()

    private val _form = MutableStateFlow(HolidayFormState())
    val form: StateFlow<HolidayFormState> = _form.asStateFlow()

    val isDirty: StateFlow<Boolean> = form
        .map { it != HolidayFormState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val selectedDayHolidayInfo: StateFlow<DayHolidayInfo> = combine(
        _selectedDate,
        holidays,
        recurringRules
    ) { date, holidayList, rules ->
        val holiday = holidayList.firstOrNull { date in it.startDate..it.endDate }
        if (holiday != null) {
            DayHolidayInfo.CustomHoliday(holiday, holiday.startDate != holiday.endDate)
        } else {
            val rule = rules.firstOrNull { it.dayOfWeek == date.dayOfWeek }
            val isRecurring = rule != null && when (rule.mode) {
                RecurringHolidayMode.NONE -> false
                RecurringHolidayMode.ALWAYS -> true
                RecurringHolidayMode.PATTERN -> {
                    val bit = 1 shl (OccurrenceGenerator.nthWeekdayOfMonth(date) - 1)
                    rule.weeksOfMonth and bit != 0
                }
            }
            if (isRecurring && rule != null) {
                DayHolidayInfo.Recurring(rule)
            } else {
                DayHolidayInfo.None
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DayHolidayInfo.None)

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        _displayedMonth.value = YearMonth.from(date)
    }

    fun previousMonth() {
        val prev = _displayedMonth.value.minusMonths(1)
        _displayedMonth.value = prev
        _selectedDate.value = prev.atDay(1)
    }

    fun nextMonth() {
        val next = _displayedMonth.value.plusMonths(1)
        _displayedMonth.value = next
        _selectedDate.value = next.atDay(1)
    }

    fun prefillForSelectedDate() {
        val date = _selectedDate.value
        _form.value = HolidayFormState(
            editingId = null,
            isRange = false,
            date = date,
            fromDate = date,
            toDate = date,
            reason = ""
        )
    }

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
