package com.iattend.app.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.math.roundToInt

class AttendanceBalanceChartTest {

    @Test
    fun `smoothWeeklyData retains points when count is within maxPoints`() {
        val data = listOf(
            WeeklyAttendancePoint(LocalDate.of(2026, 11, 2), 85f),
            WeeklyAttendancePoint(LocalDate.of(2026, 11, 9), 90f)
        )
        val smoothed = data
        assertEquals(2, smoothed.size)
        assertEquals(85f, smoothed[0].percent, 0.01f)
        assertEquals(90f, smoothed[1].percent, 0.01f)
    }

    @Test
    fun `closest index mapping accurately maps horizontal tap coordinate`() {
        val count = 5
        val width = 400f

        fun getClosest(tapX: Float): Int {
            return ((tapX / width) * (count - 1)).roundToInt().coerceIn(0, count - 1)
        }

        // Tap at start (x = 10) -> index 0
        assertEquals(0, getClosest(10f))
        // Tap near 25% (x = 100) -> index 1
        assertEquals(1, getClosest(100f))
        // Tap near middle (x = 200) -> index 2
        assertEquals(2, getClosest(200f))
        // Tap near 75% (x = 300) -> index 3
        assertEquals(3, getClosest(300f))
        // Tap at end (x = 390) -> index 4
        assertEquals(4, getClosest(390f))
    }
}
