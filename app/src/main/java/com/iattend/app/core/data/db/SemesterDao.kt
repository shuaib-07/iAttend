package com.iattend.app.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SemesterDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(semester: Semester): Long

    @Insert
    suspend fun insertAll(semesters: List<Semester>)

    @Update
    suspend fun update(semester: Semester)

    @Delete
    suspend fun delete(semester: Semester)

    @Query("SELECT * FROM semesters ORDER BY startDate DESC")
    fun getAll(): Flow<List<Semester>>

    @Query("SELECT * FROM semesters ORDER BY startDate DESC")
    suspend fun getAllOnce(): List<Semester>

    @Query("SELECT * FROM semesters WHERE id = :id")
    fun getById(id: Long): Flow<Semester?>

    @Query("SELECT * FROM semesters WHERE id = :id")
    suspend fun getByIdOnce(id: Long): Semester?

    @Query("SELECT COUNT(*) FROM semesters")
    suspend fun getCount(): Int

    @Query("SELECT * FROM semesters WHERE isArchived = 0 ORDER BY startDate DESC LIMIT 1")
    suspend fun getLatestActiveOnce(): Semester?
}
