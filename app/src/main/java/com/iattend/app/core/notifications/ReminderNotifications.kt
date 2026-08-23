package com.iattend.app.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.app.AlarmManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.iattend.app.MainActivity
import com.iattend.app.R
import com.iattend.app.core.data.db.Assessment
import com.iattend.app.core.data.db.AssessmentType
import com.iattend.app.core.data.db.ClassOccurrence
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.ui.formatDateShort
import com.iattend.app.core.ui.formatTime
import java.time.LocalDate

const val REMINDER_CHANNEL_ID = "class_reminders"
const val ASSESSMENT_REMINDER_CHANNEL_ID = "assessment_reminders"
const val APP_UPDATES_CHANNEL_ID = "app_updates"
const val EXTRA_OCCURRENCE_ID = "occurrence_id"
const val EXTRA_ASSESSMENT_ID = "assessment_id"
const val EXTRA_ACTION_STATUS = "action_status"
const val ACTION_MARK_STATUS = "com.iattend.app.action.MARK_STATUS"
/** Offset added to the occurrence id so the post-class notification gets its own id, distinct
 * from the pre-class reminder's (which uses the bare occurrence id) - both can be showing at once. */
const val CLASS_END_NOTIFICATION_ID_OFFSET = 2_000_000
const val APP_UPDATE_NOTIFICATION_ID = 3_000_001

fun canScheduleExactAlarmsFor(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

fun ensureReminderChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(
        NotificationChannel(
            REMINDER_CHANNEL_ID,
            "Class reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Reminds you before each scheduled class, at the timing you choose" }
    )
    manager.createNotificationChannel(
        NotificationChannel(
            ASSESSMENT_REMINDER_CHANNEL_ID,
            "Test & exam reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Reminds you before an upcoming test or exam, at the timing you choose" }
    )
    manager.createNotificationChannel(
        NotificationChannel(
            APP_UPDATES_CHANNEL_ID,
            "App updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Notifies you when a new release or update is available" }
    )
}

/** Builds and posts the "class starting soon" notification with inline Present/Absent actions. */
fun showClassReminderNotification(context: Context, occurrence: ClassOccurrence, subjectName: String) {
    val openIntent = PendingIntent.getActivity(
        context,
        occurrence.id.toInt(),
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val whenText = occurrence.startTime?.let(::formatTime)?.let { time ->
        if (occurrence.date == LocalDate.now()) time else "${formatDateShort(occurrence.date)}, $time"
    } ?: ""

    val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(subjectName)
        .setContentText("Starts at $whenText - mark your attendance")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(openIntent)
        .addAction(0, "Present", actionPendingIntent(context, occurrence.id, "PRESENT"))
        .addAction(0, "Absent", actionPendingIntent(context, occurrence.id, "ABSENT"))
        .build()

    NotificationManagerCompat.from(context).notify(occurrence.id.toInt(), notification)
}

/** Fired at class end time (independent of the pre-class reminder above) - prompts marking
 * attendance right after the class, with a "can skip N more" hint pulled from the subject's
 * existing attendance stats. Skipped by the caller if the occurrence was already marked. */
fun showClassEndNotification(context: Context, occurrence: ClassOccurrence, subject: Subject, classesCanSkip: Int?) {
    val openIntent = PendingIntent.getActivity(
        context,
        occurrence.id.toInt(),
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val typeLabel = occurrence.classType.pluralLabel
    val title = "${subject.name} (${subject.code}) - $typeLabel" + (subject.teacherName?.let { " by $it" } ?: "")
    val text = classesCanSkip?.let { "Can miss up to $it more $typeLabel${if (it == 1) "" else "s"}." }
        ?: "Mark your attendance for this class."

    val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(text)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(openIntent)
        .addAction(0, "Cancelled", actionPendingIntent(context, occurrence.id, "CANCELLED"))
        .addAction(0, "Absent", actionPendingIntent(context, occurrence.id, "ABSENT"))
        .addAction(0, "Present", actionPendingIntent(context, occurrence.id, "PRESENT"))
        .build()

    NotificationManagerCompat.from(context).notify(CLASS_END_NOTIFICATION_ID_OFFSET + occurrence.id.toInt(), notification)
}

/** Test and Exam get distinct title/copy ("specialised notifications") though they share a channel and builder. */
fun showAssessmentReminderNotification(context: Context, assessment: Assessment, subjectName: String) {
    val openIntent = PendingIntent.getActivity(
        context,
        assessment.id.toInt(),
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val label = assessment.title?.takeIf { it.isNotBlank() }
        ?: "$subjectName ${if (assessment.type == AssessmentType.EXAM) "Exam" else "Test"}"
    val title = if (assessment.type == AssessmentType.EXAM) "Upcoming exam: $label" else "Upcoming test: $label"
    var timeRange = "${formatTime(assessment.startTime)} - ${formatTime(assessment.endTime)}"
    if (assessment.date != LocalDate.now()) timeRange = "${formatDateShort(assessment.date)}, $timeRange"
    val text = "$subjectName - $timeRange" + (assessment.totalMarks?.let { " - $it marks" } ?: "")

    val notification = NotificationCompat.Builder(context, ASSESSMENT_REMINDER_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(text)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(openIntent)
        .build()

    NotificationManagerCompat.from(context).notify(REMINDER_CHANNEL_ID.hashCode() + assessment.id.toInt(), notification)
}

/** Posts an update notification when a newer release is found in the background. */
fun showUpdateAvailableNotification(context: Context, tagName: String, downloadUrl: String) {
    val openIntent = PendingIntent.getActivity(
        context,
        APP_UPDATE_NOTIFICATION_ID,
        Intent(Intent.ACTION_VIEW, android.net.Uri.parse(downloadUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, APP_UPDATES_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("iAttend Update Available ($tagName)")
        .setContentText("A new version of iAttend is ready to download. Tap to install.")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .setContentIntent(openIntent)
        .build()

    NotificationManagerCompat.from(context).notify(APP_UPDATE_NOTIFICATION_ID, notification)
}

private fun actionPendingIntent(context: Context, occurrenceId: Long, status: String): PendingIntent {
    val intent = Intent(context, NotificationActionReceiver::class.java).apply {
        action = ACTION_MARK_STATUS
        putExtra(EXTRA_OCCURRENCE_ID, occurrenceId)
        putExtra(EXTRA_ACTION_STATUS, status)
    }
    val statusCode = when (status) { "PRESENT" -> 1; "ABSENT" -> 2; else -> 3 }
    val requestCode = occurrenceId.toInt() * 4 + statusCode
    return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}
