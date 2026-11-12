package com.apexbytestudio.entrix.domain.usecase

import com.apexbytestudio.entrix.core.model.Attendance
import com.apexbytestudio.entrix.domain.fake.FakeAttendanceRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class DeleteVisitUseCaseTest {

    private val attendanceRepo = FakeAttendanceRepository()
    private val useCase = DeleteVisitUseCase(attendanceRepo)

    private fun visit(id: Long) = Attendance(
        id = id,
        officeId = 1,
        enterTime = Instant.parse("2026-06-22T09:00:00Z"),
        exitTime = Instant.parse("2026-06-22T11:00:00Z"),
        createdTime = Instant.parse("2026-06-22T09:00:00Z"),
    )

    @Test
    fun `deletes only the targeted record`() = runTest {
        attendanceRepo.seed(visit(1), visit(2), visit(3))

        useCase(2L)

        assertEquals(listOf(1L, 3L), attendanceRepo.current().map { it.id }.sorted())
    }
}
