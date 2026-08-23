package com.iattend.app.core.domain.occurrence

import com.iattend.app.core.data.db.ClassOccurrenceDao
import com.iattend.app.core.data.db.HolidayDao
import com.iattend.app.core.data.db.OccurrenceSource
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.core.data.db.RecurringHolidayRuleDao
import com.iattend.app.core.data.db.SubjectDao
import com.iattend.app.core.data.db.TimetableSlotDao
import com.iattend.app.core.data.db.TimetableVersionDao
import com.iattend.app.core.datastore.SettingsRepository
import com.iattend.app.core.notifications.ClassReminderScheduler
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

private const val ROLLING_WINDOW_DAYS = 90L

/**
 * Orchestrates the OccurrenceGenerator engine against the database: every structural change
 * (new/edited timetable version, holiday change) calls [regenerateUnmarkedWindow], which wipes
 * every UNMARKED scheduled occurrence between the tracking start date and the generation
 * horizon and regenerates it from scratch off current timetable + holiday state.
 *
 * ponytail: full delete+regenerate on every change rather than an incremental cursor/diff.
 * Simple and always correct; upgrade to incremental only if occurrence volume ever makes this
 * measurably slow (it won't for a single semester's worth of rows).
 */
@Singleton
class OccurrenceRepository @Inject constructor(
    private val timetableVersionDao: TimetableVersionDao,
    private val timetableSlotDao: TimetableSlotDao,
    private val classOccurrenceDao: ClassOccurrenceDao,
    private val holidayDao: HolidayDao,
    private val recurringHolidayRuleDao: RecurringHolidayRuleDao,
    private val settingsRepository: SettingsRepository,
    private val subjectDao: SubjectDao,
    private val reminderScheduler: ClassReminderScheduler
) {
    /**
     * Regenerates the unmarked window and returns the freshly-inserted occurrences (still
     * UNMARKED) so callers can drive the backdated-fill prompt when relevant.
     */
    suspend fun regenerateUnmarkedWindow(today: LocalDate = LocalDate.now()) {
        val settings = settingsRepository.settings.first()
        val versions = timetableVersionDao.getAllOnce()
        // trackingStartDate is a one-time snapshot from onboarding (there's no Settings UI to
        // update it) - if a timetable's effective date is later edited to start earlier than
        // that snapshot, tracking must extend back to cover it, not stay pinned to the stale date.
        val start = (listOfNotNull(settings.trackingStartDate) + versions.map { it.effectiveFrom }).minOrNull() ?: return
        // Horizon must cover the latest of: the global default, or any subject's own end-date
        // override (Computed mode) - otherwise that subject's total would be under-counted.
        val subjectOverrides = subjectDao.getAllOnce().mapNotNull { it.trackingEndDateOverride }
        val horizon = (listOfNotNull(settings.trackingEndDate) + subjectOverrides).maxOrNull()
            ?: today.plusDays(ROLLING_WINDOW_DAYS)
        if (horizon.isBefore(start)) return

        val slotsByVersion = versions.associate { it.id to timetableSlotDao.getForVersionOnce(it.id) }
        val holidays = holidayDao.getAllOnce()
        val rules = recurringHolidayRuleDao.getAllOnce()

        // Dates that already have a MARKED occurrence must be left alone - we only deleted the
        // unmarked rows, so skip regenerating a duplicate for those. Matched by (date, subject,
        // startTime) rather than timetableSlotId: every save deletes and reinserts all slots with
        // fresh ids (see TimetableVersionEditorViewModel.save), which SET_NULLs the id on already-
        // marked occurrences via the FK - keying off it would silently stop recognizing them as
        // already-marked and duplicate a fresh UNMARKED row on every single save.
        val existing = classOccurrenceDao.getInRangeOnce(start, horizon)
        val markedKeys = existing
            .filter { it.status != OccurrenceStatus.UNMARKED && it.source == OccurrenceSource.SCHEDULED }
            .map { Triple(it.date, it.subjectId, it.startTime) }
            .toSet()

        classOccurrenceDao.deleteUnmarkedScheduledInRange(start, horizon)

        val generated = OccurrenceGenerator.generateOccurrences(
            dateRange = start..horizon,
            versions = versions,
            slotsByVersion = slotsByVersion,
            holidays = holidays,
            recurringRules = rules
        ).filterNot { Triple(it.date, it.subjectId, it.startTime) in markedKeys }

        classOccurrenceDao.insertAll(generated)
        reminderScheduler.scheduleTodayReminders()
    }

    /** Backdated occurrences (still unmarked, dated before [today]) grouped by subject, for the fill prompt. */
    suspend fun backdatedUnmarkedBySubject(today: LocalDate = LocalDate.now()): Map<Long, List<com.iattend.app.core.data.db.ClassOccurrence>> {
        val settings = settingsRepository.settings.first()
        val versions = timetableVersionDao.getAllOnce()
        val start = (listOfNotNull(settings.trackingStartDate) + versions.map { it.effectiveFrom }).minOrNull()
            ?: return emptyMap()
        if (!start.isBefore(today)) return emptyMap()
        return classOccurrenceDao.getInRangeOnce(start, today.minusDays(1))
            .filter { it.status == OccurrenceStatus.UNMARKED }
            .groupBy { it.subjectId }
    }

    /**
     * Retroactively corrects the time on occurrences (any status, including already-marked ones)
     * that were generated with a slot's old, wrong start/end time - e.g. an AM/PM typo entered
     * at timetable-creation time. Matched by (subject, day-of-week, old start, old end) rather
     * than timetableSlotId, since every timetable save deletes and reinserts all slots with fresh
     * ids, orphaning that reference on already-marked rows. Only startTime/endTime change; status
     * and everything else (attendance stats depend on those, not on time) is left untouched.
     */
    suspend fun fixOccurrenceTimes(
        subjectId: Long,
        dayOfWeek: DayOfWeek,
        oldStart: LocalTime,
        oldEnd: LocalTime,
        newStart: LocalTime,
        newEnd: LocalTime
    ) {
        if (oldStart == newStart && oldEnd == newEnd) return
        val matching = classOccurrenceDao.getForSubjectOnce(subjectId).filter {
            it.source == OccurrenceSource.SCHEDULED &&
                it.date.dayOfWeek == dayOfWeek &&
                it.startTime == oldStart &&
                it.endTime == oldEnd
        }
        if (matching.isEmpty()) return
        classOccurrenceDao.updateAll(matching.map { it.copy(startTime = newStart, endTime = newEnd) })
    }

    suspend fun applyBackdatedFill(
        subjectId: Long,
        strategy: BackdatedFillStrategy,
        today: LocalDate = LocalDate.now()
    ) {
        val occurrences = backdatedUnmarkedBySubject(today)[subjectId] ?: return
        classOccurrenceDao.updateAll(OccurrenceGenerator.applyBackdatedFill(occurrences, strategy))
    }
}
