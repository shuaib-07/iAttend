package com.iattend.app.core.notifications

/** Reminder timing, shared by class and assessment reminders. Persisted as a comma-separated
 * minutes-before-event string (e.g. "10,60,1440") - see Subject.reminderOffsetsOverride,
 * Assessment.reminderOffsets, Settings.classReminderDefaultOffsets/examReminderDefaultOffsets. */
object ReminderOffset {
    /** Common preset chips shown in the picker, in minutes: 10m, 30m, 1h..12h, 1d..6d. */
    val PRESET_MINUTES: List<Int> = listOf(10, 30) + (1..12).map { it * 60 } + (1..6).map { it * 1440 }

    fun parse(csv: String): List<Int> =
        csv.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it > 0 }.distinct().sorted()

    fun format(offsets: List<Int>): String = offsets.distinct().sorted().joinToString(",")

    /** Human label for a minutes-before value, e.g. 10 -> "10m", 60 -> "1h", 1440 -> "1d". */
    fun label(minutes: Int): String = when {
        minutes % 1440 == 0 -> "${minutes / 1440}d"
        minutes % 60 == 0 -> "${minutes / 60}h"
        else -> "${minutes}m"
    }
}
