package com.iattend.app.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.iattend.app.core.data.export.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
@Entity(tableName = "timetable_versions")
data class TimetableVersion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String? = null,
    @Serializable(with = LocalDateSerializer::class) val effectiveFrom: LocalDate,
    val createdAt: Long = System.currentTimeMillis()
)
