package com.apexbytestudio.entrix.feature.attendance

import app.cash.turbine.test
import com.apexbytestudio.entrix.core.model.Attendance
import com.apexbytestudio.entrix.core.testing.MainDispatcherRule
import com.apexbytestudio.entrix.domain.usecase.DeleteAttendanceHistoryUseCase
import com.apexbytestudio.entrix.domain.usecase.ObserveAttendanceHistoryUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class AttendanceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeHistory = mockk<ObserveAttendanceHistoryUseCase>()
    private val deleteHistory = mockk<DeleteAttendanceHistoryUseCase>(relaxed = true)

    @Test
    fun `groups visits into days`() = runTest {
        val today = Instant.now()
        val yesterday = today.minus(1, ChronoUnit.DAYS)
        every { observeHistory() } returns flowOf(
            listOf(
                Attendance(1, 1, today.minus(8, ChronoUnit.HOURS), today.minus(1, ChronoUnit.HOURS), today.minus(8, ChronoUnit.HOURS)),
                Attendance(2, 1, yesterday.minus(8, ChronoUnit.HOURS), yesterday, yesterday.minus(8, ChronoUnit.HOURS)),
            ),
        )

        val vm = AttendanceViewModel(observeHistory, deleteHistory)

        vm.uiState.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(2, state.days.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clear history delegates to the use case`() = runTest {
        every { observeHistory() } returns flowOf(emptyList())

        val vm = AttendanceViewModel(observeHistory, deleteHistory)
        vm.clearHistory()

        coVerify { deleteHistory() }
    }
}
