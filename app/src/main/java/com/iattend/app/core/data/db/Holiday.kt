package com.iattend.app.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.iattend.app.core.data.export.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
@Entity(tableName = "holidays")
data class Holiday(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @Serializable(with = LocalDateSerializer::class) val startDate: LocalDate,
    @Serializable(with = LocalDateSerializer::class) val endDate: LocalDate,
    val label: String
)
