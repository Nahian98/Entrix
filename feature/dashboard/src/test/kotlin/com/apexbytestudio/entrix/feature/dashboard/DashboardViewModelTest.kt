package com.apexbytestudio.entrix.feature.dashboard

import app.cash.turbine.test
import com.apexbytestudio.entrix.core.model.Attendance
import com.apexbytestudio.entrix.core.model.Office
import com.apexbytestudio.entrix.core.testing.MainDispatcherRule
import com.apexbytestudio.entrix.domain.model.DashboardSnapshot
import com.apexbytestudio.entrix.domain.usecase.ObserveDashboardUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeDashboard = mockk<ObserveDashboardUseCase>()

    @Test
    fun `maps snapshot to a populated inside-office ui state`() = runTest {
        val office = Office(id = 1, name = "HQ", latitude = 1.0, longitude = 2.0, radiusMeters = 150f)
        val openVisit = Attendance(
            id = 1, officeId = 1,
            enterTime = Instant.parse("2026-06-29T09:00:00Z"),
            exitTime = null,
            createdTime = Instant.parse("2026-06-29T09:00:00Z"),
        )
        every { observeDashboard() } returns flowOf(
            DashboardSnapshot(
                office = office,
                openVisit = openVisit,
                todayVisits = listOf(openVisit),
                trackingEnabled = true,
            ),
        )

        val viewModel = DashboardViewModel(observeDashboard)

        viewModel.uiState.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()

            assertFalse(state.isLoading)
            assertTrue(state.officeConfigured)
            assertTrue(state.insideOffice)
            assertEquals("HQ", state.officeName)
            assertTrue(state.trackingEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty snapshot maps to no office configured`() = runTest {
        every { observeDashboard() } returns flowOf(DashboardSnapshot.Empty)

        val viewModel = DashboardViewModel(observeDashboard)

        viewModel.uiState.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()

            assertFalse(state.officeConfigured)
            assertFalse(state.insideOffice)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
