package com.apexbytestudio.entrix.domain.repository

import com.apexbytestudio.entrix.core.model.Attendance
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Single source of truth for attendance records. A visit is *open* from the moment it is
 * created until [closeVisit] stamps its exit time; at most one visit is open at a time per
 * office in the MVP.
 */
interface AttendanceRepository {
    /** Full history, newest first. */
    fun observeHistory(): Flow<List<Attendance>>

    /** One-shot full history, newest first — used by exports/reports that don't need a stream. */
    suspend fun getHistory(): List<Attendance>

    /** The currently open visit (entered, not yet exited), or `null` when outside. */
    fun observeOpenVisit(): Flow<Attendance?>

    /** All currently open visits — at most one per office. Empty when outside every office. */
    fun observeOpenVisits(): Flow<List<Attendance>>

    suspend fun getOpenVisit(officeId: Long): Attendance?

    /** Opens a new visit and returns its generated id. */
    suspend fun openVisit(officeId: Long, enterTime: Instant, createdTime: Instant): Long

    suspend fun closeVisit(id: Long, exitTime: Instant)

    /** Deletes a single record by id (per-record delete / correction). */
    suspend fun deleteVisit(id: Long)

    suspend fun deleteAll()
}
