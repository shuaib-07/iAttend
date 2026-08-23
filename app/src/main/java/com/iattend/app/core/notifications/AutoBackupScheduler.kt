package com.iattend.app.core.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.iattend.app.core.datastore.BackupFrequency
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val AUTO_BACKUP_WORK_NAME = "auto_backup"

@Singleton
class AutoBackupScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    fun reschedule(enabled: Boolean, frequency: BackupFrequency) {
        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            workManager.cancelUniqueWork(AUTO_BACKUP_WORK_NAME)
            return
        }
        val intervalDays = if (frequency == BackupFrequency.DAILY) 1L else 7L
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(intervalDays, TimeUnit.DAYS).build()
        workManager.enqueueUniquePeriodicWork(AUTO_BACKUP_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
