package com.iattend.app.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface AssessmentDao {
    @Insert
    suspend fun insert(assessment: Assessment): Long

    @Insert
    suspend fun insertAll(assessments: List<Assessment>)

    @Update
    suspend fun update(assessment: Assessment)

    @Delete
    suspend fun delete(assessment: Assessment)

    @Query("SELECT * FROM assessments WHERE subjectId = :subjectId ORDER BY date, startTime")
    fun getForSubject(subjectId: Long): Flow<List<Assessment>>

    @Query("SELECT * FROM assessments WHERE date BETWEEN :from AND :to ORDER BY date, startTime")
    fun getForDateRange(from: LocalDate, to: LocalDate): Flow<List<Assessment>>

    @Query("SELECT * FROM assessments WHERE id = :id")
    suspend fun getByIdOnce(id: Long): Assessment?

    @Query("SELECT * FROM assessments ORDER BY date, startTime")
    fun getAll(): Flow<List<Assessment>>

    @Query("SELECT * FROM assessments ORDER BY date, startTime")
    suspend fun getAllOnce(): List<Assessment>
}
