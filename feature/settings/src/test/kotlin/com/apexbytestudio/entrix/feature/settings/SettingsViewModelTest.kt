package com.apexbytestudio.entrix.feature.settings

import app.cash.turbine.test
import com.apexbytestudio.entrix.core.testing.MainDispatcherRule
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

    @Test
    fun `exposes tracking state from the use case`() = runTest {
        every { observeTracking() } returns flowOf(true)

        val vm = SettingsViewModel(observeTracking, setTracking)

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

        val vm = SettingsViewModel(observeTracking, setTracking)
        vm.setTracking(true)

        coVerify { setTracking(true) }
    }
}
