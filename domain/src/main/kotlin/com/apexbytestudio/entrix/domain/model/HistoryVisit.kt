package com.apexbytestudio.entrix.domain.model

import com.apexbytestudio.entrix.core.model.Attendance

/**
 * An attendance record paired with the name of the office it belongs to, so history can show which
 * site each visit was at. [officeName] is null if the office was since deleted (orphaned record).
 */
data class HistoryVisit(
    val attendance: Attendance,
    val officeName: String?,
    /** True when the exit is implausible (missed clock-out) and should be reviewed by the user. */
    val needsReview: Boolean = false,
)
