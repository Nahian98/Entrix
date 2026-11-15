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
    /** When the current (still open) visit began; `null` when not inside. */
    val currentVisitStart: Instant? = null,
    /** Number of visits that started today, including an open one. */
    val todayVisitCount: Int = 0,
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

    /**
     * Today's total *including* time spent so far in any open visit, measured up to [now]. An open
     * visit that began before [startOfToday] (e.g. overnight) only counts from the start of today.
     * Taking `now` as a parameter keeps this pure; the presentation layer ticks it.
     */
    fun todayTotalAt(now: Instant, startOfToday: Instant): Duration =
        offices.fold(todayTotalDuration) { acc, status ->
            val start = status.currentVisitStart ?: return@fold acc
            val elapsed = Duration.between(maxOf(start, startOfToday), now)
            if (elapsed.isNegative) acc else acc + elapsed
        }

    companion object {
        val Empty = DashboardSnapshot(
            offices = emptyList(),
            trackingEnabled = false,
        )
    }
}
