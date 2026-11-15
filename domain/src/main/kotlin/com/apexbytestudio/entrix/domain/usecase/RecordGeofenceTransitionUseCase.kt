package com.apexbytestudio.entrix.domain.usecase

import com.apexbytestudio.entrix.core.model.GeofenceTransition
import com.apexbytestudio.entrix.domain.repository.AttendanceRepository
import com.apexbytestudio.entrix.domain.model.AttendanceRules
import com.apexbytestudio.entrix.domain.repository.TrackingStateRepository
import java.time.Duration
import java.time.Instant

/** What a transition actually did, surfaced for logging and for deterministic testing. */
enum class TransitionOutcome {
    /** Nothing changed (e.g. duplicate transition, or an exit with no open visit). */
    Ignored,

    /** An ENTER was cached as the candidate entry time, awaiting a confirming DWELL. */
    EntryCached,

    /** A DWELL confirmed presence and opened a visit. */
    VisitOpened,

    /** An EXIT closed the open visit. */
    VisitClosed,

    /** An ENTER/DWELL soon after a check-out reopened that visit (exit grace period). */
    VisitReopened,
}

/**
 * Turns a raw geofence [GeofenceTransition] into attendance state, applying the agreed policy:
 *
 *  - **ENTER** only caches the precise boundary-crossing time (persisted, so it survives process
 *    death). It does not open a visit, so brief boundary jitter / drive-bys never create records.
 *    The earliest pending ENTER wins; a later ENTER while one is already pending is ignored.
 *  - **DWELL** confirms the user stayed and opens a visit, back-dating [com.apexbytestudio.entrix.core.model.Attendance.enterTime]
 *    to the cached ENTER time (falling back to the dwell time if none was cached, e.g. INITIAL_TRIGGER
 *    while already inside).
 *  - **EXIT** closes the open visit and abandons any pending (un-confirmed) ENTER.
 *  - **Re-entry within [AttendanceRules.EXIT_GRACE_PERIOD]** of a check-out (ENTER or DWELL) reopens
 *    that visit with its original entry time, so a false EXIT from one bad location fix doesn't end
 *    or split a stay. Visits closed by the stale-visit cap are never reopened.
 *
 * Every branch is idempotent so duplicate transitions cannot corrupt state.
 */
class RecordGeofenceTransitionUseCase(
    private val attendanceRepository: AttendanceRepository,
    private val trackingStateRepository: TrackingStateRepository,
) {
    suspend operator fun invoke(
        officeId: Long,
        transition: GeofenceTransition,
        occurredAt: Instant,
    ): TransitionOutcome {
        if (!trackingStateRepository.isTrackingEnabled()) return TransitionOutcome.Ignored

        return when (transition) {
            GeofenceTransition.Enter -> onEnter(officeId, occurredAt)
            GeofenceTransition.Dwell -> onDwell(officeId, occurredAt)
            GeofenceTransition.Exit -> onExit(officeId, occurredAt)
        }
    }

    private suspend fun onEnter(officeId: Long, occurredAt: Instant): TransitionOutcome {
        // Already inside, or an ENTER is already pending: keep the earliest known entry time.
        if (attendanceRepository.getOpenVisit(officeId) != null) return TransitionOutcome.Ignored
        if (reopenIfJustLeft(officeId, occurredAt)) return TransitionOutcome.VisitReopened
        if (trackingStateRepository.getPendingEnterTime(officeId) != null) return TransitionOutcome.Ignored

        trackingStateRepository.setPendingEnterTime(officeId, occurredAt)
        return TransitionOutcome.EntryCached
    }

    private suspend fun onDwell(officeId: Long, occurredAt: Instant): TransitionOutcome {
        if (attendanceRepository.getOpenVisit(officeId) != null) return TransitionOutcome.Ignored
        if (reopenIfJustLeft(officeId, occurredAt)) return TransitionOutcome.VisitReopened

        val enterTime = trackingStateRepository.getPendingEnterTime(officeId) ?: occurredAt
        attendanceRepository.openVisit(
            officeId = officeId,
            enterTime = enterTime,
            createdTime = occurredAt,
        )
        trackingStateRepository.setPendingEnterTime(officeId, null)
        return TransitionOutcome.VisitOpened
    }

    private suspend fun onExit(officeId: Long, occurredAt: Instant): TransitionOutcome {
        trackingStateRepository.setPendingEnterTime(officeId, null)

        val openVisit = attendanceRepository.getOpenVisit(officeId)
            ?: return TransitionOutcome.Ignored

        attendanceRepository.closeVisit(id = openVisit.id, exitTime = occurredAt)
        return TransitionOutcome.VisitClosed
    }

    /**
     * Reopens the office's most recent visit if it was closed within the grace period before
     * [occurredAt]. Returns true when it did.
     */
    private suspend fun reopenIfJustLeft(officeId: Long, occurredAt: Instant): Boolean {
        val latest = attendanceRepository.getLatestVisit(officeId) ?: return false
        val exitTime = latest.exitTime ?: return false
        // A visit closed by the stale-visit cap is a missed EXIT, not a blip; leave it for review.
        if (AttendanceRules.needsReview(latest)) return false

        val sinceExit = Duration.between(exitTime, occurredAt)
        if (sinceExit.isNegative || sinceExit > AttendanceRules.EXIT_GRACE_PERIOD) return false

        attendanceRepository.reopenVisit(latest.id)
        trackingStateRepository.setPendingEnterTime(officeId, null)
        return true
    }
}
