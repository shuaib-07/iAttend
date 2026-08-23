package com.iattend.app.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.iattend.app.core.data.db.ClassOccurrenceDao
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.core.data.db.SubjectDao
import com.iattend.app.core.datastore.SettingsRepository
import com.iattend.app.core.domain.stats.AttendanceStatsCalculator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Fired by [ClassReminderScheduler] at a class's end time - prompts marking attendance right
 * after the class, distinct from [ClassReminderReceiver]'s before-class heads-up. */
@AndroidEntryPoint
class ClassEndReceiver : BroadcastReceiver() {
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
                // Already marked (in-app or via the pre-class notification) - nothing left to prompt for.
                if (occurrence == null || occurrence.status != OccurrenceStatus.UNMARKED) return@launch
                val subject = subjectDao.getByIdOnce(occurrence.subjectId) ?: return@launch

                val settings = settingsRepository.settings.first()
                val requiredPercentage = subject.requiredPercentageOverride ?: settings.requiredPercentageDefault
                val stats = AttendanceStatsCalculator.compute(
                    subject,
                    classOccurrenceDao.getForSubjectOnce(subject.id),
                    requiredPercentage,
                    settings.trackingEndDate
                )

                ensureReminderChannel(context)
                showClassEndNotification(context, occurrence, subject, stats.classesCanSkip)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
