package com.iattend.app.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.glance.appwidget.updateAll
import com.iattend.app.core.data.db.ClassOccurrenceDao
import com.iattend.app.core.data.db.OccurrenceStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Fired when the user taps Present/Absent directly on a class-reminder notification. */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {
    @Inject lateinit var classOccurrenceDao: ClassOccurrenceDao

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MARK_STATUS) return
        val occurrenceId = intent.getLongExtra(EXTRA_OCCURRENCE_ID, -1L)
        val statusExtra = intent.getStringExtra(EXTRA_ACTION_STATUS) ?: return
        if (occurrenceId < 0) return

        val status = when (statusExtra) {
            "PRESENT" -> OccurrenceStatus.PRESENT
            "ABSENT" -> OccurrenceStatus.ABSENT
            "CANCELLED" -> OccurrenceStatus.CANCELLED
            else -> return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                classOccurrenceDao.getByIdOnce(occurrenceId)?.let {
                    classOccurrenceDao.update(it.copy(status = status))
                }
                // Both the pre-class and post-class reminders act on the same occurrence and can
                // be showing at once - dismiss whichever is up (cancelling a non-existent id is a no-op).
                NotificationManagerCompat.from(context).cancel(occurrenceId.toInt())
                NotificationManagerCompat.from(context).cancel(CLASS_END_NOTIFICATION_ID_OFFSET + occurrenceId.toInt())
                com.iattend.app.widget.UpcomingClassesWidget().updateAll(context = context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
