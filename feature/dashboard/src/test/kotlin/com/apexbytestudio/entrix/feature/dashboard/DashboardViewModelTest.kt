package com.apexbytestudio.entrix.feature.dashboard

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.apexbytestudio.entrix.core.model.Office
import com.apexbytestudio.entrix.core.testing.MainDispatcherRule
import com.apexbytestudio.entrix.domain.model.DashboardSnapshot
import com.apexbytestudio.entrix.domain.model.OfficeStatus
import com.apexbytestudio.entrix.domain.usecase.ObserveDashboardUseCase
import com.apexbytestudio.entrix.domain.usecase.ReconcileOpenVisitsUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeDashboard = mockk<ObserveDashboardUseCase>()
    private val reconcileOpenVisits = mockk<ReconcileOpenVisitsUseCase>(relaxed = true)

    // 11:30 UTC on 28 Jun 2026, minute-aligned so the ticker's next emission is exactly +60s.
    private val base = Instant.parse("2026-06-28T11:30:00Z")
    private val fixedClock = Clock.fixed(base, ZoneOffset.UTC)

    private val hq = Office(id = 1, name = "HQ", latitude = 1.0, longitude = 2.0, radiusMeters = 150f)
    private val client = Office(id = 2, name = "Client", latitude = 3.0, longitude = 4.0, radiusMeters = 150f)

    private fun at(time: String) = Instant.parse("2026-06-28T${time}:00Z")

    private fun inside(office: Office, since: Instant, completed: Duration = Duration.ZERO, visits: Int = 1) =
        OfficeStatus(
            office = office,
            isInside = true,
            todayFirstEnter = since,
            todayLastExit = null,
            todayCompletedDuration = completed,
            currentVisitStart = since,
            todayVisitCount = visits,
        )

    private fun visited(office: Office, enter: Instant, exit: Instant) = OfficeStatus(
        office = office,
        isInside = false,
        todayFirstEnter = enter,
        todayLastExit = exit,
        todayCompletedDuration = Duration.between(enter, exit),
        todayVisitCount = 1,
    )

    private fun notToday(office: Office) = OfficeStatus(
        office = office,
        isInside = false,
        todayFirstEnter = null,
        todayLastExit = null,
        todayCompletedDuration = Duration.ZERO,
    )

    private fun viewModel(snapshot: DashboardSnapshot, clock: Clock = fixedClock): DashboardViewModel {
        every { observeDashboard() } returns flowOf(snapshot)
        return DashboardViewModel(observeDashboard, reconcileOpenVisits, clock)
    }

    private suspend fun ReceiveTurbine<DashboardUiState>.awaitLoaded(): DashboardUiState {
        var state = awaitItem()
        while (state.isLoading) state = awaitItem()
        return state
    }

    @Test
    fun `inside an office shows a live total including the open visit`() = runTest {
        val vm = viewModel(
            DashboardSnapshot(
                offices = listOf(
                    inside(hq, since = at("09:00")),
                    visited(client, enter = at("07:00"), exit = at("08:00")),
                ),
                trackingEnabled = true,
            ),
        )

        vm.uiState.test {
            val state = awaitLoaded()

            // 1h completed at Client + 2h30m so far at HQ.
            assertEquals(TodayStatus.AtOffice("HQ", checkedInAt = "09:00", totalToday = "3h 30m"), state.status)
            assertTrue(state.insideAnyOffice)
            assertEquals(listOf("First in", "Visits"), state.todayFacts.map { it.label })
            assertEquals("07:00", state.todayFacts.first().value)
            assertEquals(OfficePresence.HereNow, state.offices[0].presence)
            assertEquals(OfficePresence.VisitedToday("1h 0m"), state.offices[1].presence)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `total ticks forward each minute while inside`() = runTest {
        val vm = viewModel(
            DashboardSnapshot(offices = listOf(inside(hq, since = at("11:00"))), trackingEnabled = true),
            clock = SchedulerClock(base, testScheduler),
        )

        vm.uiState.test {
            assertEquals("30m", (awaitLoaded().status as TodayStatus.AtOffice).totalToday)
            // The next emission comes from the ticker one virtual minute later.
            assertEquals("31m", (awaitItem().status as TodayStatus.AtOffice).totalToday)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `paused tracking takes priority over being inside`() = runTest {
        val vm = viewModel(
            DashboardSnapshot(offices = listOf(inside(hq, since = at("09:00"))), trackingEnabled = false),
        )

        vm.uiState.test {
            val state = awaitLoaded()

            assertEquals(TodayStatus.TrackingPaused, state.status)
            // The open visit is no longer tracked, so the office must not read "Here now".
            assertEquals(OfficePresence.VisitedToday(null), state.offices.single().presence)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `left the office shows done for now with last exit`() = runTest {
        val vm = viewModel(
            DashboardSnapshot(
                offices = listOf(visited(hq, enter = at("08:00"), exit = at("10:15")), notToday(client)),
                trackingEnabled = true,
            ),
        )

        vm.uiState.test {
            val state = awaitLoaded()

            assertEquals(TodayStatus.DoneForNow(totalToday = "2h 15m", leftAt = "10:15"), state.status)
            assertFalse(state.insideAnyOffice)
            assertEquals(listOf("First in", "Visit", "Last out"), state.todayFacts.map { it.label })
            assertEquals(OfficePresence.NotToday, state.offices[1].presence)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `no visits today shows not in yet without facts`() = runTest {
        val vm = viewModel(DashboardSnapshot(offices = listOf(notToday(hq)), trackingEnabled = true))

        vm.uiState.test {
            val state = awaitLoaded()

            assertTrue(state.officeConfigured)
            assertEquals(TodayStatus.NotInYet, state.status)
            assertTrue(state.todayFacts.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty snapshot maps to no office configured`() = runTest {
        val vm = viewModel(DashboardSnapshot.Empty)

        vm.uiState.test {
            val state = awaitLoaded()

            assertFalse(state.officeConfigured)
            assertFalse(state.insideAnyOffice)
            cancelAndIgnoreRemainingEvents()
        }
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
