package com.apexbytestudio.entrix.domain.usecase

import com.apexbytestudio.entrix.core.model.Attendance
import com.apexbytestudio.entrix.core.model.Office
import com.apexbytestudio.entrix.domain.fake.FakeAttendanceExporter
import com.apexbytestudio.entrix.domain.fake.FakeAttendanceRepository
import com.apexbytestudio.entrix.domain.fake.FakeOfficeRepository
import com.apexbytestudio.entrix.domain.model.ExportFormat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ExportAttendanceUseCaseTest {

    private val officeRepo = FakeOfficeRepository()
    private val attendanceRepo = FakeAttendanceRepository()
    private val exporter = FakeAttendanceExporter()
    private val useCase = ExportAttendanceUseCase(officeRepo, attendanceRepo, exporter)

    @Test
    fun `exports all records with office names and duration`() = runTest {
        officeRepo.seed(Office(id = 1, name = "HQ", latitude = 1.0, longitude = 1.0, radiusMeters = 150f))
        attendanceRepo.seed(
            Attendance(1, 1, Instant.parse("2026-06-22T09:00:00Z"), Instant.parse("2026-06-22T11:00:00Z"), Instant.parse("2026-06-22T09:00:00Z")),
            Attendance(2, 1, Instant.parse("2026-06-23T09:00:00Z"), null, Instant.parse("2026-06-23T09:00:00Z")), // open visit
        )

        val result = useCase(ExportFormat.Csv)

        assertTrue(result.isSuccess)
        assertEquals(ExportFormat.Csv, exporter.lastFormat)

        val rows = exporter.lastRows!!
        assertEquals(2, rows.size)

        val closed = rows.first { it.exitTime != null }
        assertEquals("HQ", closed.officeName)
        assertEquals(120L, closed.durationMinutes) // 2h

        val open = rows.first { it.exitTime == null }
        assertNull(open.durationMinutes)
    }

    @Test
    fun `orphaned record exports with a null office name`() = runTest {
        // No office seeded for id 9 — name can't be resolved.
        attendanceRepo.seed(
            Attendance(1, 9, Instant.parse("2026-06-22T09:00:00Z"), Instant.parse("2026-06-22T10:00:00Z"), Instant.parse("2026-06-22T09:00:00Z")),
        )

        useCase(ExportFormat.Json)

        val row = exporter.lastRows!!.single()
        assertNull(row.officeName)
        assertEquals(ExportFormat.Json, exporter.lastFormat)
    }
}
