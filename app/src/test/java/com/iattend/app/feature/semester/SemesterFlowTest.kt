package com.iattend.app.feature.semester

import com.iattend.app.core.data.db.Semester
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SemesterFlowTest {

    @Test
    fun `semester defaults and properties are valid`() {
        val today = LocalDate.of(2026, 11, 1)
        val semester = Semester(
            id = 1,
            name = "Semester 1",
            startDate = today,
            endDate = today.plusMonths(4),
            targetPercentage = 75f,
            isArchived = false
        )

        assertEquals("Semester 1", semester.name)
        assertEquals(75f, semester.targetPercentage, 0.01f)
        assertFalse(semester.isArchived)
        assertEquals(today, semester.startDate)
        assertEquals(today.plusMonths(4), semester.endDate)
    }

    @Test
    fun `archived flag and past end date identification`() {
        val today = LocalDate.of(2026, 11, 1)
        val activeSem = Semester(
            id = 1,
            name = "Fall 2026",
            startDate = today.minusMonths(1),
            endDate = today.plusMonths(3),
            isArchived = false
        )
        assertFalse(activeSem.isArchived)
        assertFalse(activeSem.endDate.isBefore(today))

        val pastSem = Semester(
            id = 2,
            name = "Spring 2026",
            startDate = today.minusMonths(8),
            endDate = today.minusMonths(4),
            isArchived = false
        )
        assertTrue(pastSem.endDate.isBefore(today))

        val archivedSem = activeSem.copy(isArchived = true)
        assertTrue(archivedSem.isArchived)
    }
}
