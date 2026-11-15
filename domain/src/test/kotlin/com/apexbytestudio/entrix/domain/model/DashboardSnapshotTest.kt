package com.apexbytestudio.entrix.domain.model

import com.apexbytestudio.entrix.core.model.Office
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class DashboardSnapshotTest {

    private val startOfToday = Instant.parse("2026-06-28T00:00:00Z")
    private val now = Instant.parse("2026-06-28T12:00:00Z")

    private fun status(
        id: Long,
        completed: Duration = Duration.ZERO,
        currentVisitStart: Instant? = null,
    ) = OfficeStatus(
        office = Office(id = id, name = "O$id", latitude = 0.0, longitude = 0.0, radiusMeters = 100f),
        isInside = currentVisitStart != null,
        todayFirstEnter = null,
        todayLastExit = null,
        todayCompletedDuration = completed,
        currentVisitStart = currentVisitStart,
    )

    @Test
    fun `live total adds the open visit's elapsed time to completed time`() {
        val snapshot = DashboardSnapshot(
            offices = listOf(
                status(1, currentVisitStart = Instant.parse("2026-06-28T10:30:00Z")),
                status(2, completed = Duration.ofHours(2)),
            ),
            trackingEnabled = true,
        )

        assertEquals(Duration.ofMinutes(210), snapshot.todayTotalAt(now, startOfToday))
    }

    @Test
    fun `open visit from yesterday only counts from the start of today`() {
        val snapshot = DashboardSnapshot(
            offices = listOf(status(1, currentVisitStart = Instant.parse("2026-06-27T22:00:00Z"))),
            trackingEnabled = true,
        )

        assertEquals(Duration.ofHours(12), snapshot.todayTotalAt(now, startOfToday))
    }

    @Test
    fun `open visit starting after now contributes nothing`() {
        val snapshot = DashboardSnapshot(
            offices = listOf(status(1, completed = Duration.ofHours(1), currentVisitStart = now.plusSeconds(60))),
            trackingEnabled = true,
        )

        assertEquals(Duration.ofHours(1), snapshot.todayTotalAt(now, startOfToday))
    }
}
