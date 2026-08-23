package com.iattend.app.core.data.db

import androidx.room.TypeConverter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class Converters {
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromLocalTime(value: LocalTime?): String? = value?.toString()

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let(LocalTime::parse)

    @TypeConverter
    fun fromDayOfWeek(value: DayOfWeek?): String? = value?.name

    @TypeConverter
    fun toDayOfWeek(value: String?): DayOfWeek? = value?.let(DayOfWeek::valueOf)

    @TypeConverter
    fun fromOccurrenceStatus(value: OccurrenceStatus?): String? = value?.name

    @TypeConverter
    fun toOccurrenceStatus(value: String?): OccurrenceStatus? = value?.let(OccurrenceStatus::valueOf)

    @TypeConverter
    fun fromOccurrenceSource(value: OccurrenceSource?): String? = value?.name

    @TypeConverter
    fun toOccurrenceSource(value: String?): OccurrenceSource? = value?.let(OccurrenceSource::valueOf)

    @TypeConverter
    fun fromTotalMode(value: TotalMode?): String? = value?.name

    @TypeConverter
    fun toTotalMode(value: String?): TotalMode? = value?.let(TotalMode::valueOf)

    @TypeConverter
    fun fromRecurringHolidayMode(value: RecurringHolidayMode?): String? = value?.name

    @TypeConverter
    fun toRecurringHolidayMode(value: String?): RecurringHolidayMode? =
        value?.let(RecurringHolidayMode::valueOf)

    @TypeConverter
    fun fromClassType(value: ClassType?): String? = value?.name

    @TypeConverter
    fun toClassType(value: String?): ClassType? = value?.let(ClassType::valueOf)

    @TypeConverter
    fun fromAssessmentType(value: AssessmentType?): String? = value?.name

    @TypeConverter
    fun toAssessmentType(value: String?): AssessmentType? = value?.let(AssessmentType::valueOf)
}
