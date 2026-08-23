package com.iattend.app.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Self-chaining: fires at midnight, schedules the new day's reminders, re-arms itself for tomorrow. */
@AndroidEntryPoint
class MidnightRefreshReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: ClassReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                scheduler.scheduleTodayReminders()
                scheduler.scheduleMidnightRefresh()
                com.iattend.app.widget.UpcomingClassesWidget().updateAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
