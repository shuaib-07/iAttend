package com.iattend.app.core.crash

import android.content.Context
import android.os.Build
import java.io.File
import java.time.Instant

/**
 * Local-only crash logger: no account/dashboard needed, mirrors the existing
 * "email the developer" pattern in DeveloperSupportScreen. Writes the last
 * uncaught exception to a file so the next launch can offer to send it.
 */
object CrashReporter {
    private const val FILE_NAME = "last_crash.txt"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val log = buildString {
                    appendLine("Time: ${Instant.now()}")
                    appendLine("App version: ${versionName(appContext)}")
                    appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                    appendLine()
                    appendLine(throwable.stackTraceToString())
                }
                File(appContext.filesDir, FILE_NAME).writeText(log)
            } catch (_: Exception) {
                // best-effort - never let the crash reporter crash the crash handler
            }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    fun pendingLog(context: Context): String? {
        val file = File(context.applicationContext.filesDir, FILE_NAME)
        return if (file.exists()) file.readText() else null
    }

    fun clearPendingLog(context: Context) {
        File(context.applicationContext.filesDir, FILE_NAME).delete()
    }

    private fun versionName(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    } catch (_: Exception) {
        "unknown"
    }
}
