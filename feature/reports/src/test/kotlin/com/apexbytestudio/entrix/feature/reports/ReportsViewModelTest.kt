package com.apexbytestudio.entrix.feature.reports

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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import java.time.LocalDate

class ReportsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeReport = mockk<ObserveReportUseCase>()
    private val exportTimesheet = mockk<ExportTimesheetUseCase>()

    private val start = LocalDate.of(2026, 6, 22)
    private val end = LocalDate.of(2026, 6, 28)

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
        bars = listOf(ReportBar(label = "Mon", duration = Duration.ofHours(2))),
        offices = listOf(OfficeBreakdown(officeName = "HQ", duration = Duration.ofHours(6), visitCount = 2)),
    )

    private fun viewModel() = ReportsViewModel(observeReport, exportTimesheet)

    @Test
    fun `maps a populated snapshot to ui state`() = runTest {
        every { observeReport(any(), any()) } returns flowOf(populated)

        val vm = viewModel()

        vm.uiState.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()

            assertFalse(state.isLoading)
            assertEquals(ReportPeriod.Week, state.period)
            assertEquals("22–28 Jun", state.rangeLabel)
            assertEquals("6h 0m", state.total)
            assertEquals(2, state.visitCount)
            assertEquals(2, state.daysPresent)
            assertEquals("3h 0m", state.averagePerDay)
            assertEquals(500, state.trendPercent) // (6h - 1h) / 1h
            assertTrue(state.hasData)
            assertTrue(state.canExport)
            assertEquals(1, state.bars.size)
            assertEquals("Mon", state.bars.single().label)
            assertEquals(120f, state.bars.single().value) // 2h in minutes
            assertEquals(1, state.offices.size)
            assertEquals("HQ", state.offices.single().name)
            assertEquals("6h 0m", state.offices.single().duration)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selecting a period re-queries for that period`() = runTest {
        val month = populated.copy(period = ReportPeriod.Month, rangeLabel = "June 2026")
        every { observeReport(ReportPeriod.Week, any()) } returns flowOf(populated)
        every { observeReport(ReportPeriod.Month, any()) } returns flowOf(month)

        val vm = viewModel()

        vm.uiState.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()
            assertEquals(ReportPeriod.Week, state.period)

            vm.selectPeriod(ReportPeriod.Month)

            val switched = awaitItem()
            assertEquals(ReportPeriod.Month, switched.period)
            assertEquals("June 2026", switched.rangeLabel)

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
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()

            vm.events.test {
                vm.exportPdf()
                assertEquals(ReportsEvent.Share("/cache/exports/ts.pdf", "application/pdf"), awaitItem())
            }
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { exportTimesheet(start, end, any(), any(), "22–28 Jun") }
    }

    @Test
    fun `exportPdf emits Error on failure`() = runTest {
        every { observeReport(any(), any()) } returns flowOf(populated)
        coEvery { exportTimesheet(any(), any(), any(), any(), any()) } returns
            Result.failure(RuntimeException("disk full"))

        val vm = viewModel()

        vm.uiState.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()

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
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()

            vm.events.test {
                vm.exportPdf()
                expectNoEvents()
            }
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) { exportTimesheet(any(), any(), any(), any(), any()) }
    }
}
