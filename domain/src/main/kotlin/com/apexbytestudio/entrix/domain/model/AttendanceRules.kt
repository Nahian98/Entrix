package com.apexbytestudio.entrix.domain.model

import com.apexbytestudio.entrix.core.model.Attendance
import java.time.Duration

/**
 * Plausibility rules for attendance records. Geofencing can miss an EXIT transition, leaving a visit
 * open until some much-later exit finally fires — producing a single "visit" spanning many hours or
 * days. Anything at or beyond [MAX_VISIT_DURATION] is treated as an unreliable exit that needs the
 * user's review, and open visits are capped at it (see ReconcileOpenVisitsUseCase).
 */
object AttendanceRules {
    /** A single continuous visit this long almost always means a missed EXIT, not a real stay. */
    val MAX_VISIT_DURATION: Duration = Duration.ofHours(16)

    /**
     * How soon after a check-out a return to the same office reopens that visit instead of
     * starting a new one. Indoors, a single coarse location fix (often requested by another app)
     * can land outside the geofence and fire a false EXIT followed shortly by an ENTER; without
     * this, the user is left "checked out" while still inside. A real departure isn't affected:
     * nobody re-enters within minutes, so the visit stays closed at the true exit time.
     */
    val EXIT_GRACE_PERIOD: Duration = Duration.ofMinutes(10)

    /** True when the visit's exit is implausible (too long) and should be flagged for the user. */
    fun needsReview(attendance: Attendance): Boolean =
        attendance.duration?.let { it >= MAX_VISIT_DURATION } ?: false
}
