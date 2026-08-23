package com.iattend.app.core.data.db

import androidx.room.Dao
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringHolidayRuleDao {
    @Upsert
    suspend fun upsert(rule: RecurringHolidayRule)

    @Upsert
    suspend fun upsertAll(rules: List<RecurringHolidayRule>)

    @Query("SELECT * FROM recurring_holiday_rules")
    fun getAll(): Flow<List<RecurringHolidayRule>>

    @Query("SELECT * FROM recurring_holiday_rules")
    suspend fun getAllOnce(): List<RecurringHolidayRule>
}
