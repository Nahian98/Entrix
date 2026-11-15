package com.apexbytestudio.entrix.feature.reports

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.apexbytestudio.entrix.core.testing.MainDispatcherRule
import com.apexbytestudio.entrix.domain.model.ExportedDocument
import com.apexbytestudio.entrix.domain.model.OfficeBreakdown
import com.apexbytestudio.entrix.domain.model.ReportBar
import com.apexbytestudio.entrix.domain.model.ReportPeriod
import com.apexbytestudio.entrix.domain.model.ReportSnapshot
import com.apexbytestudio.entrix.domain.usecase.ExportTimesheetUseCase
import com.apexbytestudio.entrix.domain.usecase.ObserveReportUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ReportsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeReport = mockk<ObserveReportUseCase>()
    private val exportTimesheet = mockk<ExportTimesheetUseCase>()

    // "Today" is Thursday 25 Jun 2026, inside the 22–28 Jun week.
    private val clock = Clock.fixed(Instant.parse("2026-06-25T10:00:00Z"), ZoneOffset.UTC)
    private val start = LocalDate.of(2026, 6, 22)
    private val end = LocalDate.of(2026, 6, 28)

    private val hours = listOf(2L, 4L, 0L, 0L, 0L, 0L, 0L) // Mon 2h, Tue 4h, rest empty

    private val populated = ReportSnapshot(
        period = ReportPeriod.Week,
        start = start,
        end = end,
        rangeLabel = "22–28 Jun",
        totalDuration = Duration.ofHours(6),
        visitCount = 2,
        daysPresent = 2,
        averagePerActiveDay = Duration.ofHours(3),
        previousTotal = Duration.ofHours(1),
        bars = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            .mapIndexed { i, day -> ReportBar(day, Duration.ofHours(hours[i])) },
        offices = listOf(
            OfficeBreakdown(officeName = "HQ", duration = Duration.ofMinutes(270), visitCount = 1),
            OfficeBreakdown(officeName = "Client", duration = Duration.ofMinutes(90), visitCount = 1),
        ),
    )

    private fun viewModel() = ReportsViewModel(observeReport, exportTimesheet, clock)

    private suspend fun ReceiveTurbine<ReportsUiState>.awaitLoaded(): ReportsUiState {
        var state = awaitItem()
        while (state.isLoading) state = awaitItem()
        return state
    }

    @Test
    fun `maps a populated current week`() = runTest {
        every { observeReport(any(), any()) } returns flowOf(populated)

        viewModel().uiState.test {
            val state = awaitLoaded()

            assertEquals("This week", state.title)
            assertEquals("22–28 Jun", state.dateRange)
            assertFalse(state.canGoNext)
            assertEquals("6h 0m", state.total)
            assertEquals(ReportTrend(500, "vs previous week"), state.trend) // (6h - 1h) / 1h
            assertEquals(listOf("2", "3h 0m", "2"), state.stats.map { it.value })
            assertEquals(listOf("Days at office", "Avg per day", "Visits"), state.stats.map { it.label })
            assertTrue(state.hasData)
            assertTrue(state.canExport)
            assertEquals(120f, state.bars.first().value) // minutes
            assertEquals("2h 0m", state.bars.first().valueLabel)
            assertEquals("No time", state.bars[2].valueLabel)
            assertEquals(3, state.currentBarIndex) // Thursday
            // Average over days that have time only: (2h + 4h) / 2.
            assertEquals(180f, state.barAverage)
            assertEquals("avg 3h 0m", state.barAverageLabel)
            assertEquals(listOf(0.75f, 0.25f), state.offices.map { it.share })
            assertEquals(listOf("75%", "25%"), state.offices.map { it.sharePercent })
            assertEquals(listOf("1 visit", "1 visit"), state.offices.map { it.visits })
            assertFalse(state.hasVisitInProgress)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `navigating back relabels, and forward stops at the current period`() = runTest {
        every { observeReport(any(), any()) } returns flowOf(populated)
        val vm = viewModel()

        vm.uiState.test {
            assertEquals("This week", awaitLoaded().title)

            vm.previousPeriod()
            val last = awaitItem()
            assertEquals("Last week", last.title)
            assertTrue(last.canGoNext)

            vm.previousPeriod()
            val older = awaitItem()
            assertEquals("22–28 Jun", older.title)
            assertNull(older.dateRange)

            vm.nextPeriod()
            assertEquals("Last week", awaitItem().title)
            vm.nextPeriod()
            assertEquals("This week", awaitItem().title)

            vm.nextPeriod() // already current: ignored
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        verify(exactly = 0) { observeReport(any(), 1) }
    }

    @Test
    fun `selecting month re-queries and labels it`() = runTest {
        val month = populated.copy(
            period = ReportPeriod.Month,
            start = LocalDate.of(2026, 6, 1),
            end = LocalDate.of(2026, 6, 30),
            rangeLabel = "June 2026",
            bars = (0 until 5).map { ReportBar("w$it", Duration.ofHours(1)) },
        )
        every { observeReport(ReportPeriod.Week, any()) } returns flowOf(populated)
        every { observeReport(ReportPeriod.Month, any()) } returns flowOf(month)
        val vm = viewModel()

        vm.uiState.test {
            assertEquals(ReportPeriod.Week, awaitLoaded().period)

            vm.selectPeriod(ReportPeriod.Month)
            val switched = awaitItem()

            assertEquals(ReportPeriod.Month, switched.period)
            assertEquals("This month", switched.title)
            assertEquals("June 2026", switched.dateRange)
            assertEquals(ReportTrend(500, "vs previous month"), switched.trend)
            assertEquals(3, switched.currentBarIndex) // 25 Jun falls in days 22–28
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty period has no data, trend or highlight`() = runTest {
        val empty = ReportSnapshot.empty(ReportPeriod.Week, start, end, "22–28 Jun")
        every { observeReport(any(), any()) } returns flowOf(empty)

        viewModel().uiState.test {
            val state = awaitLoaded()

            assertFalse(state.hasData)
            assertFalse(state.canExport)
            assertNull(state.trend)
            assertNull(state.barAverage)
            assertFalse(state.hasVisitInProgress)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `flags a visit in progress that is not yet counted`() = runTest {
        every { observeReport(any(), any()) } returns flowOf(populated.copy(hasVisitInProgress = true))

        viewModel().uiState.test {
            assertTrue(awaitLoaded().hasVisitInProgress)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `exportPdf emits Share for the current period on success`() = runTest {
        every { observeReport(any(), any()) } returns flowOf(populated)
        coEvery { exportTimesheet(any(), any(), any(), any(), any()) } returns Result.success(
            ExportedDocument(absolutePath = "/cache/exports/ts.pdf", fileName = "ts.pdf", mimeType = "application/pdf"),
        )
        val vm = viewModel()

        // Collecting uiState keeps the snapshot cached (`latest`) and the stream subscribed
        // while the export runs.
        vm.uiState.test {
            awaitLoaded()
            vm.events.test {
                vm.exportPdf()
                assertEquals(ReportsEvent.Share("/cache/exports/ts.pdf", "application/pdf"), awaitItem())
            }
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { exportTimesheet(start, end, ZoneOffset.UTC, any(), "22–28 Jun") }
    }

    @Test
    fun `exportPdf emits Error on failure`() = runTest {
        every { observeReport(any(), any()) } returns flowOf(populated)
        coEvery { exportTimesheet(any(), any(), any(), any(), any()) } returns
            Result.failure(RuntimeException("disk full"))
        val vm = viewModel()

        vm.uiState.test {
            awaitLoaded()
            vm.events.test {
                vm.exportPdf()
                assertTrue(awaitItem() is ReportsEvent.Error)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `exportPdf does nothing when there is no data`() = runTest {
        val empty = ReportSnapshot.empty(ReportPeriod.Week, start, end, "22–28 Jun")
        every { observeReport(any(), any()) } returns flowOf(empty)
        val vm = viewModel()

        vm.uiState.test {
            awaitLoaded()
            vm.events.test {
                vm.exportPdf()
                expectNoEvents()
            }
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) { exportTimesheet(any(), any(), any(), any(), any()) }
    }
}
