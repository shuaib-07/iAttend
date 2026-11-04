package com.iattend.app.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.iattend.app.core.data.export.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
@Entity(tableName = "semesters")
data class Semester(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @Serializable(with = LocalDateSerializer::class) val startDate: LocalDate,
    @Serializable(with = LocalDateSerializer::class) val endDate: LocalDate,
    val targetPercentage: Float = 75f,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
