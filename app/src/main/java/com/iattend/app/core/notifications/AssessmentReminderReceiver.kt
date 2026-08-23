package com.iattend.app.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.iattend.app.core.data.db.AssessmentDao
import com.iattend.app.core.data.db.SubjectDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Fired by [AssessmentReminderScheduler] at a configured offset before a test/exam - shows the reminder notification. */
@AndroidEntryPoint
class AssessmentReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var assessmentDao: AssessmentDao
    @Inject lateinit var subjectDao: SubjectDao

    override fun onReceive(context: Context, intent: Intent) {
        val assessmentId = intent.getLongExtra(EXTRA_ASSESSMENT_ID, -1L)
        if (assessmentId < 0) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val assessment = assessmentDao.getByIdOnce(assessmentId)
                val subject = assessment?.let { subjectDao.getByIdOnce(it.subjectId) }
                if (assessment != null && subject != null) {
                    ensureReminderChannel(context)
                    showAssessmentReminderNotification(context, assessment, subject.name)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
