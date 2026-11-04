package com.iattend.app.core.data.db

import androidx.room.Dao
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

    @Query("SELECT * FROM recurring_holiday_rules WHERE semesterId = :semesterId")
    fun getForSemester(semesterId: Long): Flow<List<RecurringHolidayRule>>

    @Query("SELECT * FROM recurring_holiday_rules WHERE semesterId = :semesterId")
    suspend fun getForSemesterOnce(semesterId: Long): List<RecurringHolidayRule>

    @Query("DELETE FROM recurring_holiday_rules WHERE semesterId = :semesterId")
    suspend fun deleteForSemester(semesterId: Long)
}
