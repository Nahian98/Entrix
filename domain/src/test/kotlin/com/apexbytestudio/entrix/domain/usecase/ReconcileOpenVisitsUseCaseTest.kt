package com.apexbytestudio.entrix.domain.usecase

import com.apexbytestudio.entrix.core.model.Attendance
import com.apexbytestudio.entrix.domain.fake.FakeAttendanceRepository
import com.apexbytestudio.entrix.domain.model.AttendanceRules
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ReconcileOpenVisitsUseCaseTest {

    private val now = Instant.parse("2026-11-13T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val attendanceRepo = FakeAttendanceRepository()
    private val useCase = ReconcileOpenVisitsUseCase(attendanceRepo, clock)

    private fun open(id: Long, enter: Instant) =
        Attendance(id = id, officeId = 1, enterTime = enter, exitTime = null, createdTime = enter)

    @Test
    fun `caps a visit left open beyond the max at enter plus the cap`() = runTest {
        val enter = now.minus(AttendanceRules.MAX_VISIT_DURATION).minusSeconds(3600) // open > cap
        attendanceRepo.seed(open(1, enter))

        useCase()

        val visit = attendanceRepo.current().single()
        assertEquals(enter.plus(AttendanceRules.MAX_VISIT_DURATION), visit.exitTime)
        assertTrue(AttendanceRules.needsReview(visit)) // capped visits are flagged for review
    }

    @Test
    fun `leaves a freshly open visit untouched`() = runTest {
        val enter = now.minusSeconds(3600) // open 1h, well under the cap
        attendanceRepo.seed(open(1, enter))

        useCase()

        val visit = attendanceRepo.current().single()
        assertNull(visit.exitTime)
        assertFalse(AttendanceRules.needsReview(visit))
    }

    @Test
    fun `is idempotent — a second run does not move the capped exit`() = runTest {
        val enter = now.minus(AttendanceRules.MAX_VISIT_DURATION).minusSeconds(3600)
        attendanceRepo.seed(open(1, enter))

        useCase()
        val afterFirst = attendanceRepo.current().single().exitTime
        useCase()
        val afterSecond = attendanceRepo.current().single().exitTime

        assertEquals(afterFirst, afterSecond)
    }
}
