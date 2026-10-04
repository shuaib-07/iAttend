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

/** Fired by [AssessmentReminderScheduler] at a configured offset before a test/exam - shows the reminder notification. */
@AndroidEntryPoint
class AssessmentReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var assessmentDao: AssessmentDao
    @Inject lateinit var classOccurrenceDao: ClassOccurrenceDao
    @Inject lateinit var subjectDao: SubjectDao
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        val assessmentId = intent.getLongExtra(EXTRA_ASSESSMENT_ID, -1L)
        if (assessmentId < 0) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val assessment = assessmentDao.getByIdOnce(assessmentId) ?: return@launch
                if (assessment.needsRescheduling) return@launch

                if (assessment.linkedOccurrenceId != null) {
                    val occurrence = classOccurrenceDao.getByIdOnce(assessment.linkedOccurrenceId)
                    if (occurrence == null || occurrence.status == OccurrenceStatus.CANCELLED) {
                        return@launch
                    }
                }

                val subject = subjectDao.getByIdOnce(assessment.subjectId)
                if (subject != null) {
                    val settings = settingsRepository.settings.first()
                    ensureReminderChannel(context)
                    showAssessmentReminderNotification(context, assessment, subject.name, settings.timeFormat)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
