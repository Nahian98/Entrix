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

    /** True when the visit's exit is implausible (too long) and should be flagged for the user. */
    fun needsReview(attendance: Attendance): Boolean =
        attendance.duration?.let { it >= MAX_VISIT_DURATION } ?: false
}
