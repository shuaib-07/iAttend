package com.iattend.app.feature.holidays

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iattend.app.core.data.db.RecurringHolidayMode
import com.iattend.app.core.data.db.RecurringHolidayRule
import com.iattend.app.core.data.db.RecurringHolidayRuleDao
import com.iattend.app.core.domain.occurrence.OccurrenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

@HiltViewModel
class RecurringHolidayViewModel @Inject constructor(
    private val recurringHolidayRuleDao: RecurringHolidayRuleDao,
    private val occurrenceRepository: OccurrenceRepository
) : ViewModel() {
    val rules: StateFlow<Map<DayOfWeek, RecurringHolidayRule>> = recurringHolidayRuleDao.getAll()
        .map { list -> list.associateBy { it.dayOfWeek } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setMode(day: DayOfWeek, mode: RecurringHolidayMode) {
        viewModelScope.launch {
            val existing = rules.value[day]
            recurringHolidayRuleDao.upsert(RecurringHolidayRule(day, mode, existing?.weeksOfMonth ?: 0))
            occurrenceRepository.regenerateUnmarkedWindow()
        }
    }

    /** [week] is 1-based (1st..5th occurrence of the weekday in the month). */
    fun toggleWeek(day: DayOfWeek, week: Int) {
        viewModelScope.launch {
            val existing = rules.value[day] ?: RecurringHolidayRule(day, RecurringHolidayMode.PATTERN)
            val bit = 1 shl (week - 1)
            val newMask = existing.weeksOfMonth xor bit
            recurringHolidayRuleDao.upsert(existing.copy(mode = RecurringHolidayMode.PATTERN, weeksOfMonth = newMask))
            occurrenceRepository.regenerateUnmarkedWindow()
        }
    }
}
