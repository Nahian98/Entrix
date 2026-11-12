package com.apexbytestudio.entrix.domain.usecase

import app.cash.turbine.test
import com.apexbytestudio.entrix.core.model.Attendance
import com.apexbytestudio.entrix.core.model.Office
import com.apexbytestudio.entrix.domain.fake.FakeAttendanceRepository
import com.apexbytestudio.entrix.domain.fake.FakeOfficeRepository
import com.apexbytestudio.entrix.domain.fake.FakeTrackingStateRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class ObserveDashboardUseCaseTest {

    private val zone = ZoneId.of("UTC")
    private val clock = Clock.fixed(Instant.parse("2026-06-28T12:00:00Z"), zone)

    private val officeRepo = FakeOfficeRepository()
    private val attendanceRepo = FakeAttendanceRepository()
    private val trackingRepo = FakeTrackingStateRepository(enabled = true)

    private val useCase = ObserveDashboardUseCase(officeRepo, attendanceRepo, trackingRepo, clock)

    @Test
    fun `per-office status keeps only today's visits and reflects an open visit`() = runTest {
        officeRepo.saveOffice(
            Office(name = "HQ", latitude = 52.52, longitude = 13.405, radiusMeters = 150f),
        )
        attendanceRepo.seed(
            Attendance(
                id = 1, officeId = 1,
                enterTime = Instant.parse("2026-06-28T11:00:00Z"),
                exitTime = null,
                createdTime = Instant.parse("2026-06-28T11:00:00Z"),
            ),
            Attendance(
                id = 2, officeId = 1,
                enterTime = Instant.parse("2026-06-27T09:00:00Z"),
                exitTime = Instant.parse("2026-06-27T17:00:00Z"),
                createdTime = Instant.parse("2026-06-27T09:00:00Z"),
            ),
        )

        useCase().test {
            val snapshot = awaitItem()

            assertTrue(snapshot.isOfficeConfigured)
            assertTrue(snapshot.isInsideAnyOffice)
            assertEquals(1, snapshot.offices.size)

            val hq = snapshot.offices.single()
            assertTrue(hq.isInside)
            assertEquals(Instant.parse("2026-06-28T11:00:00Z"), hq.todayFirstEnter)
            // The open visit today has no completed duration; yesterday's visit is excluded.
            assertEquals(Duration.ZERO, hq.todayCompletedDuration)
            assertTrue(snapshot.trackingEnabled)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tracks two offices independently`() = runTest {
        officeRepo.saveOffice(Office(name = "HQ", latitude = 1.0, longitude = 1.0, radiusMeters = 150f))
        officeRepo.saveOffice(Office(name = "Client", latitude = 2.0, longitude = 2.0, radiusMeters = 150f))
        attendanceRepo.seed(
            // Open at office 1, completed 2h at office 2 today.
            Attendance(1, officeId = 1, enterTime = Instant.parse("2026-06-28T11:00:00Z"), exitTime = null, createdTime = Instant.parse("2026-06-28T11:00:00Z")),
            Attendance(2, officeId = 2, enterTime = Instant.parse("2026-06-28T08:00:00Z"), exitTime = Instant.parse("2026-06-28T10:00:00Z"), createdTime = Instant.parse("2026-06-28T08:00:00Z")),
        )

        useCase().test {
            val snapshot = awaitItem()

            assertEquals(2, snapshot.offices.size)
            val hq = snapshot.offices.first { it.office.id == 1L }
            val client = snapshot.offices.first { it.office.id == 2L }
            assertTrue(hq.isInside)
            assertFalse(client.isInside)
            assertEquals(Duration.ofHours(2), client.todayCompletedDuration)
            assertEquals(Duration.ofHours(2), snapshot.todayTotalDuration)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
