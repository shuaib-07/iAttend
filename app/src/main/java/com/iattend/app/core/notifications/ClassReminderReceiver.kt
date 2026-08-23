package com.iattend.app.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.iattend.app.core.data.db.ClassOccurrenceDao
import com.iattend.app.core.data.db.SubjectDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Fired by [ClassReminderScheduler] 5 minutes before a class - shows the reminder notification. */
@AndroidEntryPoint
class ClassReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var classOccurrenceDao: ClassOccurrenceDao
    @Inject lateinit var subjectDao: SubjectDao

    override fun onReceive(context: Context, intent: Intent) {
        val occurrenceId = intent.getLongExtra(EXTRA_OCCURRENCE_ID, -1L)
        if (occurrenceId < 0) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val occurrence = classOccurrenceDao.getByIdOnce(occurrenceId)
                val subject = occurrence?.let { subjectDao.getByIdOnce(it.subjectId) }
                if (occurrence != null && subject != null) {
                    ensureReminderChannel(context)
                    showClassReminderNotification(context, occurrence, subject.name)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
