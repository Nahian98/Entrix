package com.apexbytestudio.entrix.feature.attendance

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.apexbytestudio.entrix.core.model.Attendance
import com.apexbytestudio.entrix.core.testing.MainDispatcherRule
import com.apexbytestudio.entrix.domain.model.HistoryVisit
import com.apexbytestudio.entrix.domain.usecase.DeleteAttendanceHistoryUseCase
import com.apexbytestudio.entrix.domain.usecase.DeleteVisitUseCase
import com.apexbytestudio.entrix.domain.usecase.ObserveAttendanceHistoryUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class AttendanceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeHistory = mockk<ObserveAttendanceHistoryUseCase>()
    private val deleteHistory = mockk<DeleteAttendanceHistoryUseCase>(relaxed = true)
    private val deleteVisit = mockk<DeleteVisitUseCase>(relaxed = true)

    // Sunday 15 Nov 2026, 12:00 UTC (minute-aligned for the ticker test).
    private val now = Instant.parse("2026-11-15T12:00:00Z")
    private val fixedClock = Clock.fixed(now, ZoneOffset.UTC)

    private fun visit(
        id: Long,
        enter: String,
        exit: String?,
        office: String? = "HQ",
        needsReview: Boolean = false,
    ) = HistoryVisit(
        attendance = Attendance(
            id = id,
            officeId = 1,
            enterTime = Instant.parse(enter),
            exitTime = exit?.let(Instant::parse),
            createdTime = Instant.parse(enter),
        ),
        officeName = office,
        needsReview = needsReview,
    )

    private fun viewModel(history: List<HistoryVisit>, clock: Clock = fixedClock): AttendanceViewModel {
        every { observeHistory() } returns flowOf(history)
        return AttendanceViewModel(observeHistory, deleteHistory, deleteVisit, clock)
    }

    private suspend fun ReceiveTurbine<AttendanceUiState>.awaitLoaded(): AttendanceUiState {
        var state = awaitItem()
        while (state.isLoading) state = awaitItem()
        return state
    }

    @Test
    fun `groups visits into labelled days, newest first, with a day total`() = runTest {
        val vm = viewModel(
            listOf(
                visit(1, "2026-11-15T08:00:00Z", "2026-11-15T10:30:00Z"),
                visit(2, "2026-11-14T09:00:00Z", "2026-11-14T12:00:00Z"),
                visit(3, "2026-11-14T13:00:00Z", "2026-11-14T14:15:00Z", office = "Client"),
                visit(4, "2026-11-11T09:00:00Z", "2026-11-11T17:00:00Z"),
                visit(5, "2025-12-31T09:00:00Z", "2025-12-31T10:00:00Z"),
            ),
        )

        vm.uiState.test {
            val days = awaitLoaded().days

            assertEquals(listOf("Today", "Yesterday", "Wed, 11 Nov", "Wed, 31 Dec 2025"), days.map { it.label })
            assertEquals("2h 30m", days[0].total)
            // 3h + 1h15m, with the later visit listed first.
            assertEquals("4h 15m", days[1].total)
            assertEquals(listOf("Client", "HQ"), days[1].visits.map { it.officeName })
            assertEquals("13:00 – 14:15", days[1].visits.first().timeRange)
            assertEquals("1h 15m", days[1].visits.first().duration)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `open visit shows time so far and counts toward today`() = runTest {
        val vm = viewModel(
            listOf(
                visit(1, "2026-11-15T09:15:00Z", exit = null),
                visit(2, "2026-11-15T07:00:00Z", "2026-11-15T08:00:00Z"),
            ),
        )

        vm.uiState.test {
            val today = awaitLoaded().days.single()
            val open = today.visits.first()

            assertTrue(open.inProgress)
            assertEquals("09:15 – now", open.timeRange)
            assertEquals("2h 45m", open.duration)
            assertEquals("3h 45m", today.total)
            assertFalse(today.visits[1].inProgress)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `open visit time ticks forward each minute`() = runTest {
        val vm = viewModel(
            listOf(visit(1, "2026-11-15T11:30:00Z", exit = null)),
            clock = SchedulerClock(now, testScheduler),
        )

        vm.uiState.test {
            assertEquals("30m", awaitLoaded().days.single().visits.single().duration)
            assertEquals("31m", awaitItem().days.single().visits.single().duration)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `flags, unknown office and zero totals are mapped`() = runTest {
        val vm = viewModel(
            listOf(
                visit(1, "2026-11-15T11:00:00Z", "2026-11-15T11:00:00Z", office = null, needsReview = true),
            ),
        )

        vm.uiState.test {
            val day = awaitLoaded().days.single()

            assertNull(day.total)
            assertEquals("Unknown office", day.visits.single().officeName)
            assertTrue(day.visits.single().needsReview)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `no history is empty`() = runTest {
        val vm = viewModel(emptyList())

        vm.uiState.test {
            assertTrue(awaitLoaded().isEmpty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clear history delegates to the use case`() = runTest {
        val vm = viewModel(emptyList())
        vm.clearHistory()

        coVerify { deleteHistory() }
    }

    @Test
    fun `delete removes a single record via the use case`() = runTest {
        val vm = viewModel(emptyList())
        vm.delete(7L)

        coVerify { deleteVisit(7L) }
    }
}

/** A clock that advances with the coroutine test scheduler's virtual time. */
private class SchedulerClock(
    private val base: Instant,
    private val scheduler: TestCoroutineScheduler,
    private val zone: ZoneId = ZoneOffset.UTC,
) : Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = SchedulerClock(base, scheduler, zone)
    override fun instant(): Instant = base.plusMillis(scheduler.currentTime)
}
