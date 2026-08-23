package com.iattend.app.core.updates

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.iattend.app.core.datastore.SettingsRepository
import com.iattend.app.core.notifications.ensureReminderChannel
import com.iattend.app.core.notifications.showUpdateAvailableNotification
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class UpdateCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val releaseRepository: ReleaseRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.settings.first()
        if (!settings.autoCheckUpdates) return Result.success()

        return try {
            val result = releaseRepository.checkForUpdates()
            result.onSuccess { release ->
                if (release.isUpdateAvailable) {
                    ensureReminderChannel(applicationContext)
                    showUpdateAvailableNotification(
                        context = applicationContext,
                        tagName = release.tagName,
                        downloadUrl = release.downloadUrl
                    )
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
