package com.iattend.app.feature.onboarding

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.data.db.SubjectDao
import com.iattend.app.core.data.db.TimetableSlot
import com.iattend.app.core.data.db.TimetableSlotDao
import com.iattend.app.core.data.db.TimetableVersion
import com.iattend.app.core.data.db.TimetableVersionDao
import com.iattend.app.core.data.db.RecurringHolidayMode
import com.iattend.app.core.data.db.RecurringHolidayRule
import com.iattend.app.core.data.db.RecurringHolidayRuleDao
import com.iattend.app.core.data.export.ExportImportRepository
import com.iattend.app.core.datastore.ProfileRepository
import com.iattend.app.core.datastore.SettingsRepository
import com.iattend.app.core.domain.occurrence.OccurrenceRepository
import com.iattend.app.core.ui.nextSubjectColor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class OnboardingDraftSlot(
    val localId: Int,
    val subjectId: Long,
    val subjectName: String,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val classCount: Int
)

data class OnboardingState(
    val hasChosenStart: Boolean = false,
    val step: Int = 0,
    val name: String = "",
    val subjects: List<Subject> = emptyList(),
    val slots: List<OnboardingDraftSlot> = emptyList(),
    val startDate: LocalDate = LocalDate.now(),
    val hasEndDate: Boolean = false,
    val endDate: LocalDate = LocalDate.now(),
    val holidayDays: Set<DayOfWeek> = setOf(DayOfWeek.SUNDAY),
    val requiredPercentage: String = "75"
)

sealed class OnboardingFinishEvent {
    data object ToHome : OnboardingFinishEvent()
    data class ToBackdatedFill(val versionId: Long) : OnboardingFinishEvent()
}

const val ONBOARDING_STEP_COUNT = 9

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val subjectDao: SubjectDao,
    private val timetableVersionDao: TimetableVersionDao,
    private val timetableSlotDao: TimetableSlotDao,
    private val recurringHolidayRuleDao: RecurringHolidayRuleDao,
    private val settingsRepository: SettingsRepository,
    private val occurrenceRepository: OccurrenceRepository,
    private val exportImportRepository: ExportImportRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var nextLocalId = 0
    private val _finishEvents = Channel<OnboardingFinishEvent>(Channel.BUFFERED)
    val finishEvents = _finishEvents.receiveAsFlow()

    fun chooseStartFresh() { _state.value = _state.value.copy(hasChosenStart = true) }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: return@launch
                exportImportRepository.import(text)
                settingsRepository.setOnboardingComplete(true)
                _finishEvents.send(OnboardingFinishEvent.ToHome)
            } catch (e: Exception) {
                _message.value = "Import failed: ${e.message}"
            }
        }
    }

    fun clearMessage() { _message.value = null }

    fun onNotificationsEnabled() {
        viewModelScope.launch { settingsRepository.setRemindersEnabled(true) }
    }

    fun nextStep() { _state.value = _state.value.copy(step = (_state.value.step + 1).coerceAtMost(ONBOARDING_STEP_COUNT - 1)) }
    fun previousStep() { _state.value = _state.value.copy(step = (_state.value.step - 1).coerceAtLeast(0)) }

    fun onNameChange(v: String) { _state.value = _state.value.copy(name = v) }

    fun addSubject(name: String, code: String) {
        viewModelScope.launch {
            val color = nextSubjectColor(_state.value.subjects.size)
            val id = subjectDao.insert(Subject(code = code, name = name, colorArgb = color))
            _state.value = _state.value.copy(subjects = _state.value.subjects + Subject(id, code, name, color))
        }
    }

    fun removeSubject(subject: Subject) {
        viewModelScope.launch {
            subjectDao.delete(subject)
            _state.value = _state.value.copy(
                subjects = _state.value.subjects.filterNot { it.id == subject.id },
                slots = _state.value.slots.filterNot { it.subjectId == subject.id }
            )
        }
    }

    fun addSlot(subject: Subject, dayOfWeek: DayOfWeek, startTime: LocalTime, endTime: LocalTime, classCount: Int) {
        val slot = OnboardingDraftSlot(nextLocalId++, subject.id, subject.name, dayOfWeek, startTime, endTime, classCount)
        _state.value = _state.value.copy(slots = _state.value.slots + slot)
    }

    fun removeSlot(localId: Int) {
        _state.value = _state.value.copy(slots = _state.value.slots.filterNot { it.localId == localId })
    }

    fun onStartDateChange(v: LocalDate) { _state.value = _state.value.copy(startDate = v) }
    fun onHasEndDateChange(v: Boolean) { _state.value = _state.value.copy(hasEndDate = v) }
    fun onEndDateChange(v: LocalDate) { _state.value = _state.value.copy(endDate = v) }
    fun toggleHolidayDay(day: DayOfWeek) {
        val current = _state.value.holidayDays
        _state.value = _state.value.copy(holidayDays = if (day in current) current - day else current + day)
    }
    fun onRequiredPercentageChange(v: String) { _state.value = _state.value.copy(requiredPercentage = v) }

    fun finish() {
        viewModelScope.launch {
            val s = _state.value
            profileRepository.setName(s.name.trim())
            settingsRepository.setTrackingStartDate(s.startDate)
            settingsRepository.setTrackingEndDate(if (s.hasEndDate) s.endDate else null)
            settingsRepository.setRequiredPercentageDefault(s.requiredPercentage.toFloatOrNull() ?: 75f)

            DayOfWeek.entries.forEach { day ->
                recurringHolidayRuleDao.upsert(
                    RecurringHolidayRule(day, if (day in s.holidayDays) RecurringHolidayMode.ALWAYS else RecurringHolidayMode.NONE)
                )
            }

            val versionId = timetableVersionDao.insert(TimetableVersion(effectiveFrom = s.startDate))
            s.slots.forEach { slot ->
                timetableSlotDao.insert(
                    TimetableSlot(
                        timetableVersionId = versionId,
                        subjectId = slot.subjectId,
                        dayOfWeek = slot.dayOfWeek,
                        startTime = slot.startTime,
                        endTime = slot.endTime,
                        classCount = slot.classCount
                    )
                )
            }

            occurrenceRepository.regenerateUnmarkedWindow()
            settingsRepository.setOnboardingComplete(true)

            _finishEvents.send(
                if (s.startDate.isBefore(LocalDate.now())) OnboardingFinishEvent.ToBackdatedFill(versionId)
                else OnboardingFinishEvent.ToHome
            )
        }
    }
}
