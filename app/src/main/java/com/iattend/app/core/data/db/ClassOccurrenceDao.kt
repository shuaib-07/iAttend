package com.iattend.app.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ClassOccurrenceDao {
    @Insert
    suspend fun insert(occurrence: ClassOccurrence): Long

    @Insert
    suspend fun insertAll(occurrences: List<ClassOccurrence>)

    @Update
    suspend fun update(occurrence: ClassOccurrence)

    @Update
    suspend fun updateAll(occurrences: List<ClassOccurrence>)

    @Delete
    suspend fun delete(occurrence: ClassOccurrence)

    @Query("SELECT * FROM class_occurrences WHERE date = :date ORDER BY startTime")
    fun getForDate(date: LocalDate): Flow<List<ClassOccurrence>>

    @Query("SELECT * FROM class_occurrences WHERE date = :date ORDER BY startTime")
    suspend fun getForDateOnce(date: LocalDate): List<ClassOccurrence>

    @Query("SELECT * FROM class_occurrences WHERE id = :id")
    suspend fun getByIdOnce(id: Long): ClassOccurrence?

    @Query("SELECT * FROM class_occurrences WHERE date BETWEEN :from AND :to ORDER BY date")
    fun getForDateRange(from: LocalDate, to: LocalDate): Flow<List<ClassOccurrence>>

    @Query("SELECT * FROM class_occurrences WHERE subjectId = :subjectId ORDER BY date")
    fun getForSubject(subjectId: Long): Flow<List<ClassOccurrence>>

    @Query("SELECT * FROM class_occurrences WHERE subjectId = :subjectId ORDER BY date")
    suspend fun getForSubjectOnce(subjectId: Long): List<ClassOccurrence>

    @Query("SELECT * FROM class_occurrences ORDER BY date")
    fun getAll(): Flow<List<ClassOccurrence>>

    @Query("SELECT * FROM class_occurrences ORDER BY date")
    suspend fun getAllOnce(): List<ClassOccurrence>

    @Query("SELECT * FROM class_occurrences WHERE date BETWEEN :from AND :to")
    suspend fun getInRangeOnce(from: LocalDate, to: LocalDate): List<ClassOccurrence>

    // source = 'SCHEDULED' only: EXTRA (ad-hoc) occurrences are user-added and must
    // never be silently wiped by timetable/holiday regeneration.
    @Query(
        "DELETE FROM class_occurrences WHERE status = 'UNMARKED' AND source = 'SCHEDULED' AND date BETWEEN :from AND :to"
    )
    suspend fun deleteUnmarkedScheduledInRange(from: LocalDate, to: LocalDate)

    @Query("UPDATE class_occurrences SET roomNumber = :room WHERE subjectId = :subjectId AND status = 'UNMARKED' AND roomNumberOverridden = 0")
    suspend fun updateInheritedRooms(subjectId: Long, room: String?)
}
