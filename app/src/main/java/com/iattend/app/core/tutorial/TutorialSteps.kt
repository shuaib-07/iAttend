package com.iattend.app.core.tutorial

import androidx.navigation.NavDestination.Companion.hasRoute
import com.iattend.app.core.navigation.CalendarRoute
import com.iattend.app.core.navigation.ExtraClassesRoute
import com.iattend.app.core.navigation.HomeRoute
import com.iattend.app.core.navigation.RecurringHolidayRoute
import com.iattend.app.core.navigation.SettingsRoute
import com.iattend.app.core.navigation.SubjectDetailRoute
import com.iattend.app.core.navigation.SubjectEditorRoute
import com.iattend.app.core.navigation.SubjectListRoute
import com.iattend.app.core.navigation.TimetableHubRoute
import com.iattend.app.core.navigation.TimetableVersionEditorRoute
import com.iattend.app.core.navigation.TimetableVersionListRoute
import java.time.LocalDate

/**
 * The live-screen guided tour (see ignore/docs/ONBOARDING_TUTORIAL_PLAN.md §3), replacing the old
 * wizard's Subjects/Timetable/Dates/Holidays/Threshold steps with real setup on the real screens.
 *
 * ponytail: steps 12/13 of the plan doc ("Duplicate day to" + per-slot copy, both live inside the
 * slots bottom sheet, which is local screen state a nav-driven controller can't force open) are
 * merged into one informational step here rather than each getting a forced-open-sheet target.
 */
val TUTORIAL_STEPS: List<TutorialStep> = listOf(
    TutorialStep(
        id = 1, navigateTo = SettingsRoute, targetId = "required_pct_row",
        title = "Required attendance %",
        body = "This is the minimum attendance you need. Everything else is measured against it."
    ),
    TutorialStep(
        id = 2, targetId = "tracking_period_row",
        title = "Tracking period (optional)",
        body = "Set when your term starts/ends if you know it. If you leave it unset, the app just " +
            "tracks a rolling 90 days ahead instead - you can always set this later."
    ),
    TutorialStep(
        id = 3, navigateTo = TimetableHubRoute,
        title = "Timetable hub",
        body = "Subjects, timetable, holidays and extra classes all live here."
    ),
    TutorialStep(
        id = 4, targetId = "subjects_tile",
        title = "Subjects",
        body = "Tap Subjects to add your first one.",
        completion = TutorialCompletion.OnRoute { it?.hasRoute<SubjectListRoute>() == true }
    ),
    TutorialStep(
        id = 5, targetId = "add_subject_fab",
        title = "Add a subject",
        body = "Tap the + to create one.",
        completion = TutorialCompletion.OnRoute { it?.hasRoute<SubjectEditorRoute>() == true }
    ),
    TutorialStep(
        id = 6, targetId = "subject_name_field",
        title = "Name it",
        body = "Give it a name - the code below is optional."
    ),
    TutorialStep(
        id = 7, targetId = "required_pct_override_field",
        title = "Per-subject override",
        body = "Optional - if one subject has a different required %, set it here instead of the global default."
    ),
    TutorialStep(
        id = 8, targetId = "total_mode_radios",
        title = "Total classes",
        body = "Known = you know the exact class count for the term. Computed = counted forward to " +
            "your tracking end date from Settings. Open-ended = no end date yet, so it rolls on a 90-day " +
            "horizon."
    ),
    TutorialStep(
        id = 9, targetId = "subject_save_button",
        title = "Save the subject",
        body = "Scroll down and tap Save to create it.",
        completion = TutorialCompletion.OnSignal(TutorialSignal.SUBJECT_SAVED)
    ),
    TutorialStep(
        id = 10, navigateTo = RecurringHolidayRoute,
        title = "Weekly holidays",
        body = "Toggle any day that's always off (e.g. every Sunday) - it won't count against your attendance."
    ),
    TutorialStep(
        id = 11, navigateTo = TimetableHubRoute, targetId = "manage_timetable_tile",
        title = "Manage timetable",
        body = "Tap Manage Timetable to build your weekly schedule.",
        completion = TutorialCompletion.OnRoute { it?.hasRoute<TimetableVersionListRoute>() == true }
    ),
    TutorialStep(
        id = 12, targetId = "add_version_fab",
        title = "Add a timetable version",
        body = "Tap the + to start building this term's schedule.",
        completion = TutorialCompletion.OnRoute { it?.hasRoute<TimetableVersionEditorRoute>() == true }
    ),
    TutorialStep(
        id = 13, targetId = "slot_form",
        title = "Add a class slot",
        body = "Pick the day, subject, start/end time, class type, and how many classes this slot " +
            "counts as - then Save Slot.",
        completion = TutorialCompletion.OnSignal(TutorialSignal.SLOT_SAVED)
    ),
    TutorialStep(
        id = 14, targetId = "view_slots_button",
        title = "Duplicating slots",
        body = "Open View Slots any time to duplicate a whole day's schedule to other days, or copy " +
            "just one slot to another day, instead of re-entering it by hand."
    ),
    TutorialStep(
        id = 15, targetId = "save_timetable_button",
        title = "Save your timetable",
        body = "Slots aren't kept until you save the timetable itself.",
        completion = TutorialCompletion.OnSignal(TutorialSignal.TIMETABLE_SAVED)
    ),
    TutorialStep(
        id = 16, navigateTo = ExtraClassesRoute(prefillDate = LocalDate.now().toString()), targetId = "extra_class_form",
        title = "Extra classes",
        body = "For one-off classes outside your regular timetable. Let's add one for today so you " +
            "have something real to mark in the next couple steps.",
        completion = TutorialCompletion.OnSignal(TutorialSignal.EXTRA_CLASS_SAVED)
    ),
    TutorialStep(
        id = 17, navigateTo = HomeRoute, targetId = "attendance_chart",
        title = "Your dashboard",
        body = "This chart tracks your overall attendance trend over time."
    ),
    TutorialStep(
        id = 18, targetId = "subject_summary_card",
        title = "Subject cards",
        body = "Each card below is one subject. Tap yours to open it.",
        completion = TutorialCompletion.OnRoute { it?.hasRoute<SubjectDetailRoute>() == true }
    ),
    TutorialStep(
        id = 19, targetId = "history_row",
        title = "Marking attendance",
        body = "Tap any date to cycle it: Present -> Absent -> Cancelled -> Unmarked. Try it now.",
        completion = TutorialCompletion.OnSignal(TutorialSignal.HISTORY_STATUS_CYCLED)
    ),
    TutorialStep(
        id = 20, navigateTo = CalendarRoute, targetId = "mark_chip",
        title = "Marking from Calendar",
        body = "You can also mark any scheduled or extra class Present/Absent/Cancelled right from here. Try it now.",
        completion = TutorialCompletion.OnSignal(TutorialSignal.OCCURRENCE_MARKED)
    ),
    TutorialStep(
        id = 21, targetId = "notification_bell",
        title = "Class reminders",
        body = "Tap the bell to enable reminders before each class and grant the permission it needs. " +
            "You can also add multiple reminder timings there. That's the tour - you're set."
    )
)
