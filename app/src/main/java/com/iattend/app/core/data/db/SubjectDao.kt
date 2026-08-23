package com.iattend.app.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(subject: Subject): Long

    @Insert
    suspend fun insertAll(subjects: List<Subject>)

    @Update
    suspend fun update(subject: Subject)

    @Delete
    suspend fun delete(subject: Subject)

    @Query("SELECT * FROM subjects ORDER BY name")
    fun getAll(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects ORDER BY name")
    suspend fun getAllOnce(): List<Subject>

    @Query("SELECT * FROM subjects WHERE id = :id")
    fun getById(id: Long): Flow<Subject?>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getByIdOnce(id: Long): Subject?
}
