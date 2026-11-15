package com.apexbytestudio.entrix.feature.attendance

data class AttendanceUiState(
    val isLoading: Boolean = true,
    val days: List<AttendanceDayUi> = emptyList(),
) {
    val isEmpty: Boolean get() = !isLoading && days.isEmpty()
}

data class AttendanceDayUi(
    /** Stable key (ISO date) for the list. */
    val key: String,
    /** "Today", "Yesterday", or a short date such as "Wed, 11 Nov". */
    val label: String,
    /** Time spent that day across all visits; null when nothing has accrued yet. */
    val total: String?,
    val visits: List<AttendanceVisitUi>,
)

data class AttendanceVisitUi(
    val id: Long,
    val officeName: String,
    /** "09:28 – 18:14", or "17:39 – now" while the visit is still open. */
    val timeRange: String,
    /** Completed duration, or time so far for an open visit. */
    val duration: String,
    val inProgress: Boolean = false,
    val needsReview: Boolean = false,
)
