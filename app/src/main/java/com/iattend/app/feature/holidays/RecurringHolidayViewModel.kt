package com.iattend.app.feature.holidays

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iattend.app.core.data.db.RecurringHolidayMode
import com.iattend.app.core.data.db.RecurringHolidayRule
import com.iattend.app.core.data.db.RecurringHolidayRuleDao
import com.iattend.app.core.datastore.SettingsRepository
import com.iattend.app.core.domain.occurrence.OccurrenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

@HiltViewModel
class RecurringHolidayViewModel @Inject constructor(
    private val recurringHolidayRuleDao: RecurringHolidayRuleDao,
    private val occurrenceRepository: OccurrenceRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val rules: StateFlow<Map<DayOfWeek, RecurringHolidayRule>> = combine(
        recurringHolidayRuleDao.getAll(),
        settingsRepository.settings
    ) { all, settings ->
        val activeSemId = settings.activeSemesterId
        val filtered = if (activeSemId != null) all.filter { it.semesterId == activeSemId } else all
        filtered.associateBy { it.dayOfWeek }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setMode(day: DayOfWeek, mode: RecurringHolidayMode) {
        viewModelScope.launch {
            val semId = settingsRepository.settings.first().activeSemesterId ?: 1L
            val existing = rules.value[day]
            recurringHolidayRuleDao.upsert(
                RecurringHolidayRule(
                    dayOfWeek = day,
                    mode = mode,
                    weeksOfMonth = existing?.weeksOfMonth ?: 0,
                    semesterId = semId
                )
            )
            occurrenceRepository.regenerateUnmarkedWindow(targetSemesterId = semId)
        }
    }

    /** [week] is 1-based (1st..5th occurrence of the weekday in the month). */
    fun toggleWeek(day: DayOfWeek, week: Int) {
        viewModelScope.launch {
            val semId = settingsRepository.settings.first().activeSemesterId ?: 1L
            val existing = rules.value[day] ?: RecurringHolidayRule(dayOfWeek = day, mode = RecurringHolidayMode.PATTERN, semesterId = semId)
            val bit = 1 shl (week - 1)
            val newMask = existing.weeksOfMonth xor bit
            recurringHolidayRuleDao.upsert(
                existing.copy(
                    mode = RecurringHolidayMode.PATTERN,
                    weeksOfMonth = newMask,
                    semesterId = semId
                )
            )
            occurrenceRepository.regenerateUnmarkedWindow(targetSemesterId = semId)
        }
    }
}
