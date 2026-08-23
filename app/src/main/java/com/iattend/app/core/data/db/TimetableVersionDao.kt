package com.iattend.app.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableVersionDao {
    @Insert
    suspend fun insert(version: TimetableVersion): Long

    @Insert
    suspend fun insertAll(versions: List<TimetableVersion>)

    @Update
    suspend fun update(version: TimetableVersion)

    @Delete
    suspend fun delete(version: TimetableVersion)

    @Query("SELECT * FROM timetable_versions ORDER BY effectiveFrom")
    fun getAll(): Flow<List<TimetableVersion>>

    @Query("SELECT * FROM timetable_versions ORDER BY effectiveFrom")
    suspend fun getAllOnce(): List<TimetableVersion>

    @Query("SELECT * FROM timetable_versions WHERE id = :id")
    suspend fun getById(id: Long): TimetableVersion?
}
