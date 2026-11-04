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

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE subjects ADD COLUMN defaultRoomNumber TEXT")
        db.execSQL("ALTER TABLE timetable_slots ADD COLUMN roomNumberOverridden INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE timetable_slots SET roomNumberOverridden = 1 WHERE roomNumber IS NOT NULL AND TRIM(roomNumber) != ''")
        db.execSQL("ALTER TABLE class_occurrences ADD COLUMN roomNumberOverridden INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE class_occurrences SET roomNumberOverridden = 1 WHERE roomNumber IS NOT NULL AND TRIM(roomNumber) != ''")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE subjects ADD COLUMN gradingSystem TEXT")
        db.execSQL("ALTER TABLE subjects ADD COLUMN totalWeightedMarks REAL")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS deleted_class_exceptions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                subjectId INTEGER NOT NULL,
                date TEXT NOT NULL,
                startTime TEXT,
                FOREIGN KEY(subjectId) REFERENCES subjects(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_deleted_class_exceptions_subjectId ON deleted_class_exceptions(subjectId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_deleted_class_exceptions_date ON deleted_class_exceptions(date)")

        db.execSQL(
            """
            CREATE TABLE assessments_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                subjectId INTEGER NOT NULL,
                type TEXT NOT NULL,
                date TEXT NOT NULL,
                startTime TEXT NOT NULL,
                endTime TEXT NOT NULL,
                totalMarks REAL,
                portions TEXT,
                reminderOffsets TEXT NOT NULL DEFAULT '',
                title TEXT,
                obtainedMarks REAL,
                weight REAL,
                attemptStatus TEXT NOT NULL DEFAULT 'PENDING',
                isAttemptOverridden INTEGER NOT NULL DEFAULT 0,
                linkedOccurrenceId INTEGER,
                needsRescheduling INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(subjectId) REFERENCES subjects(id) ON DELETE CASCADE,
                FOREIGN KEY(linkedOccurrenceId) REFERENCES class_occurrences(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO assessments_new (
                id, subjectId, type, date, startTime, endTime, totalMarks, portions, reminderOffsets, title,
                obtainedMarks, weight, attemptStatus, isAttemptOverridden, linkedOccurrenceId, needsRescheduling
            )
            SELECT
                id, subjectId, type, date, startTime, endTime, CAST(totalMarks AS REAL), portions, reminderOffsets, title,
                NULL, NULL, 'PENDING', 0, NULL, 0
            FROM assessments
            """.trimIndent()
        )
        db.execSQL("DROP TABLE assessments")
        db.execSQL("ALTER TABLE assessments_new RENAME TO assessments")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_assessments_subjectId ON assessments(subjectId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_assessments_date ON assessments(date)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_assessments_linkedOccurrenceId ON assessments(linkedOccurrenceId)")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create semesters table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS semesters (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                startDate TEXT NOT NULL,
                endDate TEXT NOT NULL,
                targetPercentage REAL NOT NULL DEFAULT 75.0,
                isArchived INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // 2. Insert default initial semester for existing data
        val now = System.currentTimeMillis()
        var minDate = "2026-01-01"
        var maxDate = "2026-12-31"

        try {
            val cursor = db.query("SELECT MIN(date), MAX(date) FROM class_occurrences")
            if (cursor.moveToFirst()) {
                val minVal = cursor.getString(0)
                val maxVal = cursor.getString(1)
                if (!minVal.isNullOrBlank()) minDate = minVal
                if (!maxVal.isNullOrBlank()) maxDate = maxVal
            }
            cursor.close()
        } catch (_: Exception) {}

        db.execSQL(
            """
            INSERT OR IGNORE INTO semesters (id, name, startDate, endDate, targetPercentage, isArchived, createdAt)
            VALUES (1, 'Semester 1', '$minDate', '$maxDate', 75.0, 0, $now)
            """.trimIndent()
        )

        // 3. Add semesterId to subjects
        db.execSQL("ALTER TABLE subjects ADD COLUMN semesterId INTEGER NOT NULL DEFAULT 1")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_subjects_semesterId ON subjects(semesterId)")

        // 4. Add semesterId to timetable_versions
        db.execSQL("ALTER TABLE timetable_versions ADD COLUMN semesterId INTEGER NOT NULL DEFAULT 1")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_timetable_versions_semesterId ON timetable_versions(semesterId)")

        // 5. Add semesterId to holidays
        db.execSQL("ALTER TABLE holidays ADD COLUMN semesterId INTEGER NOT NULL DEFAULT 1")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_holidays_semesterId ON holidays(semesterId)")

        // 6. Recreate recurring_holiday_rules with primary key (semesterId, dayOfWeek)
        db.execSQL(
            """
            CREATE TABLE recurring_holiday_rules_new (
                dayOfWeek TEXT NOT NULL,
                mode TEXT NOT NULL,
                weeksOfMonth INTEGER NOT NULL,
                semesterId INTEGER NOT NULL DEFAULT 1,
                PRIMARY KEY(semesterId, dayOfWeek)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO recurring_holiday_rules_new (dayOfWeek, mode, weeksOfMonth, semesterId)
            SELECT dayOfWeek, mode, weeksOfMonth, 1 FROM recurring_holiday_rules
            """.trimIndent()
        )
        db.execSQL("DROP TABLE recurring_holiday_rules")
        db.execSQL("ALTER TABLE recurring_holiday_rules_new RENAME TO recurring_holiday_rules")
    }
}

@Database(
    entities = [
        Semester::class,
        Subject::class,
        TimetableVersion::class,
        TimetableSlot::class,
        ClassOccurrence::class,
        Holiday::class,
        RecurringHolidayRule::class,
        Assessment::class,
        DeletedClassException::class
    ],
    version = 10,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun semesterDao(): SemesterDao
    abstract fun subjectDao(): SubjectDao
    abstract fun timetableVersionDao(): TimetableVersionDao
    abstract fun timetableSlotDao(): TimetableSlotDao
    abstract fun classOccurrenceDao(): ClassOccurrenceDao
    abstract fun holidayDao(): HolidayDao
    abstract fun recurringHolidayRuleDao(): RecurringHolidayRuleDao
    abstract fun assessmentDao(): AssessmentDao
    abstract fun deletedClassExceptionDao(): DeletedClassExceptionDao
}
