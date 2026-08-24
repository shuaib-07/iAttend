package com.iattend.app.core.updates

import android.content.Context
import com.iattend.app.core.AppLinks
import com.iattend.app.core.datastore.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class AppReleaseInfo(
    val tagName: String,
    val name: String,
    val body: String,
    val publishedAt: String,
    val htmlUrl: String,
    val downloadUrl: String,
    val isUpdateAvailable: Boolean
)

data class FeatureHighlight(
    val category: String,
    val title: String,
    val description: String
)

@Singleton
class ReleaseRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    fun getCurrentVersionName(): String {
        return runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "0.1"
    }

    suspend fun checkForUpdates(): Result<AppReleaseInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(AppLinks.LATEST_RELEASE_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "iAttend-AndroidApp")
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode == 200) {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonStr)

                val tagName = json.optString("tag_name", "v${getCurrentVersionName()}")
                val name = json.optString("name", "iAttend $tagName")
                val body = json.optString("body", "")
                val publishedAt = json.optString("published_at", "")
                val htmlUrl = json.optString("html_url", AppLinks.RELEASES_PAGE_URL)

                var downloadUrl = htmlUrl
                val assets = json.optJSONArray("assets")
                if (assets != null && assets.length() > 0) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val assetName = asset.optString("name", "")
                        if (assetName.endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = asset.optString("browser_download_url", htmlUrl)
                            break
                        }
                    }
                }

                val currentVersion = getCurrentVersionName()
                val isUpdateAvailable = isVersionNewer(latestVersion = tagName, currentVersion = currentVersion)

                AppReleaseInfo(
                    tagName = tagName,
                    name = name,
                    body = body,
                    publishedAt = publishedAt,
                    htmlUrl = htmlUrl,
                    downloadUrl = downloadUrl,
                    isUpdateAvailable = isUpdateAvailable
                )
            } else {
                throw Exception("HTTP Error: ${connection.responseCode}")
            }
        }
    }

    fun isVersionNewer(latestVersion: String, currentVersion: String): Boolean {
        val cleanLatest = latestVersion.removePrefix("v").trim()
        val cleanCurrent = currentVersion.removePrefix("v").trim()

        val latestParts = cleanLatest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    fun getBuiltInReleaseNotes(): List<FeatureHighlight> {
        return listOf(
            FeatureHighlight(
                category = "NEW FEATURE",
                title = "Interactive Live Tour (21 Steps)",
                description = "Learn how to use iAttend with a live spotlight tour directly on the real screens."
            ),
            FeatureHighlight(
                category = "NEW FEATURE",
                title = "Onboarding Avatar Picker",
                description = "Choose your favorite avatar or upload a custom image directly during onboarding."
            ),
            FeatureHighlight(
                category = "NEW FEATURE",
                title = "Data Reset (Danger Zone)",
                description = "Easily reset academic data for a fresh semester or perform a full app wipe in Settings."
            ),
            FeatureHighlight(
                category = "BUG FIX",
                title = "Stability & One UI Fixes",
                description = "Fixed Insights 0% bar crash, Samsung alarm crash risk, One UI settings scroll, and added CrashReporter."
            )
        )
    }
}
