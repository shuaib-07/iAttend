package com.iattend.app.core.data.export

import com.iattend.app.core.data.db.Assessment
import com.iattend.app.core.data.db.AssessmentAttemptStatus
import com.iattend.app.core.data.db.AssessmentType
import com.iattend.app.core.data.db.DeletedClassException
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.data.db.TotalMode
import com.iattend.app.core.data.db.WeightageSystem
import com.iattend.app.core.datastore.AppSettings
import com.iattend.app.core.datastore.Profile
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ExportImportCompatibilityTest {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }

    @Test
    fun `export payload v3 roundtrip preserves new assessment, subject and deletion fields`() {
        val subject = Subject(
            id = 1,
            code = "CS101",
            name = "Computer Science",
            colorArgb = 0xFF00FF,
            totalMode = TotalMode.OPEN_ENDED,
            gradingSystem = WeightageSystem.WEIGHTED_MARKS,
            totalWeightedMarks = 100.0
        )
        val assessment = Assessment(
            id = 10,
            subjectId = 1,
            type = AssessmentType.TEST,
            date = LocalDate.of(2026, 10, 15),
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            totalMarks = 50.0,
            obtainedMarks = 45.5,
            weight = 20.0,
            attemptStatus = AssessmentAttemptStatus.ATTEMPTED,
            isAttemptOverridden = true,
            linkedOccurrenceId = 101L,
            needsRescheduling = false,
            title = "Midterm"
        )
        val deletion = DeletedClassException(
            id = 5,
            subjectId = 1,
            date = LocalDate.of(2026, 10, 12),
            startTime = LocalTime.of(10, 0)
        )

        val payload = ExportPayload(
            exportVersion = 3,
            subjects = listOf(subject),
            timetableVersions = emptyList(),
            timetableSlots = emptyList(),
            classOccurrences = emptyList(),
            holidays = emptyList(),
            recurringHolidayRules = emptyList(),
            settings = AppSettings(),
            profile = Profile(),
            assessments = listOf(assessment),
            deletedClassExceptions = listOf(deletion)
        )

        val serialized = json.encodeToString(payload)
        val deserialized = json.decodeFromString<ExportPayload>(serialized)

        assertEquals(3, deserialized.exportVersion)
        assertEquals(1, deserialized.subjects.size)
        assertEquals(WeightageSystem.WEIGHTED_MARKS, deserialized.subjects[0].gradingSystem)
        assertEquals(100.0, deserialized.subjects[0].totalWeightedMarks)

        assertEquals(1, deserialized.assessments.size)
        val a = deserialized.assessments[0]
        assertEquals(50.0, a.totalMarks)
        assertEquals(45.5, a.obtainedMarks)
        assertEquals(20.0, a.weight)
        assertEquals(AssessmentAttemptStatus.ATTEMPTED, a.attemptStatus)
        assertTrue(a.isAttemptOverridden)
        assertEquals(101L, a.linkedOccurrenceId)
        assertFalse(a.needsRescheduling)

        assertEquals(1, deserialized.deletedClassExceptions.size)
        val d = deserialized.deletedClassExceptions[0]
        assertEquals(1L, d.subjectId)
        assertEquals(LocalDate.of(2026, 10, 12), d.date)
        assertEquals(LocalTime.of(10, 0), d.startTime)
    }

    @Test
    fun `legacy v2 json without new grading and deletion fields imports with safe defaults`() {
        val legacyJson = """
        {
          "exportVersion": 2,
          "subjects": [
            {
              "id": 1,
              "code": "CS101",
              "name": "Intro to CS",
              "colorArgb": 12345,
              "totalMode": "OPEN_ENDED"
            }
          ],
          "timetableVersions": [],
          "timetableSlots": [],
          "classOccurrences": [],
          "holidays": [],
          "recurringHolidayRules": [],
          "settings": {},
          "profile": {},
          "assessments": [
            {
              "id": 1,
              "subjectId": 1,
              "type": "TEST",
              "date": "2026-10-10",
              "startTime": "09:00",
              "endTime": "10:00",
              "totalMarks": 40.0,
              "reminderOffsets": "60"
            }
          ]
        }
        """.trimIndent()

        val parsed = json.decodeFromString<ExportPayload>(legacyJson)
        assertEquals(2, parsed.exportVersion)
        assertEquals(1, parsed.subjects.size)
        assertNull(parsed.subjects[0].gradingSystem)
        assertNull(parsed.subjects[0].totalWeightedMarks)

        assertEquals(1, parsed.assessments.size)
        val a = parsed.assessments[0]
        assertEquals(40.0, a.totalMarks)
        assertNull(a.obtainedMarks)
        assertNull(a.weight)
        assertEquals(AssessmentAttemptStatus.PENDING, a.attemptStatus)
        assertFalse(a.isAttemptOverridden)
        assertNull(a.linkedOccurrenceId)
        assertFalse(a.needsRescheduling)

        assertTrue(parsed.deletedClassExceptions.isEmpty())
    }
}
