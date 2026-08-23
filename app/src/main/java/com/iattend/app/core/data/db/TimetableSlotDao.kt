package com.iattend.app.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableSlotDao {
    @Insert
    suspend fun insert(slot: TimetableSlot): Long

    @Insert
    suspend fun insertAll(slots: List<TimetableSlot>)

    @Update
    suspend fun update(slot: TimetableSlot)

    @Delete
    suspend fun delete(slot: TimetableSlot)

    @Query("SELECT * FROM timetable_slots")
    suspend fun getAllOnce(): List<TimetableSlot>

    @Query("SELECT * FROM timetable_slots WHERE timetableVersionId = :versionId")
    fun getForVersion(versionId: Long): Flow<List<TimetableSlot>>

    @Query("SELECT * FROM timetable_slots WHERE timetableVersionId = :versionId")
    suspend fun getForVersionOnce(versionId: Long): List<TimetableSlot>

    @Query("DELETE FROM timetable_slots WHERE timetableVersionId = :versionId")
    suspend fun deleteAllForVersion(versionId: Long)

    @Query("SELECT DISTINCT roomNumber FROM timetable_slots WHERE roomNumber IS NOT NULL AND roomNumber != '' ORDER BY roomNumber")
    fun getDistinctRoomNumbers(): Flow<List<String>>
}
