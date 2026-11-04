package com.iattend.app.core.ui.heatmap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class HeatmapDataTest {

    @Test
    fun `generateHeatmapData produces aligned 7-day weeks starting on Monday`() {
        val testDate = LocalDate.of(2026, 11, 4)
        val data = mapOf(
            testDate to 3,
            testDate.minusDays(1) to 1
        )

        val weeks = generateHeatmapData(
            activeDates = data,
            customStartDate = LocalDate.of(2026, 10, 1),
            customEndDate = LocalDate.of(2026, 11, 4)
        )

        assertTrue(weeks.isNotEmpty())
        // Every week should have 7 days
        for (w in weeks) {
            assertEquals(7, w.days.size)
            assertEquals(DayOfWeek.MONDAY, w.days.first().date.dayOfWeek)
            assertEquals(DayOfWeek.SUNDAY, w.days.last().date.dayOfWeek)
        }

        // Check intensity matching
        val targetDay = weeks.flatMap { it.days }.firstOrNull { it.date == testDate }
        assertNotNull(targetDay)
        assertEquals(3, targetDay?.intensity)
    }

    @Test
    fun `generateHeatmapData identifies first day of month labels`() {
        val weeks = generateHeatmapData(
            activeDates = emptyMap(),
            customStartDate = LocalDate.of(2026, 10, 20),
            customEndDate = LocalDate.of(2026, 11, 10)
        )

        // There should be a week containing Nov 1 with label "Nov"
        val novWeek = weeks.firstOrNull { w -> w.days.any { it.date == LocalDate.of(2026, 11, 1) } }
        assertNotNull(novWeek)
        assertEquals("Nov", novWeek?.firstDayOfMonth)
    }
}
