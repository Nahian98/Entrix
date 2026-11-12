package com.apexbytestudio.entrix.domain.usecase

import com.apexbytestudio.entrix.core.model.Attendance
import com.apexbytestudio.entrix.core.model.Office
import com.apexbytestudio.entrix.domain.fake.FakeAttendanceRepository
import com.apexbytestudio.entrix.domain.fake.FakeOfficeRepository
import com.apexbytestudio.entrix.domain.fake.FakeTimesheetExporter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ExportTimesheetUseCaseTest {

    private val zone = ZoneId.of("UTC")
    private val attendanceRepo = FakeAttendanceRepository()
    private val officeRepo = FakeOfficeRepository()
    private val exporter = FakeTimesheetExporter()
    private val useCase = ExportTimesheetUseCase(officeRepo, attendanceRepo, exporter)

    private fun visit(id: Long, officeId: Long, enter: String, exit: String?) = Attendance(
        id = id,
        officeId = officeId,
        enterTime = Instant.parse(enter),
        exitTime = exit?.let(Instant::parse),
        createdTime = Instant.parse(enter),
    )

    @Test
    fun `groups completed in-range visits by office with totals`() = runTest {
        officeRepo.seed(
            Office(id = 1, name = "HQ", latitude = 1.0, longitude = 1.0, radiusMeters = 150f),
            Office(id = 2, name = "Client", latitude = 2.0, longitude = 2.0, radiusMeters = 150f),
        )
        attendanceRepo.seed(
            visit(1, 1, "2026-06-22T09:00:00Z", "2026-06-22T11:00:00Z"), // HQ 2h
            visit(2, 2, "2026-06-23T08:00:00Z", "2026-06-23T12:00:00Z"), // Client 4h
            visit(3, 1, "2026-06-24T13:00:00Z", null),                   // open → excluded
            visit(4, 1, "2026-07-01T09:00:00Z", "2026-07-01T10:00:00Z"), // out of range → excluded
        )

        val result = useCase(
            startDate = LocalDate.of(2026, 6, 22),
            endDate = LocalDate.of(2026, 6, 28),
            zone = zone,
            periodLabel = "22–28 Jun",
        )

        assertTrue(result.isSuccess)
        val timesheet = exporter.lastTimesheet!!
        assertEquals("22–28 Jun", timesheet.periodLabel)
        assertEquals(2, timesheet.offices.size)
        assertEquals(Duration.ofHours(6), timesheet.grandTotal)

        val hq = timesheet.offices.first { it.officeName == "HQ" }
        assertEquals(Duration.ofHours(2), hq.total)
        assertEquals(1, hq.rows.size) // open + out-of-range excluded
    }

    @Test
    fun `empty timesheet when no completed visits in range`() = runTest {
        officeRepo.seed(Office(id = 1, name = "HQ", latitude = 1.0, longitude = 1.0, radiusMeters = 150f))

        useCase(
            startDate = LocalDate.of(2026, 6, 22),
            endDate = LocalDate.of(2026, 6, 28),
            zone = zone,
            periodLabel = "22–28 Jun",
        )

        assertTrue(exporter.lastTimesheet!!.isEmpty)
    }
}
