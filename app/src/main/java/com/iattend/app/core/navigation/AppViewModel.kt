package com.iattend.app.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iattend.app.core.datastore.NavBarStyle
import com.iattend.app.core.datastore.SettingsRepository
import com.iattend.app.core.datastore.ThemeMode
import com.iattend.app.core.notifications.AutoBackupScheduler
import com.iattend.app.core.notifications.ClassReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val reminderScheduler: ClassReminderScheduler,
    private val autoBackupScheduler: AutoBackupScheduler
) : ViewModel() {
    private val _onboardingComplete = MutableStateFlow<Boolean?>(null)
    val onboardingComplete: StateFlow<Boolean?> = _onboardingComplete.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _dynamicColorEnabled = MutableStateFlow(true)
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    private val _accentColor = MutableStateFlow("FOREST")
    val accentColor: StateFlow<String> = _accentColor.asStateFlow()

    private val _navBarStyle = MutableStateFlow(NavBarStyle.PILL)
    val navBarStyle: StateFlow<NavBarStyle> = _navBarStyle.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect {
                _onboardingComplete.value = it.onboardingComplete
                _themeMode.value = it.themeMode
                _dynamicColorEnabled.value = it.dynamicColorEnabled
                _accentColor.value = it.accentColor
                _navBarStyle.value = it.navBarStyle
            }
        }
        viewModelScope.launch {
            settingsRepository.settings
                .map { it.autoBackupEnabled to it.autoBackupFrequency }
                .distinctUntilChanged()
                .collect { (enabled, frequency) -> autoBackupScheduler.reschedule(enabled, frequency) }
        }
        viewModelScope.launch {
            reminderScheduler.scheduleTodayReminders()
            reminderScheduler.scheduleMidnightRefresh()
        }
    }
}
