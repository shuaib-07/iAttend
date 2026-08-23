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
enum class OccurrenceStatus { UNMARKED, PRESENT, ABSENT, CANCELLED }
@Serializable
enum class OccurrenceSource { SCHEDULED, EXTRA }

@Serializable
@Entity(
    tableName = "class_occurrences",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TimetableSlot::class,
            parentColumns = ["id"],
            childColumns = ["timetableSlotId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("subjectId"), Index("timetableSlotId"), Index("date")]
)
data class ClassOccurrence(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    @Serializable(with = LocalTimeSerializer::class) val startTime: LocalTime?,
    @Serializable(with = LocalTimeSerializer::class) val endTime: LocalTime?,
    val status: OccurrenceStatus = OccurrenceStatus.UNMARKED,
    val source: OccurrenceSource,
    val timetableSlotId: Long?,
    val classCount: Int = 1,
    val roomNumber: String? = null,
    val classType: ClassType = ClassType.LECTURE,
    /** Only meaningful when status == CANCELLED. */
    val cancelReason: String? = null
)
