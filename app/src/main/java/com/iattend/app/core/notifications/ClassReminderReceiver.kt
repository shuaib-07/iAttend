package com.iattend.app.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.iattend.app.core.data.db.ClassOccurrenceDao
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.core.data.db.SubjectDao
import com.iattend.app.core.datastore.SettingsRepository
import kotlinx.coroutines.flow.first
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
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        val occurrenceId = intent.getLongExtra(EXTRA_OCCURRENCE_ID, -1L)
        if (occurrenceId < 0) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val occurrence = classOccurrenceDao.getByIdOnce(occurrenceId)
                if (occurrence == null || occurrence.status != OccurrenceStatus.UNMARKED) return@launch
                val subject = subjectDao.getByIdOnce(occurrence.subjectId)
                if (subject != null) {
                    val settings = settingsRepository.settings.first()
                    ensureReminderChannel(context)
                    showClassReminderNotification(context, occurrence, subject.name, settings.timeFormat)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
