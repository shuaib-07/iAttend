package com.iattend.app.core.updates

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

sealed class UpdateDownloadState {
    data object Idle : UpdateDownloadState()
    data class Downloading(val progress: Float, val bytesDownloaded: Long, val totalBytes: Long) : UpdateDownloadState()
    data class ReadyToInstall(val apkFile: File) : UpdateDownloadState()
    data class Error(val message: String) : UpdateDownloadState()
}

object UpdateInstaller {

    private const val UPDATES_DIR = "updates"

    fun getUpdatesDir(context: Context): File {
        val dir = File(context.cacheDir, UPDATES_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getTargetApkFile(context: Context, tagName: String): File {
        val safeTag = tagName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        return File(getUpdatesDir(context), "iAttend-$safeTag.apk")
    }

    fun getExistingDownloadedApk(context: Context, tagName: String): File? {
        val file = getTargetApkFile(context, tagName)
        return if (file.exists() && file.length() > 100_000L) file else null
    }

    suspend fun downloadApk(
        downloadUrl: String,
        destinationFile: File,
        onProgress: (progress: Float, downloaded: Long, total: Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        val parent = destinationFile.parentFile ?: File(destinationFile.path).parentFile
        parent?.mkdirs()
        val tempFile = File(destinationFile.parentFile, "${destinationFile.name}.tmp")
        if (tempFile.exists()) tempFile.delete()

        val connection = openConnectionWithRedirects(downloadUrl)
        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            throw IOException("Server returned HTTP $responseCode")
        }

        val totalBytes = connection.contentLengthLong.let { if (it <= 0) connection.contentLength.toLong() else it }
        var bytesDownloaded = 0L

        try {
            connection.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        ensureActive()
                        output.write(buffer, 0, read)
                        bytesDownloaded += read
                        val progress = if (totalBytes > 0) {
                            (bytesDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f)
                        } else 0f
                        onProgress(progress, bytesDownloaded, totalBytes)
                    }
                    output.flush()
                }
            }

            if (destinationFile.exists()) destinationFile.delete()
            if (!tempFile.renameTo(destinationFile)) {
                tempFile.copyTo(destinationFile, overwrite = true)
                tempFile.delete()
            }
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            throw e
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnectionWithRedirects(initialUrl: String): HttpURLConnection {
        var currentUrl = URL(initialUrl)
        var redirects = 0
        while (redirects < 5) {
            val conn = (currentUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "iAttend-AndroidApp")
                setRequestProperty("Accept", "application/octet-stream")
                connectTimeout = 15000
                readTimeout = 15000
                instanceFollowRedirects = true
            }
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                code == HttpURLConnection.HTTP_MOVED_TEMP ||
                code == HttpURLConnection.HTTP_SEE_OTHER ||
                code == 307 || code == 308
            ) {
                val location = conn.getHeaderField("Location") ?: break
                conn.disconnect()
                currentUrl = URL(if (location.startsWith("http")) location else URL(currentUrl, location).toString())
                redirects++
            } else {
                return conn
            }
        }
        return currentUrl.openConnection() as HttpURLConnection
    }

    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) {
            Toast.makeText(context, "Update file not found. Please download again.", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                Toast.makeText(
                    context,
                    "Please allow iAttend to install updates",
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to open installer: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        return String.format(Locale.getDefault(), "%.1f %s", size, units[unitIndex])
    }
}
