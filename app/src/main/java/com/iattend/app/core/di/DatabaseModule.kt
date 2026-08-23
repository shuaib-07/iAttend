package com.iattend.app.core.di

import android.content.Context
import androidx.room.Room
import com.iattend.app.core.data.db.AppDatabase
import com.iattend.app.core.data.db.AssessmentDao
import com.iattend.app.core.data.db.ClassOccurrenceDao
import com.iattend.app.core.data.db.HolidayDao
import com.iattend.app.core.data.db.MIGRATION_1_2
import com.iattend.app.core.data.db.MIGRATION_2_3
import com.iattend.app.core.data.db.MIGRATION_3_4
import com.iattend.app.core.data.db.MIGRATION_4_5
import com.iattend.app.core.data.db.MIGRATION_5_6
import com.iattend.app.core.data.db.MIGRATION_6_7
import com.iattend.app.core.data.db.RecurringHolidayRuleDao
import com.iattend.app.core.data.db.SubjectDao
import com.iattend.app.core.data.db.TimetableSlotDao
import com.iattend.app.core.data.db.TimetableVersionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "iattend.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
            .build()

    @Provides
    fun provideSubjectDao(db: AppDatabase): SubjectDao = db.subjectDao()

    @Provides
    fun provideTimetableVersionDao(db: AppDatabase): TimetableVersionDao = db.timetableVersionDao()

    @Provides
    fun provideTimetableSlotDao(db: AppDatabase): TimetableSlotDao = db.timetableSlotDao()

    @Provides
    fun provideClassOccurrenceDao(db: AppDatabase): ClassOccurrenceDao = db.classOccurrenceDao()

    @Provides
    fun provideHolidayDao(db: AppDatabase): HolidayDao = db.holidayDao()

    @Provides
    fun provideRecurringHolidayRuleDao(db: AppDatabase): RecurringHolidayRuleDao =
        db.recurringHolidayRuleDao()

    @Provides
    fun provideAssessmentDao(db: AppDatabase): AssessmentDao = db.assessmentDao()
}
