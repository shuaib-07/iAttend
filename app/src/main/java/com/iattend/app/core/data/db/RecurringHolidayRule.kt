package com.iattend.app.core.data.db

import androidx.room.Entity
import com.iattend.app.core.data.export.DayOfWeekSerializer
import kotlinx.serialization.Serializable
import java.time.DayOfWeek

@Serializable
enum class RecurringHolidayMode { NONE, ALWAYS, PATTERN }

/**
 * One fixed row per weekday. weeksOfMonth is a bitmask (bit 0 = 1st occurrence
 * in the month ... bit 4 = 5th), only meaningful when mode = PATTERN.
 */
@Serializable
@Entity(tableName = "recurring_holiday_rules", primaryKeys = ["dayOfWeek"])
data class RecurringHolidayRule(
    @Serializable(with = DayOfWeekSerializer::class) val dayOfWeek: DayOfWeek,
    val mode: RecurringHolidayMode = RecurringHolidayMode.NONE,
    val weeksOfMonth: Int = 0
)
