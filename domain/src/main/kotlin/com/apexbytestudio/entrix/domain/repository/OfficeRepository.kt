package com.apexbytestudio.entrix.domain.repository

import com.apexbytestudio.entrix.core.model.Office
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for the configured offices. The app can track any number of offices.
 *
 * "Active" accessors ([observeOffices]/[getOffices]) exclude archived offices and back the office
 * list, dashboard and geofencing. The "all" accessors ([observeAllOffices]/[getAllOffices]) include
 * archived offices so history and reports can still resolve the name of a since-deleted office.
 */
interface OfficeRepository {
    fun observeOffices(): Flow<List<Office>>

    fun observeOffice(id: Long): Flow<Office?>

    suspend fun getOffices(): List<Office>

    /** All offices, including archived ones — for resolving names of past visits. */
    fun observeAllOffices(): Flow<List<Office>>

    /** All offices, including archived ones — for resolving names of past visits. */
    suspend fun getAllOffices(): List<Office>

    suspend fun getOffice(id: Long): Office?

    /** Number of active (non-archived) offices. */
    suspend fun count(): Int

    /** Inserts or updates the office and returns it with its persisted id. */
    suspend fun saveOffice(office: Office): Office

    /** Soft-deletes the office: it leaves the active lists but its attendance history is kept. */
    suspend fun archiveOffice(id: Long)
}
