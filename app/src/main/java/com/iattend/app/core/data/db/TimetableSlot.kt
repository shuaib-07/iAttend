package com.iattend.app.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.iattend.app.core.data.export.DayOfWeekSerializer
import com.iattend.app.core.data.export.LocalTimeSerializer
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalTime

@Serializable
@Entity(
    tableName = "timetable_slots",
    foreignKeys = [
        ForeignKey(
            entity = TimetableVersion::class,
            parentColumns = ["id"],
            childColumns = ["timetableVersionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("timetableVersionId"), Index("subjectId")]
)
data class TimetableSlot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timetableVersionId: Long,
    val subjectId: Long,
    @Serializable(with = DayOfWeekSerializer::class) val dayOfWeek: DayOfWeek,
    @Serializable(with = LocalTimeSerializer::class) val startTime: LocalTime,
    @Serializable(with = LocalTimeSerializer::class) val endTime: LocalTime,
    val classCount: Int = 1,
    val roomNumber: String? = null,
    val classType: ClassType = ClassType.LECTURE
)
