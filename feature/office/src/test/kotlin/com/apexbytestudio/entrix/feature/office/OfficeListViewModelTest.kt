package com.apexbytestudio.entrix.feature.office

import app.cash.turbine.test
import com.apexbytestudio.entrix.core.model.Office
import com.apexbytestudio.entrix.core.testing.MainDispatcherRule
import com.apexbytestudio.entrix.domain.usecase.DeleteOfficeUseCase
import com.apexbytestudio.entrix.domain.usecase.ObserveOfficesUseCase
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

class OfficeListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeOffices = mockk<ObserveOfficesUseCase>()
    private val deleteOffice = mockk<DeleteOfficeUseCase>()

    private fun viewModel() = OfficeListViewModel(observeOffices, deleteOffice)

    @Test
    fun `maps offices to formatted list items`() = runTest {
        every { observeOffices() } returns flowOf(
            listOf(
                Office(id = 1, name = "HQ", latitude = 23.78, longitude = 90.41, radiusMeters = 150f),
            ),
        )

        val vm = viewModel()

        vm.uiState.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()

            assertFalse(state.isLoading)
            assertFalse(state.isEmpty)
            assertEquals(1, state.offices.size)
            val item = state.offices.single()
            assertEquals(1L, item.id)
            assertEquals("HQ", item.name)
            assertEquals("23.78000, 90.41000", item.coordinates)
            assertEquals("150 m", item.radius)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty office list reports isEmpty`() = runTest {
        every { observeOffices() } returns flowOf(emptyList())

        val vm = viewModel()

        vm.uiState.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()

            assertFalse(state.isLoading)
            assertTrue(state.isEmpty)
            assertTrue(state.offices.isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `delete forwards the id to the delete use case`() = runTest {
        every { observeOffices() } returns flowOf(emptyList())
        coEvery { deleteOffice(any()) } returns Unit

        val vm = viewModel()
        vm.delete(42L)

        coVerify { deleteOffice(42L) }
    }
}
