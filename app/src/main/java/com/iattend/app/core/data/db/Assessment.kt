package com.iattend.app.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.iattend.app.core.data.export.LocalDateSerializer
import com.iattend.app.core.data.export.LocalTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime

@Serializable
enum class AssessmentType { TEST, EXAM }

@Serializable
@Entity(
    tableName = "assessments",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId"), Index("date")]
)
data class Assessment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val type: AssessmentType,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    @Serializable(with = LocalTimeSerializer::class) val startTime: LocalTime,
    @Serializable(with = LocalTimeSerializer::class) val endTime: LocalTime,
    val totalMarks: Int? = null,
    val portions: String? = null,
    /** Comma-separated minutes-before-event list, e.g. "60,1440". Prefilled from the type's global default at creation, editable after. */
    val reminderOffsets: String = "",
    val title: String? = null
)
