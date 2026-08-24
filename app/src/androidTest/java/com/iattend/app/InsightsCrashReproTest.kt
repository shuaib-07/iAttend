package com.iattend.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iattend.app.core.data.db.AppDatabase
import com.iattend.app.core.data.db.ClassOccurrence
import com.iattend.app.core.data.db.ClassType
import com.iattend.app.core.data.db.OccurrenceSource
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.data.db.TimetableSlot
import com.iattend.app.core.data.db.TimetableVersion
import com.iattend.app.core.data.db.TotalMode
import com.iattend.app.core.data.export.ExportImportRepository
import com.iattend.app.core.data.export.TemplatePayload
import com.iattend.app.core.data.export.TemplateSettings
import com.iattend.app.core.datastore.ProfileRepository
import com.iattend.app.core.datastore.SettingsRepository
import com.iattend.app.core.domain.occurrence.OccurrenceRepository
import com.iattend.app.core.notifications.ClassReminderScheduler
import com.iattend.app.feature.home.HomeViewModel
import com.iattend.app.feature.home.InsightsScreen
import com.iattend.app.core.datastore.ThemeMode
import com.iattend.app.ui.theme.IAttendTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Reproduces the reported crash: import a shareable template (built by another install,
 * unrelated ids/data), then open Insights. Drives the real repository + real HomeViewModel +
 * real InsightsScreen composable so any exception surfaces exactly like on-device.
 */
@RunWith(AndroidJUnit4::class)
class InsightsCrashReproTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun importTemplate_thenOpenInsights_doesNotCrash() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .build()

        val settingsRepository = SettingsRepository(context)
        val profileRepository = ProfileRepository(context)
        val reminderScheduler = ClassReminderScheduler(
            context, db.classOccurrenceDao(), db.subjectDao(), settingsRepository
        )
        val occurrenceRepository = OccurrenceRepository(
            db.timetableVersionDao(), db.timetableSlotDao(), db.classOccurrenceDao(),
            db.holidayDao(), db.recurringHolidayRuleDao(), settingsRepository,
            db.subjectDao(), reminderScheduler
        )
        val repo = ExportImportRepository(
            db, db.subjectDao(), db.timetableVersionDao(), db.timetableSlotDao(),
            db.classOccurrenceDao(), db.holidayDao(), db.recurringHolidayRuleDao(),
            settingsRepository, profileRepository, occurrenceRepository, db.assessmentDao()
        )

        val today = LocalDate.now()

        // Simulate real prior usage: existing subject + weeks of marked attendance,
        // and a HomeViewModel that's already alive (subscribed) before the import happens -
        // matches a user who's been using the app and then imports a classmate's template.
        runBlocking {
            val existingSubjectId = db.subjectDao().insert(
                Subject(code = "OLD1", name = "Old Subject", colorArgb = 0xFF888888.toInt(), totalMode = TotalMode.OPEN_ENDED)
            )
            val existingOccurrences = (0..13L).map { offset ->
                ClassOccurrence(
                    subjectId = existingSubjectId,
                    date = today.minusDays(offset),
                    startTime = LocalTime.of(9, 0),
                    endTime = LocalTime.of(10, 0),
                    status = if (offset % 3 == 0L) OccurrenceStatus.ABSENT else OccurrenceStatus.PRESENT,
                    source = OccurrenceSource.SCHEDULED,
                    timetableSlotId = null
                )
            }
            db.classOccurrenceDao().insertAll(existingOccurrences)
        }

        val homeViewModel = HomeViewModel(db.subjectDao(), db.classOccurrenceDao(), settingsRepository)
        val collectorScope = CoroutineScope(Dispatchers.Default)
        collectorScope.launch { homeViewModel.state.collect { } }
        runBlocking {
            withTimeout(10_000) { homeViewModel.state.first { it.subjects.size == 1 } }
        }

        val payload = TemplatePayload(
            subjects = listOf(
                Subject(id = 1, code = "CS101", name = "Data Structures", colorArgb = 0xFF0000FF.toInt(), totalMode = TotalMode.KNOWN, knownTotalClasses = 40),
                Subject(id = 2, code = "CS102", name = "Algorithms", colorArgb = 0xFFFF0000.toInt(), totalMode = TotalMode.COMPUTED),
                Subject(id = 3, code = "CS103", name = "Databases", colorArgb = 0xFF00FF00.toInt(), totalMode = TotalMode.OPEN_ENDED)
            ),
            timetableVersions = listOf(
                TimetableVersion(id = 1, label = "Sem 1", effectiveFrom = today.minusDays(30))
            ),
            timetableSlots = listOf(
                TimetableSlot(id = 1, timetableVersionId = 1, subjectId = 1, dayOfWeek = DayOfWeek.MONDAY, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(10, 0), classType = ClassType.LECTURE),
                TimetableSlot(id = 2, timetableVersionId = 1, subjectId = 2, dayOfWeek = DayOfWeek.TUESDAY, startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 0), classType = ClassType.LECTURE),
                TimetableSlot(id = 3, timetableVersionId = 1, subjectId = 3, dayOfWeek = DayOfWeek.WEDNESDAY, startTime = LocalTime.of(11, 0), endTime = LocalTime.of(12, 0), classType = ClassType.LECTURE)
            ),
            holidays = emptyList(),
            recurringHolidayRules = emptyList(),
            extraClasses = emptyList(),
            assessments = emptyList(),
            templateSettings = TemplateSettings(
                trackingStartDate = today.minusDays(30),
                trackingEndDate = today.plusDays(60),
                requiredPercentageDefault = 75f
            )
        )
        val json = Json { encodeDefaults = true }.encodeToString(payload)

        runBlocking {
            repo.importTemplate(json)
        }

        runBlocking {
            withTimeout(10_000) {
                homeViewModel.state.first { it.subjects.size == 3 }
            }
        }

        composeRule.setContent {
            // Real theme, not the default M3 shapes - SquircleShape is where the crash lives.
            IAttendTheme(themeMode = ThemeMode.SYSTEM, dynamicColorEnabled = false, accentColor = "FOREST") {
                InsightsScreen(viewModel = homeViewModel)
            }
        }
        composeRule.waitForIdle()
    }
}
