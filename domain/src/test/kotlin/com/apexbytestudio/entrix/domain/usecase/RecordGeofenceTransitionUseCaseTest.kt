package com.apexbytestudio.entrix.domain.usecase

import com.apexbytestudio.entrix.core.model.Attendance
import com.apexbytestudio.entrix.core.model.GeofenceTransition
import com.apexbytestudio.entrix.domain.fake.FakeAttendanceRepository
import com.apexbytestudio.entrix.domain.fake.FakeTrackingStateRepository
import com.apexbytestudio.entrix.domain.model.AttendanceRules
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class RecordGeofenceTransitionUseCaseTest {

    private val officeId = 42L
    private val attendance = FakeAttendanceRepository()
    private val tracking = FakeTrackingStateRepository(enabled = true)
    private val useCase = RecordGeofenceTransitionUseCase(attendance, tracking)

    private val t0 = Instant.parse("2026-06-28T08:00:00Z") // ENTER
    private val t1 = Instant.parse("2026-06-28T08:02:00Z") // DWELL
    private val t2 = Instant.parse("2026-06-28T17:00:00Z") // EXIT

    @Test
    fun `enter caches pending time without opening a visit`() = runTest {
        val outcome = useCase(officeId, GeofenceTransition.Enter, t0)

        assertEquals(TransitionOutcome.EntryCached, outcome)
        assertEquals(t0, tracking.getPendingEnterTime(officeId))
        assertTrue(attendance.current().isEmpty())
    }

    @Test
    fun `dwell opens a visit back-dated to the cached enter time`() = runTest {
        useCase(officeId, GeofenceTransition.Enter, t0)

        val outcome = useCase(officeId, GeofenceTransition.Dwell, t1)

        assertEquals(TransitionOutcome.VisitOpened, outcome)
        val open = attendance.getOpenVisit(officeId)!!
        assertEquals(t0, open.enterTime) // back-dated, not t1
        assertEquals(t1, open.createdTime)
        assertNull(tracking.getPendingEnterTime(officeId)) // cache cleared
    }

    @Test
    fun `dwell without a cached enter falls back to the dwell time`() = runTest {
        val outcome = useCase(officeId, GeofenceTransition.Dwell, t1)

        assertEquals(TransitionOutcome.VisitOpened, outcome)
        assertEquals(t1, attendance.getOpenVisit(officeId)!!.enterTime)
    }

    @Test
    fun `exit closes the open visit and clears any pending enter`() = runTest {
        useCase(officeId, GeofenceTransition.Enter, t0)
        useCase(officeId, GeofenceTransition.Dwell, t1)

        val outcome = useCase(officeId, GeofenceTransition.Exit, t2)

        assertEquals(TransitionOutcome.VisitClosed, outcome)
        assertNull(attendance.getOpenVisit(officeId))
        val record = attendance.current().single()
        assertEquals(t0, record.enterTime)
        assertEquals(t2, record.exitTime)
    }

    @Test
    fun `drive-by enter then exit creates no record`() = runTest {
        useCase(officeId, GeofenceTransition.Enter, t0)
        val outcome = useCase(officeId, GeofenceTransition.Exit, t1)

        assertEquals(TransitionOutcome.Ignored, outcome)
        assertTrue(attendance.current().isEmpty())
        assertNull(tracking.getPendingEnterTime(officeId))
    }

    @Test
    fun `duplicate dwell does not open a second visit`() = runTest {
        useCase(officeId, GeofenceTransition.Dwell, t1)
        val outcome = useCase(officeId, GeofenceTransition.Dwell, t1.plusSeconds(30))

        assertEquals(TransitionOutcome.Ignored, outcome)
        assertEquals(1, attendance.current().size)
    }

    @Test
    fun `second enter while one is pending keeps the earliest time`() = runTest {
        useCase(officeId, GeofenceTransition.Enter, t0)
        val outcome = useCase(officeId, GeofenceTransition.Enter, t0.plusSeconds(60))

        assertEquals(TransitionOutcome.Ignored, outcome)
        assertEquals(t0, tracking.getPendingEnterTime(officeId))
    }

    @Test
    fun `enter while already inside is ignored`() = runTest {
        attendance.seed(
            Attendance(id = 1, officeId = officeId, enterTime = t0, exitTime = null, createdTime = t0),
        )
        val outcome = useCase(officeId, GeofenceTransition.Enter, t1)

        assertEquals(TransitionOutcome.Ignored, outcome)
        assertNull(tracking.getPendingEnterTime(officeId))
    }

    @Test
    fun `exit without an open visit is ignored`() = runTest {
        val outcome = useCase(officeId, GeofenceTransition.Exit, t2)

        assertEquals(TransitionOutcome.Ignored, outcome)
        assertTrue(attendance.current().isEmpty())
    }

    @Test
    fun `a pending enter at one office does not leak into another`() = runTest {
        val other = 99L

        // Office A gets a pending ENTER; office B then DWELLs and must use its own (dwell) time,
        // not A's cached time, and A's pending time must survive.
        useCase(officeId, GeofenceTransition.Enter, t0)
        val outcome = useCase(other, GeofenceTransition.Dwell, t1)

        assertEquals(TransitionOutcome.VisitOpened, outcome)
        assertEquals(t1, attendance.getOpenVisit(other)!!.enterTime)
        assertNull(attendance.getOpenVisit(officeId))
        assertEquals(t0, tracking.getPendingEnterTime(officeId))
    }

    @Test
    fun `transitions are ignored when tracking is disabled`() = runTest {
        tracking.setTrackingEnabled(false)

        assertEquals(TransitionOutcome.Ignored, useCase(officeId, GeofenceTransition.Enter, t0))
        assertEquals(TransitionOutcome.Ignored, useCase(officeId, GeofenceTransition.Dwell, t1))
        assertTrue(attendance.current().isEmpty())
        assertNull(tracking.getPendingEnterTime(officeId))
    }

    // ---- exit grace period ----

    /** A visit that was open from t0 and closed (possibly falsely) at [exit]. */
    private fun seedClosedVisit(exit: Instant, enter: Instant = t0) = attendance.seed(
        Attendance(id = 7, officeId = officeId, enterTime = enter, exitTime = exit, createdTime = t1),
    )

    @Test
    fun `false exit then enter within the grace period reopens the same visit`() = runTest {
        // Real-device case: EXIT from a coarse fix, ENTER 12 seconds later, user never left.
        useCase(officeId, GeofenceTransition.Enter, t0)
        useCase(officeId, GeofenceTransition.Dwell, t1)
        val falseExit = Instant.parse("2026-06-28T15:53:46Z")
        useCase(officeId, GeofenceTransition.Exit, falseExit)

        val outcome = useCase(officeId, GeofenceTransition.Enter, falseExit.plusSeconds(12))

        assertEquals(TransitionOutcome.VisitReopened, outcome)
        val visit = attendance.current().single() // no second record
        assertNull(visit.exitTime)
        assertEquals(t0, visit.enterTime) // original check-in kept
        assertNull(tracking.getPendingEnterTime(officeId))
    }

    @Test
    fun `dwell within the grace period also reopens`() = runTest {
        seedClosedVisit(exit = t2)

        val outcome = useCase(officeId, GeofenceTransition.Dwell, t2.plus(Duration.ofMinutes(3)))

        assertEquals(TransitionOutcome.VisitReopened, outcome)
        assertEquals(1, attendance.current().size)
        assertNull(attendance.current().single().exitTime)
    }

    @Test
    fun `re-entry exactly at the end of the grace period still reopens`() = runTest {
        seedClosedVisit(exit = t2)

        val outcome = useCase(officeId, GeofenceTransition.Enter, t2.plus(AttendanceRules.EXIT_GRACE_PERIOD))

        assertEquals(TransitionOutcome.VisitReopened, outcome)
    }

    @Test
    fun `re-entry after the grace period starts a new visit as usual`() = runTest {
        seedClosedVisit(exit = t2)
        val later = t2.plus(AttendanceRules.EXIT_GRACE_PERIOD).plusSeconds(1)

        val outcome = useCase(officeId, GeofenceTransition.Enter, later)

        assertEquals(TransitionOutcome.EntryCached, outcome)
        assertEquals(t2, attendance.current().single().exitTime) // old visit stays closed
        assertEquals(later, tracking.getPendingEnterTime(officeId))
    }

    @Test
    fun `boundary flapping keeps a single open visit`() = runTest {
        useCase(officeId, GeofenceTransition.Dwell, t1)
        var at = t2
        repeat(4) {
            useCase(officeId, GeofenceTransition.Exit, at)
            at = at.plusSeconds(20)
            useCase(officeId, GeofenceTransition.Enter, at)
            at = at.plusSeconds(40)
        }

        val visit = attendance.current().single()
        assertNull(visit.exitTime)
        assertEquals(t1, visit.enterTime)
    }

    @Test
    fun `a capped stale visit is never reopened`() = runTest {
        // Closed by the 16h cap: duration == MAX_VISIT_DURATION, flagged for review.
        val cappedExit = t0.plus(AttendanceRules.MAX_VISIT_DURATION)
        seedClosedVisit(exit = cappedExit)

        val outcome = useCase(officeId, GeofenceTransition.Enter, cappedExit.plusSeconds(30))

        assertEquals(TransitionOutcome.EntryCached, outcome)
        assertEquals(cappedExit, attendance.current().single().exitTime)
    }

    @Test
    fun `a recent exit at another office does not reopen anything here`() = runTest {
        attendance.seed(
            Attendance(id = 7, officeId = 99L, enterTime = t0, exitTime = t2, createdTime = t1),
        )

        val outcome = useCase(officeId, GeofenceTransition.Enter, t2.plusSeconds(30))

        assertEquals(TransitionOutcome.EntryCached, outcome)
        assertEquals(t2, attendance.current().single().exitTime)
    }
}
