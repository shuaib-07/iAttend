package com.iattend.app.widget

import com.iattend.app.core.data.db.ClassOccurrenceDao
import com.iattend.app.core.data.db.SubjectDao
import com.iattend.app.core.datastore.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Glance widgets aren't Hilt-injectable directly (no Android entry point Hilt recognizes) -
 * this is the standard workaround to reach the same singletons from [UpcomingClassesWidget]. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun classOccurrenceDao(): ClassOccurrenceDao
    fun subjectDao(): SubjectDao
    fun settingsRepository(): SettingsRepository
}
