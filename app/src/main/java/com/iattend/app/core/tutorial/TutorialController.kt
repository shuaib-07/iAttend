package com.iattend.app.core.tutorial

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iattend.app.core.datastore.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** App-lifetime (not per-screen) - obtained once via hiltViewModel() at the AppNavHost root, same
 * scope as AppViewModel, then reached from any screen through LocalTutorialController. */
@HiltViewModel
class TutorialController @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val stepIndex: StateFlow<Int> = settingsRepository.settings
        .map { it.tutorialStep }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1)

    /** Bounds (root coordinates) of every currently-composed tagged target, keyed by targetId -
     * written unconditionally by Modifier.tutorialTarget whenever that widget lays out. Matching
     * against "is this the current step's target" happens at read time (see targetBounds below),
     * not here - a target's layout can (and usually does) fire before its step is even current,
     * and nothing else would re-trigger that layout once the step catches up. */
    val targetBoundsById = mutableStateMapOf<String, Rect>()

    /** The current step's target bounds, if that widget has composed at least once. */
    val targetBounds: Rect?
        get() = currentStep()?.targetId?.takeIf { it.isNotEmpty() }?.let { targetBoundsById[it] }

    /** The tutorial subject created in step 5/7, so later steps (extra class, history row) can
     * reference the same real subject instead of guessing which one is "the tutorial subject". */
    var tutorialSubjectId by mutableStateOf<Long?>(null)
        private set

    // Reactive, not a one-shot check: this controller is created immediately on app start (while
    // still on the onboarding wizard, before onboardingComplete flips true), so a single read at
    // init time would always see onboardingComplete = false and never fire.
    init {
        viewModelScope.launch {
            settingsRepository.settings
                .map { it.onboardingComplete to it.tutorialStep }
                .distinctUntilChanged()
                .collect { (onboardingComplete, tutorialStep) ->
                    if (onboardingComplete && tutorialStep == -1) settingsRepository.setTutorialStep(0)
                }
        }
    }

    fun currentStep(): TutorialStep? = TUTORIAL_STEPS.getOrNull(stepIndex.value)

    fun setTutorialSubjectId(id: Long) { tutorialSubjectId = id }

    fun next() {
        viewModelScope.launch {
            val n = (stepIndex.value + 1).coerceAtMost(TUTORIAL_STEPS.size)
            settingsRepository.setTutorialStep(n)
        }
    }

    fun skipAll() {
        viewModelScope.launch {
            settingsRepository.setTutorialStep(TUTORIAL_STEPS.size)
        }
    }

    fun restart() {
        viewModelScope.launch {
            targetBoundsById.clear()
            tutorialSubjectId = null
            settingsRepository.setTutorialStep(0)
        }
    }

    /** Called from real save/action call sites (see TutorialSignal) - advances only if it's what
     * the current step is actually waiting on. */
    fun reportSignal(signalId: String) {
        val completion = currentStep()?.completion
        if (completion is TutorialCompletion.OnSignal && completion.signalId == signalId) next()
    }
}

val LocalTutorialController = compositionLocalOf<TutorialController?> { null }
