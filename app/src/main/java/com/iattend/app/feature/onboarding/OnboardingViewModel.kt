package com.iattend.app.feature.onboarding

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iattend.app.core.data.db.RecurringHolidayMode
import com.iattend.app.core.data.db.RecurringHolidayRule
import com.iattend.app.core.data.db.RecurringHolidayRuleDao
import com.iattend.app.core.data.db.Semester
import com.iattend.app.core.data.db.SemesterDao
import com.iattend.app.core.data.db.TimetableVersionDao
import com.iattend.app.core.data.export.ExportImportRepository
import com.iattend.app.core.datastore.ProfileRepository
import com.iattend.app.core.datastore.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

data class OnboardingState(
    val hasChosenStart: Boolean = false,
    val step: Int = 0,
    val name: String = "",
    val avatarUri: String? = null,
    val semesterName: String = "Semester 1",
    val semesterStartDate: LocalDate = LocalDate.now(),
    val semesterEndDate: LocalDate = LocalDate.now().plusMonths(4),
    val semesterTargetPercentage: Float = 75f,
    val showTemplateNamePrompt: Boolean = false,
    val templateNameInput: String = ""
)

sealed class OnboardingFinishEvent {
    data object ToHome : OnboardingFinishEvent()
    data class ToBackdatedFill(val versionId: Long) : OnboardingFinishEvent()
}

/**
 * Onboarding steps:
 * 0: Features
 * 1: Notifications
 * 2: Theme
 * 3: Name / Avatar
 * 4: Semester Setup
 */
const val ONBOARDING_STEP_COUNT = 5

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val semesterDao: SemesterDao,
    private val recurringHolidayRuleDao: RecurringHolidayRuleDao,
    private val settingsRepository: SettingsRepository,
    private val timetableVersionDao: TimetableVersionDao,
    private val exportImportRepository: ExportImportRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _finishEvents = Channel<OnboardingFinishEvent>(Channel.BUFFERED)
    val finishEvents = _finishEvents.receiveAsFlow()

    fun chooseStartFresh() { _state.value = _state.value.copy(hasChosenStart = true) }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: return@launch
                val isTemplate = exportImportRepository.importAuto(text)
                if (isTemplate) {
                    val profile = profileRepository.profile.first()
                    if (profile.name.isBlank()) {
                        _state.value = _state.value.copy(showTemplateNamePrompt = true)
                    } else {
                        finishImportedTemplate()
                    }
                } else {
                    settingsRepository.setOnboardingComplete(true)
                    _finishEvents.send(OnboardingFinishEvent.ToHome)
                }
            } catch (e: Exception) {
                _message.value = "Import failed: ${e.message}"
            }
        }
    }

    fun onTemplateNameInputChange(name: String) {
        _state.value = _state.value.copy(templateNameInput = name)
    }

    fun submitTemplateNameAndFinish() {
        viewModelScope.launch {
            val name = _state.value.templateNameInput.trim()
            if (name.isNotBlank()) {
                profileRepository.setName(name)
            }
            _state.value = _state.value.copy(showTemplateNamePrompt = false)
            finishImportedTemplate()
        }
    }

    private suspend fun finishImportedTemplate() {
        settingsRepository.setOnboardingComplete(true)
        val latestVersion = timetableVersionDao.getAllOnce().maxByOrNull { it.effectiveFrom }
        if (latestVersion != null) {
            _finishEvents.send(OnboardingFinishEvent.ToBackdatedFill(latestVersion.id))
        } else {
            _finishEvents.send(OnboardingFinishEvent.ToHome)
        }
    }

    fun clearMessage() { _message.value = null }

    fun onNotificationsEnabled() {
        viewModelScope.launch { settingsRepository.setRemindersEnabled(true) }
    }

    fun nextStep() { _state.value = _state.value.copy(step = (_state.value.step + 1).coerceAtMost(ONBOARDING_STEP_COUNT - 1)) }
    fun previousStep() { _state.value = _state.value.copy(step = (_state.value.step - 1).coerceAtLeast(0)) }

    fun onNameChange(v: String) { _state.value = _state.value.copy(name = v) }

    fun onSemesterNameChange(name: String) { _state.value = _state.value.copy(semesterName = name) }
    fun onSemesterStartDateChange(date: LocalDate) { _state.value = _state.value.copy(semesterStartDate = date) }
    fun onSemesterEndDateChange(date: LocalDate) { _state.value = _state.value.copy(semesterEndDate = date) }
    fun onSemesterTargetPercentageChange(pct: Float) { _state.value = _state.value.copy(semesterTargetPercentage = pct) }

    /** Copies the picked image into app-private storage (same as ProfileViewModel.setPicture)
     * so the path stays valid, kept as a draft until [finish] persists it. */
    fun onAvatarPicked(uri: Uri) {
        viewModelScope.launch {
            val file = File(context.filesDir, "profile_picture.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            _state.value = _state.value.copy(avatarUri = file.absolutePath)
        }
    }

    fun onAvatarUrlSelected(url: String) { _state.value = _state.value.copy(avatarUri = url) }

    fun finish() {
        viewModelScope.launch {
            val s = _state.value
            profileRepository.setName(s.name.trim())
            profileRepository.setPictureUri(s.avatarUri)

            val semName = s.semesterName.trim().ifBlank { "Semester 1" }
            val existingSemesters = semesterDao.getAllOnce()
            val semesterId = if (existingSemesters.isNotEmpty()) {
                val existing = existingSemesters.first()
                semesterDao.update(
                    existing.copy(
                        name = semName,
                        startDate = s.semesterStartDate,
                        endDate = s.semesterEndDate,
                        targetPercentage = s.semesterTargetPercentage
                    )
                )
                existing.id
            } else {
                semesterDao.insert(
                    Semester(
                        name = semName,
                        startDate = s.semesterStartDate,
                        endDate = s.semesterEndDate,
                        targetPercentage = s.semesterTargetPercentage,
                        isArchived = false,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            DayOfWeek.entries.forEach { day ->
                recurringHolidayRuleDao.upsert(
                    RecurringHolidayRule(
                        dayOfWeek = day,
                        mode = RecurringHolidayMode.NONE,
                        semesterId = semesterId
                    )
                )
            }

            settingsRepository.setActiveSemesterId(semesterId)
            settingsRepository.setTrackingStartDate(s.semesterStartDate)
            settingsRepository.setTrackingEndDate(s.semesterEndDate)
            settingsRepository.setRequiredPercentageDefault(s.semesterTargetPercentage.coerceIn(0f, 100f))
            settingsRepository.setOnboardingComplete(true)
            _finishEvents.send(OnboardingFinishEvent.ToHome)
        }
    }
}
