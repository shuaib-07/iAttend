package com.iattend.app.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HolidayDao {
    @Insert
    suspend fun insert(holiday: Holiday): Long

    @Insert
    suspend fun insertAll(holidays: List<Holiday>)

    @Update
    suspend fun update(holiday: Holiday)

    @Delete
    suspend fun delete(holiday: Holiday)

    @Query("SELECT * FROM holidays ORDER BY startDate")
    fun getAll(): Flow<List<Holiday>>

    @Query("SELECT * FROM holidays ORDER BY startDate")
    suspend fun getAllOnce(): List<Holiday>
}
