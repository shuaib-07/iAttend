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
enum class AssessmentAttemptStatus { PENDING, ATTEMPTED, MISSED }

@Serializable
@Entity(
    tableName = "assessments",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ClassOccurrence::class,
            parentColumns = ["id"],
            childColumns = ["linkedOccurrenceId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("subjectId"),
        Index("date"),
        Index("linkedOccurrenceId")
    ]
)
data class Assessment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val type: AssessmentType,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    @Serializable(with = LocalTimeSerializer::class) val startTime: LocalTime,
    @Serializable(with = LocalTimeSerializer::class) val endTime: LocalTime,
    val totalMarks: Double? = null,
    val portions: String? = null,
    /** Comma-separated minutes-before-event list, e.g. "60,1440". Prefilled from the type's global default at creation, editable after. */
    val reminderOffsets: String = "",
    val title: String? = null,
    val obtainedMarks: Double? = null,
    val weight: Double? = null,
    val attemptStatus: AssessmentAttemptStatus = AssessmentAttemptStatus.PENDING,
    val isAttemptOverridden: Boolean = false,
    val linkedOccurrenceId: Long? = null,
    val needsRescheduling: Boolean = false
)
