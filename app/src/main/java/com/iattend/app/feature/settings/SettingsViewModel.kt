package com.iattend.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iattend.app.core.datastore.BackupFrequency
import com.iattend.app.core.datastore.NavBarStyle
import com.iattend.app.core.datastore.SettingsRepository
import com.iattend.app.core.datastore.ThemeMode
import com.iattend.app.core.domain.occurrence.OccurrenceRepository
import com.iattend.app.core.notifications.ClassReminderScheduler
import com.iattend.app.core.notifications.ReminderOffset
import com.iattend.app.widget.UpcomingClassesWidget
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val occurrenceRepository: OccurrenceRepository,
    private val reminderScheduler: ClassReminderScheduler,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val requiredPercentage: StateFlow<Float> = settingsRepository.settings
        .map { it.requiredPercentageDefault }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 75f)

    val themeMode: StateFlow<ThemeMode> = settingsRepository.settings
        .map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val dynamicColorEnabled: StateFlow<Boolean> = settingsRepository.settings
        .map { it.dynamicColorEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val trackingStartDate: StateFlow<LocalDate?> = settingsRepository.settings
        .map { it.trackingStartDate }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val trackingEndDate: StateFlow<LocalDate?> = settingsRepository.settings
        .map { it.trackingEndDate }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val remindersEnabled: StateFlow<Boolean> = settingsRepository.settings
        .map { it.remindersEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val navBarStyle: StateFlow<NavBarStyle> = settingsRepository.settings
        .map { it.navBarStyle }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NavBarStyle.PILL)

    val accentColor: StateFlow<String> = settingsRepository.settings
        .map { it.accentColor }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "FOREST")

    val classReminderDefaultOffsets: StateFlow<String> = settingsRepository.settings
        .map { it.classReminderDefaultOffsets }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "10")

    val examReminderDefaultOffsets: StateFlow<String> = settingsRepository.settings
        .map { it.examReminderDefaultOffsets }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "60,1440")

    val autoBackupEnabled: StateFlow<Boolean> = settingsRepository.settings
        .map { it.autoBackupEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val autoBackupFrequency: StateFlow<BackupFrequency> = settingsRepository.settings
        .map { it.autoBackupFrequency }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BackupFrequency.WEEKLY)

    val autoBackupFolderUri: StateFlow<String?> = settingsRepository.settings
        .map { it.autoBackupFolderUri }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setRequiredPercentage(value: Float) {
        viewModelScope.launch { settingsRepository.setRequiredPercentageDefault(value) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
            UpcomingClassesWidget().updateAll(context)
        }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicColorEnabled(enabled)
            UpcomingClassesWidget().updateAll(context)
        }
    }

    fun setTrackingStartDate(date: LocalDate?) {
        viewModelScope.launch {
            settingsRepository.setTrackingStartDate(date)
            occurrenceRepository.regenerateUnmarkedWindow()
        }
    }

    fun setTrackingEndDate(date: LocalDate?) {
        viewModelScope.launch {
            settingsRepository.setTrackingEndDate(date)
            occurrenceRepository.regenerateUnmarkedWindow()
        }
    }

    fun canScheduleExactAlarms(): Boolean = reminderScheduler.canScheduleExactAlarms()

    fun setNavBarStyle(style: NavBarStyle) {
        viewModelScope.launch { settingsRepository.setNavBarStyle(style) }
    }

    fun setAccentColor(color: String) {
        viewModelScope.launch {
            settingsRepository.setAccentColor(color)
            UpcomingClassesWidget().updateAll(context)
        }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRemindersEnabled(enabled)
            if (enabled) {
                reminderScheduler.scheduleTodayReminders()
                reminderScheduler.scheduleMidnightRefresh()
            }
        }
    }

    fun setClassReminderDefaultOffsets(offsets: List<Int>) {
        viewModelScope.launch {
            settingsRepository.setClassReminderDefaultOffsets(ReminderOffset.format(offsets))
            reminderScheduler.scheduleTodayReminders()
        }
    }

    fun setExamReminderDefaultOffsets(offsets: List<Int>) {
        viewModelScope.launch {
            settingsRepository.setExamReminderDefaultOffsets(ReminderOffset.format(offsets))
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoBackupEnabled(enabled) }
    }

    fun setAutoBackupFrequency(frequency: BackupFrequency) {
        viewModelScope.launch { settingsRepository.setAutoBackupFrequency(frequency) }
    }

    fun setAutoBackupFolderUri(uri: String?) {
        viewModelScope.launch { settingsRepository.setAutoBackupFolderUri(uri) }
    }
}
