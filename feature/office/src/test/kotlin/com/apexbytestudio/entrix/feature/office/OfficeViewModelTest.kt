package com.apexbytestudio.entrix.feature.office

import app.cash.turbine.test
import com.apexbytestudio.entrix.core.model.Coordinates
import com.apexbytestudio.entrix.core.model.Office
import com.apexbytestudio.entrix.core.testing.MainDispatcherRule
import com.apexbytestudio.entrix.domain.usecase.GetCurrentLocationUseCase
import com.apexbytestudio.entrix.domain.usecase.ObserveOfficeUseCase
import com.apexbytestudio.entrix.domain.usecase.SaveOfficeUseCase
import com.apexbytestudio.entrix.domain.usecase.SyncGeofencesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OfficeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeOffice = mockk<ObserveOfficeUseCase>()
    private val saveOffice = mockk<SaveOfficeUseCase>()
    private val syncGeofences = mockk<SyncGeofencesUseCase>()
    private val getCurrentLocation = mockk<GetCurrentLocationUseCase>()

    @Before
    fun setUp() {
        // No pre-existing office by default (VM prefills from this in init).
        every { observeOffice() } returns flowOf(null)
    }

    private fun viewModel() = OfficeViewModel(observeOffice, saveOffice, syncGeofences, getCurrentLocation)

    @Test
    fun `valid save persists, syncs geofences and emits Saved`() = runTest {
        coEvery { saveOffice(any()) } returns Result.success(
            Office(id = 1, name = "My Office", latitude = 1.0, longitude = 2.0, radiusMeters = 150f),
        )
        coEvery { syncGeofences() } returns Result.success(Unit)

        val vm = viewModel()
        vm.onLatitudeChange("1.0")
        vm.onLongitudeChange("2.0")

        vm.events.test {
            vm.save()
            assertEquals(OfficeEvent.Saved, awaitItem())
        }
        coVerify { saveOffice(any()) }
        coVerify { syncGeofences() }
    }

    @Test
    fun `save with invalid coordinates surfaces an error and does not persist`() = runTest {
        val vm = viewModel()
        vm.onLatitudeChange("not-a-number")
        vm.onLongitudeChange("2.0")

        vm.save()

        assertEquals("Enter a valid latitude and longitude.", vm.uiState.value.errorMessage)
        coVerify(exactly = 0) { saveOffice(any()) }
    }

    @Test
    fun `use current location fills coordinates`() = runTest {
        coEvery { getCurrentLocation() } returns Result.success(Coordinates(23.78, 90.41))

        val vm = viewModel()
        vm.useCurrentLocation()

        assertEquals("23.780000", vm.uiState.value.latitude)
        assertEquals("90.410000", vm.uiState.value.longitude)
    }
}
