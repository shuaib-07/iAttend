package com.iattend.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iattend.app.core.datastore.SettingsRepository
import com.iattend.app.core.updates.AppReleaseInfo
import com.iattend.app.core.updates.FeatureHighlight
import com.iattend.app.core.updates.ReleaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AboutUiEvent {
    data class ShowToast(val message: String) : AboutUiEvent
}

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val releaseRepository: ReleaseRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _isCheckingForUpdates = MutableStateFlow(false)
    val isCheckingForUpdates: StateFlow<Boolean> = _isCheckingForUpdates.asStateFlow()

    private val _showWhatsNew = MutableStateFlow(false)
    val showWhatsNew: StateFlow<Boolean> = _showWhatsNew.asStateFlow()

    private val _updateAvailableInfo = MutableStateFlow<AppReleaseInfo?>(null)
    val updateAvailableInfo: StateFlow<AppReleaseInfo?> = _updateAvailableInfo.asStateFlow()

    private val _uiEvent = MutableSharedFlow<AboutUiEvent>()
    val uiEvent: SharedFlow<AboutUiEvent> = _uiEvent.asSharedFlow()

    val currentVersionName: String = releaseRepository.getCurrentVersionName()

    val builtInHighlights: List<FeatureHighlight> = releaseRepository.getBuiltInReleaseNotes()

    val autoCheckUpdates: StateFlow<Boolean> = settingsRepository.settings
        .map { it.autoCheckUpdates }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), true)

    fun setAutoCheckUpdates(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoCheckUpdates(enabled)
        }
    }

    init {
        checkAutoWhatsNew()
    }

    private fun checkAutoWhatsNew() {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            if (settings.lastSeenVersion != currentVersionName) {
                _showWhatsNew.value = true
            }
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _isCheckingForUpdates.value = true
            val result = releaseRepository.checkForUpdates()
            _isCheckingForUpdates.value = false

            result.onSuccess { releaseInfo ->
                if (releaseInfo.isUpdateAvailable) {
                    _updateAvailableInfo.value = releaseInfo
                } else {
                    _uiEvent.emit(AboutUiEvent.ShowToast("You're on the latest version (v$currentVersionName)!"))
                }
            }.onFailure {
                _uiEvent.emit(AboutUiEvent.ShowToast("Couldn't check for updates. Check internet connection."))
            }
        }
    }

    fun showWhatsNew() {
        _showWhatsNew.value = true
    }

    fun dismissWhatsNew() {
        _showWhatsNew.value = false
        viewModelScope.launch {
            settingsRepository.setLastSeenVersion(currentVersionName)
        }
    }

    fun dismissUpdateAvailable() {
        _updateAvailableInfo.value = null
    }
}
