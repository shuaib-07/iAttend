package com.iattend.app.core.domain.stats

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendancePercentageTest {
    @Test fun acceptsOnlyFinitePercentagesWithinBounds() {
        listOf(0f, 75f, 80.5f, 100f).forEach { assertTrue(isValidAttendancePercentage(it)) }
        listOf(null, -1f, 100.01f, 101f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
            .forEach { assertFalse(isValidAttendancePercentage(it)) }
    }
}
