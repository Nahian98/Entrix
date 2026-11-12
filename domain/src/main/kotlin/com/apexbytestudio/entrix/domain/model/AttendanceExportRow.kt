package com.apexbytestudio.entrix.domain.model

import java.time.Instant

/** One attendance record flattened for a raw export, with its office name resolved. */
data class AttendanceExportRow(
    val officeId: Long,
    val officeName: String?,
    val enterTime: Instant,
    val exitTime: Instant?,
    val durationMinutes: Long?,
)
