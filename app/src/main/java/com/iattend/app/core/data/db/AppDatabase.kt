package com.iattend.app.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE timetable_slots ADD COLUMN roomNumber TEXT")
        db.execSQL("ALTER TABLE class_occurrences ADD COLUMN roomNumber TEXT")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE subjects ADD COLUMN trackingEndDateOverride TEXT")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE timetable_slots ADD COLUMN classType TEXT NOT NULL DEFAULT 'LECTURE'")
        db.execSQL("ALTER TABLE class_occurrences ADD COLUMN classType TEXT NOT NULL DEFAULT 'LECTURE'")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE subjects ADD COLUMN teacherName TEXT")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE class_occurrences ADD COLUMN cancelReason TEXT")
        db.execSQL("ALTER TABLE subjects ADD COLUMN reminderOffsetsOverride TEXT")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS assessments (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                subjectId INTEGER NOT NULL,
                type TEXT NOT NULL,
                date TEXT NOT NULL,
                time TEXT,
                totalMarks INTEGER,
                portions TEXT,
                reminderOffsets TEXT NOT NULL DEFAULT '',
                title TEXT,
                FOREIGN KEY(subjectId) REFERENCES subjects(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_assessments_subjectId ON assessments(subjectId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_assessments_date ON assessments(date)")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Test/exam time becomes a required start-end range (was an optional single time) so
        // reminders - which need a start time to count back from - always have one to schedule.
        db.execSQL(
            """
            CREATE TABLE assessments_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                subjectId INTEGER NOT NULL,
                type TEXT NOT NULL,
                date TEXT NOT NULL,
                startTime TEXT NOT NULL,
                endTime TEXT NOT NULL,
                totalMarks INTEGER,
                portions TEXT,
                reminderOffsets TEXT NOT NULL DEFAULT '',
                title TEXT,
                FOREIGN KEY(subjectId) REFERENCES subjects(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO assessments_new (id, subjectId, type, date, startTime, endTime, totalMarks, portions, reminderOffsets, title)
            SELECT id, subjectId, type, date, COALESCE(time, '09:00'), COALESCE(time, '10:00'), totalMarks, portions, reminderOffsets, title
            FROM assessments
            """.trimIndent()
        )
        db.execSQL("DROP TABLE assessments")
        db.execSQL("ALTER TABLE assessments_new RENAME TO assessments")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_assessments_subjectId ON assessments(subjectId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_assessments_date ON assessments(date)")
    }
}

@Database(
    entities = [
        Subject::class,
        TimetableVersion::class,
        TimetableSlot::class,
        ClassOccurrence::class,
        Holiday::class,
        RecurringHolidayRule::class,
        Assessment::class
    ],
    version = 7,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun timetableVersionDao(): TimetableVersionDao
    abstract fun timetableSlotDao(): TimetableSlotDao
    abstract fun classOccurrenceDao(): ClassOccurrenceDao
    abstract fun holidayDao(): HolidayDao
    abstract fun recurringHolidayRuleDao(): RecurringHolidayRuleDao
    abstract fun assessmentDao(): AssessmentDao
}
