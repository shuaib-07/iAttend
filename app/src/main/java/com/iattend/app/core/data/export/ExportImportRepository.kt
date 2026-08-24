package com.iattend.app.core.data.export

import androidx.room.withTransaction
import com.iattend.app.core.data.db.AppDatabase
import com.iattend.app.core.data.db.Assessment
import com.iattend.app.core.data.db.AssessmentDao
import com.iattend.app.core.data.db.ClassOccurrence
import com.iattend.app.core.data.db.ClassOccurrenceDao
import com.iattend.app.core.data.db.Holiday
import com.iattend.app.core.data.db.HolidayDao
import com.iattend.app.core.data.db.RecurringHolidayRule
import com.iattend.app.core.data.db.RecurringHolidayRuleDao
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.data.db.SubjectDao
import com.iattend.app.core.data.db.TimetableSlot
import com.iattend.app.core.data.db.TimetableSlotDao
import com.iattend.app.core.data.db.TimetableVersion
import com.iattend.app.core.data.db.TimetableVersionDao
import com.iattend.app.core.datastore.AppSettings
import com.iattend.app.core.datastore.Profile
import com.iattend.app.core.datastore.ProfileRepository
import com.iattend.app.core.datastore.SettingsRepository
import com.iattend.app.core.data.db.OccurrenceSource
import com.iattend.app.core.domain.occurrence.OccurrenceRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/** Full local-DB snapshot (Product Plan §11): every entity table plus Settings/Profile. */
@Serializable
data class ExportPayload(
    val exportVersion: Int = 1,
    val subjects: List<Subject>,
    val timetableVersions: List<TimetableVersion>,
    val timetableSlots: List<TimetableSlot>,
    val classOccurrences: List<ClassOccurrence>,
    val holidays: List<Holiday>,
    val recurringHolidayRules: List<RecurringHolidayRule>,
    val settings: AppSettings,
    val profile: Profile,
    val assessments: List<Assessment> = emptyList()
)

@Serializable
data class TemplateSettings(
    @Serializable(with = LocalDateSerializer::class) val trackingStartDate: java.time.LocalDate? = null,
    @Serializable(with = LocalDateSerializer::class) val trackingEndDate: java.time.LocalDate? = null,
    val requiredPercentageDefault: Float = 75f
)

/** Timetable template for sharing between classmates (subjects + timetable + holidays + optional extras). */
@Serializable
data class TemplatePayload(
    val exportType: String = "TEMPLATE",
    val templateVersion: Int = 1,
    val subjects: List<Subject>,
    val timetableVersions: List<TimetableVersion>,
    val timetableSlots: List<TimetableSlot>,
    val holidays: List<Holiday>,
    val recurringHolidayRules: List<RecurringHolidayRule>,
    val extraClasses: List<ClassOccurrence> = emptyList(),
    val assessments: List<Assessment> = emptyList(),
    val templateSettings: TemplateSettings? = null
)

