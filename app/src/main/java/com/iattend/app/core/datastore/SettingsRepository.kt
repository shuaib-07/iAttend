package com.iattend.app.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.iattend.app.core.data.export.LocalDateSerializer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

@Serializable
enum class ThemeMode { LIGHT, DARK, AMOLED, SYSTEM }

@Serializable
enum class NavBarStyle { PILL, CAPSULE }

@Serializable
enum class BackupFrequency { DAILY, WEEKLY }

@Serializable
data class AppSettings(
    val onboardingComplete: Boolean = false,
    val requiredPercentageDefault: Float = 75f,
    @Serializable(with = LocalDateSerializer::class) val trackingStartDate: LocalDate? = null,
    @Serializable(with = LocalDateSerializer::class) val trackingEndDate: LocalDate? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val accentColor: String = "FOREST",
    val remindersEnabled: Boolean = false,
    val navBarStyle: NavBarStyle = NavBarStyle.PILL,
    /** Comma-separated minutes-before-class list. */
    val classReminderDefaultOffsets: String = "10",
    /** Comma-separated minutes-before-assessment list, shared by Test and Exam defaults. */
    val examReminderDefaultOffsets: String = "60,1440",
    val autoBackupEnabled: Boolean = false,
    val autoBackupFrequency: BackupFrequency = BackupFrequency.WEEKLY,
    /** SAF persisted-permission tree URI. Null = write to the app-private external files dir instead. */
    val autoBackupFolderUri: String? = null,
    val lastSeenVersion: String = "",
    val autoCheckUpdates: Boolean = true
)

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore = context.settingsDataStore

    private object Keys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val REQUIRED_PERCENTAGE = floatPreferencesKey("required_percentage")
        val TRACKING_START_DATE = stringPreferencesKey("tracking_start_date")
        val TRACKING_END_DATE = stringPreferencesKey("tracking_end_date")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color_enabled")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val NAV_BAR_STYLE = stringPreferencesKey("nav_bar_style")
        val CLASS_REMINDER_DEFAULT_OFFSETS = stringPreferencesKey("class_reminder_default_offsets")
        val EXAM_REMINDER_DEFAULT_OFFSETS = stringPreferencesKey("exam_reminder_default_offsets")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val AUTO_BACKUP_FREQUENCY = stringPreferencesKey("auto_backup_frequency")
        val AUTO_BACKUP_FOLDER_URI = stringPreferencesKey("auto_backup_folder_uri")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val LAST_SEEN_VERSION = stringPreferencesKey("last_seen_version")
        val AUTO_CHECK_UPDATES = booleanPreferencesKey("auto_check_updates")
    }

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            onboardingComplete = prefs[Keys.ONBOARDING_COMPLETE] ?: false,
            requiredPercentageDefault = prefs[Keys.REQUIRED_PERCENTAGE] ?: 75f,
            trackingStartDate = prefs[Keys.TRACKING_START_DATE]?.let(LocalDate::parse),
            trackingEndDate = prefs[Keys.TRACKING_END_DATE]?.let(LocalDate::parse),
            themeMode = prefs[Keys.THEME_MODE]?.let(ThemeMode::valueOf) ?: ThemeMode.SYSTEM,
            dynamicColorEnabled = prefs[Keys.DYNAMIC_COLOR] ?: true,
            remindersEnabled = prefs[Keys.REMINDERS_ENABLED] ?: false,
            navBarStyle = prefs[Keys.NAV_BAR_STYLE]?.let(NavBarStyle::valueOf) ?: NavBarStyle.PILL,
            classReminderDefaultOffsets = prefs[Keys.CLASS_REMINDER_DEFAULT_OFFSETS] ?: "10",
            examReminderDefaultOffsets = prefs[Keys.EXAM_REMINDER_DEFAULT_OFFSETS] ?: "60,1440",
            autoBackupEnabled = prefs[Keys.AUTO_BACKUP_ENABLED] ?: false,
            autoBackupFrequency = prefs[Keys.AUTO_BACKUP_FREQUENCY]?.let(BackupFrequency::valueOf) ?: BackupFrequency.WEEKLY,
            autoBackupFolderUri = prefs[Keys.AUTO_BACKUP_FOLDER_URI],
            accentColor = prefs[Keys.ACCENT_COLOR] ?: "FOREST",
            lastSeenVersion = prefs[Keys.LAST_SEEN_VERSION] ?: "",
            autoCheckUpdates = prefs[Keys.AUTO_CHECK_UPDATES] ?: true
        )
    }

    suspend fun setAutoCheckUpdates(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_CHECK_UPDATES] = enabled }
    }

    suspend fun setLastSeenVersion(version: String) {
        dataStore.edit { it[Keys.LAST_SEEN_VERSION] = version }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setRequiredPercentageDefault(percentage: Float) {
        dataStore.edit { it[Keys.REQUIRED_PERCENTAGE] = percentage }
    }

    suspend fun setTrackingStartDate(date: LocalDate?) {
        dataStore.edit {
            if (date == null) it.remove(Keys.TRACKING_START_DATE) else it[Keys.TRACKING_START_DATE] = date.toString()
        }
    }

    suspend fun setTrackingEndDate(date: LocalDate?) {
        dataStore.edit {
            if (date == null) it.remove(Keys.TRACKING_END_DATE) else it[Keys.TRACKING_END_DATE] = date.toString()
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.REMINDERS_ENABLED] = enabled }
    }

    suspend fun setNavBarStyle(style: NavBarStyle) {
        dataStore.edit { it[Keys.NAV_BAR_STYLE] = style.name }
    }

    suspend fun setClassReminderDefaultOffsets(offsets: String) {
        dataStore.edit { it[Keys.CLASS_REMINDER_DEFAULT_OFFSETS] = offsets }
    }

    suspend fun setExamReminderDefaultOffsets(offsets: String) {
        dataStore.edit { it[Keys.EXAM_REMINDER_DEFAULT_OFFSETS] = offsets }
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_BACKUP_ENABLED] = enabled }
    }

    suspend fun setAutoBackupFrequency(frequency: BackupFrequency) {
        dataStore.edit { it[Keys.AUTO_BACKUP_FREQUENCY] = frequency.name }
    }

    suspend fun setAutoBackupFolderUri(uri: String?) {
        dataStore.edit {
            if (uri == null) it.remove(Keys.AUTO_BACKUP_FOLDER_URI) else it[Keys.AUTO_BACKUP_FOLDER_URI] = uri
        }
    }

    suspend fun setAccentColor(color: String) {
        dataStore.edit { it[Keys.ACCENT_COLOR] = color }
    }

    suspend fun applyTemplateSettings(template: com.iattend.app.core.data.export.TemplateSettings) {
        dataStore.edit { prefs ->
            prefs[Keys.REQUIRED_PERCENTAGE] = template.requiredPercentageDefault
            if (template.trackingStartDate == null) prefs.remove(Keys.TRACKING_START_DATE) else prefs[Keys.TRACKING_START_DATE] = template.trackingStartDate.toString()
            if (template.trackingEndDate == null) prefs.remove(Keys.TRACKING_END_DATE) else prefs[Keys.TRACKING_END_DATE] = template.trackingEndDate.toString()
        }
    }

    /** Full overwrite used by import (Product Plan §11): replaces every stored value at once. */
    suspend fun replaceAll(settings: AppSettings) {
        dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETE] = settings.onboardingComplete
            prefs[Keys.REQUIRED_PERCENTAGE] = settings.requiredPercentageDefault
            if (settings.trackingStartDate == null) prefs.remove(Keys.TRACKING_START_DATE) else prefs[Keys.TRACKING_START_DATE] = settings.trackingStartDate.toString()
            if (settings.trackingEndDate == null) prefs.remove(Keys.TRACKING_END_DATE) else prefs[Keys.TRACKING_END_DATE] = settings.trackingEndDate.toString()
            prefs[Keys.THEME_MODE] = settings.themeMode.name
            prefs[Keys.DYNAMIC_COLOR] = settings.dynamicColorEnabled
            prefs[Keys.REMINDERS_ENABLED] = settings.remindersEnabled
            prefs[Keys.NAV_BAR_STYLE] = settings.navBarStyle.name
            prefs[Keys.CLASS_REMINDER_DEFAULT_OFFSETS] = settings.classReminderDefaultOffsets
            prefs[Keys.EXAM_REMINDER_DEFAULT_OFFSETS] = settings.examReminderDefaultOffsets
            prefs[Keys.AUTO_BACKUP_ENABLED] = settings.autoBackupEnabled
            prefs[Keys.AUTO_BACKUP_FREQUENCY] = settings.autoBackupFrequency.name
            prefs[Keys.ACCENT_COLOR] = settings.accentColor
            prefs[Keys.AUTO_CHECK_UPDATES] = settings.autoCheckUpdates
            // autoBackupFolderUri deliberately not restored - a SAF tree URI grant is device/user-specific
            // and won't resolve on a different device or after a reinstall; falls back to the app-private default.
        }
    }
}
