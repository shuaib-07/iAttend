package com.iattend.app.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Exact alarms don't survive reboot - re-arm today's reminders and the midnight chain. */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: ClassReminderScheduler
    @Inject lateinit var assessmentScheduler: AssessmentReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                scheduler.scheduleTodayReminders()
                scheduler.scheduleMidnightRefresh()
                assessmentScheduler.rescheduleAllUpcoming()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