@Singleton
class ExportImportRepository @Inject constructor(
    private val db: AppDatabase,
    private val subjectDao: SubjectDao,
    private val timetableVersionDao: TimetableVersionDao,
    private val timetableSlotDao: TimetableSlotDao,
    private val classOccurrenceDao: ClassOccurrenceDao,
    private val holidayDao: HolidayDao,
    private val recurringHolidayRuleDao: RecurringHolidayRuleDao,
    private val settingsRepository: SettingsRepository,
    private val profileRepository: ProfileRepository,
    private val occurrenceRepository: OccurrenceRepository,
    private val assessmentDao: AssessmentDao
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }

    suspend fun export(): String = json.encodeToString(
        ExportPayload(
            subjects = subjectDao.getAllOnce(),
            timetableVersions = timetableVersionDao.getAllOnce(),
            timetableSlots = timetableSlotDao.getAllOnce(),
            classOccurrences = classOccurrenceDao.getAllOnce(),
            holidays = holidayDao.getAllOnce(),
            recurringHolidayRules = recurringHolidayRuleDao.getAllOnce(),
            settings = settingsRepository.settings.first(),
            profile = profileRepository.profile.first(),
            assessments = assessmentDao.getAllOnce()
        )
    )

    /** Replaces the local DB wholesale (Product Plan §11: no field-level merge in v1). */
    suspend fun import(jsonText: String) {
        val payload = json.decodeFromString<ExportPayload>(jsonText)
        db.withTransaction {
            db.clearAllTables()
            subjectDao.insertAll(payload.subjects)
            timetableVersionDao.insertAll(payload.timetableVersions)
            timetableSlotDao.insertAll(payload.timetableSlots)
            classOccurrenceDao.insertAll(payload.classOccurrences)
            holidayDao.insertAll(payload.holidays)
            recurringHolidayRuleDao.upsertAll(payload.recurringHolidayRules)
            if (payload.assessments.isNotEmpty()) assessmentDao.insertAll(payload.assessments)
        }
        settingsRepository.replaceAll(payload.settings)
        profileRepository.replaceAll(payload.profile)
    }

    suspend fun exportTemplate(
        selectedSubjectIds: Set<Long>? = null,
        includeExtraClasses: Boolean = true,
        includeAssessments: Boolean = true
    ): String {
        val allSubjects = subjectDao.getAllOnce()
        val filteredSubjects = if (selectedSubjectIds.isNullOrEmpty()) allSubjects
        else allSubjects.filter { it.id in selectedSubjectIds }
        val filteredSubjectIds = filteredSubjects.map { it.id }.toSet()
        val allSlots = timetableSlotDao.getAllOnce()
        val filteredSlots = if (filteredSubjectIds.size == allSubjects.size) allSlots
        else allSlots.filter { it.subjectId in filteredSubjectIds }
        val extraClasses = if (includeExtraClasses) {
            classOccurrenceDao.getAllOnce()
                .filter { it.source == OccurrenceSource.EXTRA && it.subjectId in filteredSubjectIds }
        } else emptyList()
        val assessments = if (includeAssessments) {
            assessmentDao.getAllOnce().filter { it.subjectId in filteredSubjectIds }
        } else emptyList()
        val settings = settingsRepository.settings.first()
        val payload = TemplatePayload(
            subjects = filteredSubjects,
            timetableVersions = timetableVersionDao.getAllOnce(),
            timetableSlots = filteredSlots,
            holidays = holidayDao.getAllOnce(),
            recurringHolidayRules = recurringHolidayRuleDao.getAllOnce(),
            extraClasses = extraClasses,
            assessments = assessments,
            templateSettings = TemplateSettings(
                trackingStartDate = settings.trackingStartDate,
                trackingEndDate = settings.trackingEndDate,
                requiredPercentageDefault = settings.requiredPercentageDefault
            )
        )
        return json.encodeToString(payload)
    }

    suspend fun importTemplate(jsonText: String) {
        val payload = json.decodeFromString<TemplatePayload>(jsonText)
        db.withTransaction {
            db.clearAllTables()
            val subjectMap = mutableMapOf<Long, Long>()
            for (subject in payload.subjects) {
                val oldId = subject.id
                val newId = subjectDao.insert(subject.copy(id = 0))
                subjectMap[oldId] = newId
            }
            val versionMap = mutableMapOf<Long, Long>()
            for (version in payload.timetableVersions) {
                val oldId = version.id
                val newId = timetableVersionDao.insert(version.copy(id = 0))
                versionMap[oldId] = newId
            }
            val remappedSlots = payload.timetableSlots.map { slot ->
                slot.copy(
                    id = 0,
                    timetableVersionId = versionMap[slot.timetableVersionId]
                        ?: error("Missing timetableVersion ${slot.timetableVersionId}"),
                    subjectId = subjectMap[slot.subjectId]
                        ?: error("Missing subject ${slot.subjectId}")
                )
            }
            if (remappedSlots.isNotEmpty()) timetableSlotDao.insertAll(remappedSlots)
            if (payload.holidays.isNotEmpty()) {
                holidayDao.insertAll(payload.holidays.map { it.copy(id = 0) })
            }
            if (payload.recurringHolidayRules.isNotEmpty()) {
                recurringHolidayRuleDao.upsertAll(payload.recurringHolidayRules)
            }
            if (payload.extraClasses.isNotEmpty()) {
                val remappedExtras = payload.extraClasses.map { occ ->
                    occ.copy(
                        id = 0,
                        subjectId = subjectMap[occ.subjectId]
                            ?: error("Missing subject for extra ${occ.subjectId}"),
                        timetableSlotId = null
                    )
                }
                classOccurrenceDao.insertAll(remappedExtras)
            }
            if (payload.assessments.isNotEmpty()) {
                val remappedAssessments = payload.assessments.map { assessment ->
                    assessment.copy(
                        id = 0,
                        subjectId = subjectMap[assessment.subjectId]
                            ?: error("Missing subject for assessment ${assessment.subjectId}")
                    )
                }
                assessmentDao.insertAll(remappedAssessments)
            }
        }
        payload.templateSettings?.let { s ->
            settingsRepository.applyTemplateSettings(s)
        }
        occurrenceRepository.regenerateUnmarkedWindow()
    }

    /** Wipes subjects/timetable/attendance/holidays/assessments; keeps theme, profile name and
     * reminder/backup preferences. The tracking dates and required-% default are reset too since
     * they're meaningless once every subject they applied to is gone. */
    suspend fun resetAcademicData() {
        db.withTransaction { db.clearAllTables() }
        settingsRepository.setTrackingStartDate(null)
        settingsRepository.setTrackingEndDate(null)
        settingsRepository.setRequiredPercentageDefault(75f)
    }

    /** Full wipe back to a fresh install: every DB table plus every setting and profile field,
     * including onboardingComplete - sends the user back through onboarding. */
    suspend fun resetFull() {
        db.withTransaction { db.clearAllTables() }
        settingsRepository.replaceAll(AppSettings())
        profileRepository.replaceAll(Profile())
    }

    suspend fun importAuto(jsonText: String): Boolean {
        val isTemplate = try {
            val el = json.parseToJsonElement(jsonText).jsonObject
            val exportType = el["exportType"]?.jsonPrimitive?.content
            val hasTemplateVersion = el.containsKey("templateVersion")
            val hasFullBackupFields = el.containsKey("classOccurrences") || el.containsKey("settings") || el.containsKey("profile")
            exportType == "TEMPLATE" || hasTemplateVersion || !hasFullBackupFields
        } catch (_: Exception) {
            jsonText.contains("templateVersion") || !jsonText.contains("classOccurrences")
        }

        return if (isTemplate) {
            importTemplate(jsonText)
            true
        } else {
            import(jsonText)
            false
        }
    }
}
