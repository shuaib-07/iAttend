package com.iattend.app.core.navigation

import kotlinx.serialization.Serializable

@Serializable object OnboardingRoute

@Serializable object HomeRoute
@Serializable object CalendarRoute
@Serializable object SettingsRoute
@Serializable object AppearanceRoute
@Serializable object InsightsRoute

@Serializable object TimetableHubRoute
@Serializable object SubjectListRoute
@Serializable data class SubjectDetailRoute(val subjectId: Long)
@Serializable data class SubjectEditorRoute(val subjectId: Long = 0L) // 0 = new
@Serializable object TimetableVersionListRoute
@Serializable data class TimetableVersionEditorRoute(val versionId: Long = 0L) // 0 = new
@Serializable data class BackdatedFillRoute(val versionId: Long)
@Serializable object HolidayListRoute
@Serializable object RecurringHolidayRoute
@Serializable data class ExtraClassesRoute(val prefillDate: String? = null)
@Serializable data class AssessmentListRoute(val subjectId: Long)
@Serializable data class AssessmentEditorRoute(val subjectId: Long, val assessmentId: Long = 0L) // 0 = new
@Serializable object AllAssessmentsRoute
@Serializable object AboutRoute
@Serializable object LicensesRoute

/** Top-level destinations that get a persistent bottom nav bar entry. */
enum class BottomTab(val label: String) {
    Home("Home"), Calendar("Calendar"), Timetable("Timetable"), Insights("Insights"), Settings("Settings")
}
