package com.iattend.app.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

@Dao
interface DeletedClassExceptionDao {
    @Query("SELECT * FROM deleted_class_exceptions")
    fun getAll(): Flow<List<DeletedClassException>>

    @Query("SELECT * FROM deleted_class_exceptions")
    suspend fun getAllOnce(): List<DeletedClassException>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exception: DeletedClassException): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exceptions: List<DeletedClassException>)

    @Delete
    suspend fun delete(exception: DeletedClassException)

    @Query("DELETE FROM deleted_class_exceptions WHERE subjectId = :subjectId AND date = :date AND (startTime = :startTime OR (:startTime IS NULL AND startTime IS NULL))")
    suspend fun deleteByKey(subjectId: Long, date: LocalDate, startTime: LocalTime?)

    @Query("SELECT EXISTS(SELECT 1 FROM deleted_class_exceptions WHERE subjectId = :subjectId AND date = :date AND (startTime = :startTime OR (:startTime IS NULL AND startTime IS NULL)))")
    suspend fun isDeleted(subjectId: Long, date: LocalDate, startTime: LocalTime?): Boolean

    @Query("DELETE FROM deleted_class_exceptions")
    suspend fun deleteAll()
}
