package com.iattend.app.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.iattend.app.core.data.db.AssessmentDao
import com.iattend.app.core.data.db.ClassOccurrenceDao
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.core.data.db.SubjectDao
import com.iattend.app.core.datastore.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Fired by [ClassReminderScheduler] 5 minutes before a class - shows the reminder notification. */
@AndroidEntryPoint
class ClassReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var classOccurrenceDao: ClassOccurrenceDao
    @Inject lateinit var assessmentDao: AssessmentDao
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
                    val linkedTests = assessmentDao.getForOccurrenceOnce(occurrence.id).filter { !it.needsRescheduling }
                    if (linkedTests.isNotEmpty()) {
                        showCombinedClassAndAssessmentReminderNotification(
                            context = context,
                            occurrence = occurrence,
                            assessment = linkedTests.first(),
                            subjectName = subject.name,
                            timeFormat = settings.timeFormat
                        )
                    } else {
                        showClassReminderNotification(context, occurrence, subject.name, settings.timeFormat)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
