package com.iattend.app.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.iattend.app.core.data.export.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
enum class TotalMode { KNOWN, COMPUTED, OPEN_ENDED }

@Serializable
@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val colorArgb: Int,
    val teacherName: String? = null,
    val requiredPercentageOverride: Float? = null,
    val totalMode: TotalMode = TotalMode.OPEN_ENDED,
    val knownTotalClasses: Int? = null,
    /** COMPUTED mode only. Null = fall back to Settings.trackingEndDate (global default). */
    @Serializable(with = LocalDateSerializer::class) val trackingEndDateOverride: LocalDate? = null,
    /** Comma-separated minutes-before-class list. Null = fall back to Settings.classReminderDefaultOffsets. */
    val reminderOffsetsOverride: String? = null
)
