package com.apexbytestudio.entrix.feature.settings

import app.cash.turbine.test
import com.apexbytestudio.entrix.core.testing.MainDispatcherRule
import com.apexbytestudio.entrix.domain.model.ExportFormat
import com.apexbytestudio.entrix.domain.model.ExportedDocument
import com.apexbytestudio.entrix.domain.usecase.ExportAttendanceUseCase
import com.apexbytestudio.entrix.domain.usecase.ObserveTrackingEnabledUseCase
import com.apexbytestudio.entrix.domain.usecase.SetTrackingEnabledUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeTracking = mockk<ObserveTrackingEnabledUseCase>()
    private val setTracking = mockk<SetTrackingEnabledUseCase>()
    private val exportAttendance = mockk<ExportAttendanceUseCase>()

    @Test
    fun `exposes tracking state from the use case`() = runTest {
        every { observeTracking() } returns flowOf(true)

        val vm = SettingsViewModel(observeTracking, setTracking, exportAttendance)

        vm.trackingEnabled.test {
            var enabled = awaitItem()
            if (!enabled) enabled = awaitItem()
            assertTrue(enabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggling tracking delegates to the use case`() = runTest {
        every { observeTracking() } returns flowOf(false)
        coEvery { setTracking(any()) } returns Result.success(Unit)

        val vm = SettingsViewModel(observeTracking, setTracking, exportAttendance)
        vm.setTracking(true)

        coVerify { setTracking(true) }
    }

    @Test
    fun `exporting data emits a share event`() = runTest {
        every { observeTracking() } returns flowOf(false)
        coEvery { exportAttendance(ExportFormat.Csv) } returns Result.success(
            ExportedDocument(absolutePath = "/cache/exports/a.csv", fileName = "a.csv", mimeType = "text/csv"),
        )

        val vm = SettingsViewModel(observeTracking, setTracking, exportAttendance)

        vm.events.test {
            vm.exportData(ExportFormat.Csv)
            val event = awaitItem()
            assertTrue(event is SettingsEvent.Share)
        }
    }
}
