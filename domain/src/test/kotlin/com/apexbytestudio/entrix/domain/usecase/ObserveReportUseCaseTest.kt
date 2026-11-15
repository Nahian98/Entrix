package com.apexbytestudio.entrix.domain.usecase

import app.cash.turbine.test
import com.apexbytestudio.entrix.core.model.Attendance
import com.apexbytestudio.entrix.core.model.Office
import com.apexbytestudio.entrix.domain.fake.FakeAttendanceRepository
import com.apexbytestudio.entrix.domain.fake.FakeOfficeRepository
import com.apexbytestudio.entrix.domain.model.ReportPeriod
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ObserveReportUseCaseTest {

    private val zone = ZoneId.of("UTC")
    // A Wednesday — its Monday is 2026-06-22, so the week runs 22–28 June 2026.
    private val clock = Clock.fixed(Instant.parse("2026-06-24T12:00:00Z"), zone)

    private val attendanceRepo = FakeAttendanceRepository()
    private val officeRepo = FakeOfficeRepository()
    private val useCase = ObserveReportUseCase(attendanceRepo, officeRepo, clock)

    private fun visit(id: Long, officeId: Long, enter: String, exit: String?) = Attendance(
        id = id,
        officeId = officeId,
        enterTime = Instant.parse(enter),
        exitTime = exit?.let(Instant::parse),
        createdTime = Instant.parse(enter),
    )

    @Test
    fun `weekly report includes an open visit's time so far and computes the trend`() = runTest {
        officeRepo.seed(Office(id = 1, name = "HQ", latitude = 1.0, longitude = 1.0, radiusMeters = 150f))
        attendanceRepo.seed(
            visit(1, 1, "2026-06-22T09:00:00Z", "2026-06-22T11:00:00Z"), // Mon, 2h (this week)
            visit(2, 1, "2026-06-24T08:00:00Z", "2026-06-24T12:00:00Z"), // Wed, 4h (this week)
            visit(3, 1, "2026-06-24T10:00:00Z", null),                   // open since 10:00 → 2h so far
            visit(4, 1, "2026-06-15T09:00:00Z", "2026-06-15T10:00:00Z"), // prev week, 1h
        )

        useCase(ReportPeriod.Week, offset = 0).test {
            val report = awaitItem()

            assertTrue(report.hasData)
            assertEquals(Duration.ofHours(8), report.totalDuration)
            assertEquals(3, report.visitCount)
            assertEquals(2, report.daysPresent)
            assertEquals(Duration.ofHours(4), report.averagePerActiveDay)
            assertEquals(Duration.ofHours(6), report.bars[2].duration) // Wed: 4h done + 2h open
            assertEquals(7, report.bars.size) // one per weekday
            assertEquals(Duration.ofHours(1), report.previousTotal)
            assertEquals(700, report.trendPercent) // (8h - 1h) / 1h
            assertEquals(1, report.offices.size)
            assertEquals("HQ", report.offices.single().officeName)
            assertEquals(Duration.ofHours(8), report.offices.single().duration)
            assertTrue(report.hasVisitInProgress) // the open Wed visit

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty period reports no data and a null trend`() = runTest {
        officeRepo.seed(Office(id = 1, name = "HQ", latitude = 1.0, longitude = 1.0, radiusMeters = 150f))

        useCase(ReportPeriod.Week, offset = 0).test {
            val report = awaitItem()

            assertFalse(report.hasData)
            assertEquals(Duration.ZERO, report.totalDuration)
            assertNull(report.trendPercent)
            assertEquals(7, report.bars.size)
            assertFalse(report.hasVisitInProgress)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `monthly report buckets by week`() = runTest {
        officeRepo.seed(Office(id = 1, name = "HQ", latitude = 1.0, longitude = 1.0, radiusMeters = 150f))
        attendanceRepo.seed(
            visit(1, 1, "2026-06-03T09:00:00Z", "2026-06-03T12:00:00Z"), // 3h in June
        )

        useCase(ReportPeriod.Month, offset = 0).test {
            val report = awaitItem()

            assertEquals(Duration.ofHours(3), report.totalDuration)
            // June 2026 starts on a Monday: five calendar weeks, the last trimmed to the 30th.
            assertEquals(listOf("1–7", "8–14", "15–21", "22–28", "29–30"), report.bars.map { it.label })
            assertEquals(Duration.ofHours(3), report.bars.first().duration)

            cancelAndIgnoreRemainingEvents()
        }
    }
    @Test
    fun `month bars are calendar weeks trimmed to the month`() = runTest {
        // November 2026 starts on a Sunday and ends on a Monday.
        useCase(ReportPeriod.Month, offset = 5).test {
            val report = awaitItem()

            assertEquals("November 2026", report.rangeLabel)
            assertEquals(listOf("1", "2–8", "9–15", "16–22", "23–29", "30"), report.bars.map { it.label })
            assertEquals(LocalDate.of(2026, 11, 2), report.bars[1].start)
            assertEquals(LocalDate.of(2026, 11, 8), report.bars[1].end)
            assertEquals(LocalDate.of(2026, 11, 30), report.bars.last().start)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `open visit time ticks forward each minute`() = runTest {
        val ticking = ObserveReportUseCase(attendanceRepo, officeRepo, SchedulerClock(clock.instant(), testScheduler))
        officeRepo.seed(Office(id = 1, name = "HQ", latitude = 1.0, longitude = 1.0, radiusMeters = 150f))
        attendanceRepo.seed(visit(1, 1, "2026-06-24T11:30:00Z", null))

        ticking(ReportPeriod.Week, offset = 0).test {
            assertEquals(Duration.ofMinutes(30), awaitItem().totalDuration)
            assertEquals(Duration.ofMinutes(31), awaitItem().totalDuration)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

/** A clock that advances with the coroutine test scheduler's virtual time. */
private class SchedulerClock(
    private val base: Instant,
    private val scheduler: TestCoroutineScheduler,
    private val zone: ZoneId = ZoneId.of("UTC"),
) : Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = SchedulerClock(base, scheduler, zone)
    override fun instant(): Instant = base.plusMillis(scheduler.currentTime)
}
