package com.iattend.app.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.iattend.app.core.data.db.Assessment
import com.iattend.app.core.data.db.AssessmentDao
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules exact alarms for a single assessment's configured reminder offsets. Unlike class
 * reminders (rebuilt daily for "today"), assessments are sparse one-off events - alarms are
 * scheduled directly against their absolute date/time whenever the assessment is created, edited,
 * or deleted, and re-armed for all upcoming assessments on boot (exact alarms don't survive reboot).
 */
@Singleton
class AssessmentReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val assessmentDao: AssessmentDao
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean = canScheduleExactAlarmsFor(context)

    /** Cancels any previously scheduled reminders for this assessment, then reschedules from its current offsets. */
    fun reschedule(assessment: Assessment) {
        cancel(assessment.id)
        if (!canScheduleExactAlarms()) return

        val now = LocalDateTime.now()
        val zone = ZoneId.systemDefault()
        ReminderOffset.parse(assessment.reminderOffsets).forEach { offsetMinutes ->
            val reminderAt = LocalDateTime.of(assessment.date, assessment.startTime).minusMinutes(offsetMinutes.toLong())
            if (reminderAt.isAfter(now)) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderAt.atZone(zone).toInstant().toEpochMilli(),
                    pendingIntent(assessment.id, offsetMinutes)
                )
            }
        }
    }

    fun cancel(assessmentId: Long) {
        // ponytail: sweeps preset offsets only - a removed fully-custom offset can leak a stale,
        // harmless (no-op on delete) alarm. Same known limitation as ClassReminderScheduler.
        ReminderOffset.PRESET_MINUTES.forEach { offsetMinutes ->
            alarmManager.cancel(pendingIntent(assessmentId, offsetMinutes))
        }
    }

    suspend fun rescheduleAllUpcoming() {
        val today = LocalDate.now()
        assessmentDao.getAllOnce()
            .filter { !it.date.isBefore(today) }
            .forEach(::reschedule)
    }

    private fun pendingIntent(assessmentId: Long, offsetMinutes: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        // ponytail: see matching comment in ClassReminderScheduler - *100_000 keeps spacing safe
        // up to the 30-day custom offset max.
        (assessmentId * 100_000 + offsetMinutes).toInt(),
        Intent(context, AssessmentReminderReceiver::class.java).putExtra(EXTRA_ASSESSMENT_ID, assessmentId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
