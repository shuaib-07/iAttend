package com.iattend.app.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.iattend.app.core.data.db.ClassOccurrenceDao
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.core.data.db.SubjectDao
import com.iattend.app.core.datastore.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

private const val MIDNIGHT_REFRESH_REQUEST_CODE = Int.MAX_VALUE

/**
 * Schedules exact alarms for today's remaining classes (one per configured reminder offset,
 * Product Plan reminder-offsets addendum) plus a self-chaining midnight alarm that refreshes
 * tomorrow's schedule - the only way to cover "app never opened that day" without a periodic
 * background job (ponytail: reuses AlarmManager, no WorkManager).
 */
@Singleton
class ClassReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val classOccurrenceDao: ClassOccurrenceDao,
    private val subjectDao: SubjectDao,
    private val settingsRepository: SettingsRepository
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean = canScheduleExactAlarmsFor(context)

    suspend fun scheduleTodayReminders() {
        val settings = settingsRepository.settings.first()
        if (!settings.remindersEnabled || !canScheduleExactAlarms()) return

        val today = LocalDate.now()
        val now = LocalDateTime.now()
        val zone = ZoneId.systemDefault()
        val subjectsById = subjectDao.getAllOnce().associateBy { it.id }

        classOccurrenceDao.getForDateOnce(today)
            .filter { it.status == OccurrenceStatus.UNMARKED && it.startTime != null }
            .forEach { occurrence ->
                // Sweeps every preset offset's request code first so a changed reminder-timing setting
                // doesn't leave a stale alarm behind at the old offset (ponytail: a fully custom, non-preset
                // offset value that's later removed can still leak; harmless no-op fire, out of scope here).
                ReminderOffset.PRESET_MINUTES.forEach { offsetMinutes ->
                    alarmManager.cancel(classReminderPendingIntent(occurrence.id, offsetMinutes))
                }
                val override = subjectsById[occurrence.subjectId]?.reminderOffsetsOverride
                val offsets = ReminderOffset.parse(override ?: settings.classReminderDefaultOffsets)
                offsets.forEach { offsetMinutes ->
                    val reminderAt = LocalDateTime.of(today, occurrence.startTime).minusMinutes(offsetMinutes.toLong())
                    if (reminderAt.isAfter(now)) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminderAt.atZone(zone).toInstant().toEpochMilli(),
                            classReminderPendingIntent(occurrence.id, offsetMinutes)
                        )
                    }
                }

                alarmManager.cancel(classEndPendingIntent(occurrence.id))
                occurrence.endTime?.let { endTime ->
                    val endAt = LocalDateTime.of(today, endTime)
                    if (endAt.isAfter(now)) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            endAt.atZone(zone).toInstant().toEpochMilli(),
                            classEndPendingIntent(occurrence.id)
                        )
                    }
                }
            }
    }

    private fun classReminderPendingIntent(occurrenceId: Long, offsetMinutes: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        // ponytail: *100_000 covers offsets up to 99999min (~69 days) without colliding across
        // occurrences - the custom reminder picker allows up to 30 days (43200min), and the old
        // *10_000 spacing broke silently past ~6 days by aliasing onto another occurrence's alarm.
        (occurrenceId * 100_000 + offsetMinutes).toInt(),
        Intent(context, ClassReminderReceiver::class.java).putExtra(EXTRA_OCCURRENCE_ID, occurrenceId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun classEndPendingIntent(occurrenceId: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        occurrenceId.toInt(),
        Intent(context, ClassEndReceiver::class.java).putExtra(EXTRA_OCCURRENCE_ID, occurrenceId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun scheduleMidnightRefresh() {
        val zone = ZoneId.systemDefault()
        val nextMidnight = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MIDNIGHT_REFRESH_REQUEST_CODE,
            Intent(context, MidnightRefreshReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextMidnight.atZone(zone).toInstant().toEpochMilli(),
                pendingIntent
            )
        }
    }
}
