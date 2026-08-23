package com.iattend.app.core.notifications

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.iattend.app.core.data.export.ExportImportRepository
import com.iattend.app.core.datastore.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val BACKUP_FILENAME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
private const val BACKUP_FILENAME_PREFIX = "iattend-backup-"
private const val KEEP_LAST_N_BACKUPS = 7

/** Periodic (daily/weekly) automated backup - reuses [ExportImportRepository.export] verbatim, just
 * schedules it and writes the result to disk with retention pruning. WorkManager, not AlarmManager:
 * this doesn't need to-the-minute precision, unlike class/assessment reminders. */
@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val exportImportRepository: ExportImportRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.settings.first()
        if (!settings.autoBackupEnabled) return Result.success()

        val json = exportImportRepository.export()
        val filename = "$BACKUP_FILENAME_PREFIX${LocalDateTime.now().format(BACKUP_FILENAME_FORMAT)}.json"
        val folderUri = settings.autoBackupFolderUri

        return try {
            if (folderUri != null) {
                writeToSafFolder(folderUri, filename, json)
            } else {
                writeToAppPrivateFolder(filename, json)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun writeToSafFolder(folderUri: String, filename: String, json: String) {
        val tree = DocumentFile.fromTreeUri(applicationContext, Uri.parse(folderUri)) ?: return
        val file = tree.createFile("application/json", filename) ?: return
        applicationContext.contentResolver.openOutputStream(file.uri)?.use { it.write(json.toByteArray()) }

        val existing = tree.listFiles()
            .filter { it.name?.startsWith(BACKUP_FILENAME_PREFIX) == true }
            .sortedByDescending { it.name }
        existing.drop(KEEP_LAST_N_BACKUPS).forEach { it.delete() }
    }

    private fun writeToAppPrivateFolder(filename: String, json: String) {
        val dir = java.io.File(applicationContext.getExternalFilesDir(null), "backups").apply { mkdirs() }
        java.io.File(dir, filename).writeText(json)

        val existing = dir.listFiles { f -> f.name.startsWith(BACKUP_FILENAME_PREFIX) }
            ?.sortedByDescending { it.name } ?: emptyList()
        existing.drop(KEEP_LAST_N_BACKUPS).forEach { it.delete() }
    }
}
