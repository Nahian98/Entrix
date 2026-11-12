package com.apexbytestudio.entrix.domain.model

import com.apexbytestudio.entrix.core.model.Office
import java.time.Duration
import java.time.Instant

/**
 * Per-office status for the dashboard: whether the user is currently inside, and today's activity.
 * Derived values are computed here so the screen never re-implements the rules.
 */
data class OfficeStatus(
    val office: Office,
    val isInside: Boolean,
    val todayFirstEnter: Instant?,
    val todayLastExit: Instant?,
    /** Sum of completed visit durations at this office today (the open visit is excluded). */
    val todayCompletedDuration: Duration,
)

/**
 * Everything the dashboard needs in a single immutable snapshot. One [OfficeStatus] per configured
 * office, plus aggregates across all offices. Live "time since entry" for an open visit is
 * intentionally *not* here — that ticks in the presentation layer against `now`.
 */
data class DashboardSnapshot(
    val offices: List<OfficeStatus>,
    val trackingEnabled: Boolean,
) {
    val isOfficeConfigured: Boolean get() = offices.isNotEmpty()

    val isInsideAnyOffice: Boolean get() = offices.any { it.isInside }

    /** Combined completed duration across every office today. */
    val todayTotalDuration: Duration
        get() = offices.fold(Duration.ZERO) { acc, status -> acc + status.todayCompletedDuration }

    companion object {
        val Empty = DashboardSnapshot(
            offices = emptyList(),
            trackingEnabled = false,
        )
    }
}
