package com.iattend.app.core.tutorial

import androidx.navigation.NavDestination

/** How a step hands control to the next one. */
sealed class TutorialCompletion {
    /** User taps "Next" in the tooltip - no real action required. */
    data object ManualNext : TutorialCompletion()
    /** Advances automatically once navigation lands on a destination [matches] accepts - used
     * when the real action to perform IS the navigation (tapping a FAB/tile). */
    data class OnRoute(val matches: (NavDestination?) -> Boolean) : TutorialCompletion()
    /** Advances when the screen reports this signal id via TutorialController.reportSignal -
     * used when the real action is saving something (subject, slot, timetable, extra class...). */
    data class OnSignal(val signalId: String) : TutorialCompletion()
}

data class TutorialStep(
    val id: Int,
    /** Force-navigate here when this step becomes current. Null = stay on current screen
     * (either already there, or the previous step's own action already got us here). */
    val navigateTo: Any? = null,
    /** Matches the id passed to Modifier.tutorialTarget(...) on the real widget. Empty = no
     * specific cutout, just a whole-screen tooltip. */
    val targetId: String = "",
    val title: String,
    val body: String,
    val completion: TutorialCompletion = TutorialCompletion.ManualNext
)

/** Signal ids reported by real save/action call sites - kept as constants so the screen code and
 * the step list can't drift apart on a typo'd string. */
object TutorialSignal {
    const val SUBJECT_SAVED = "subject_saved"
    const val SLOT_SAVED = "slot_saved"
    const val TIMETABLE_SAVED = "timetable_saved"
    const val EXTRA_CLASS_SAVED = "extra_class_saved"
    const val HISTORY_STATUS_CYCLED = "history_status_cycled"
    const val OCCURRENCE_MARKED = "occurrence_marked"
}
