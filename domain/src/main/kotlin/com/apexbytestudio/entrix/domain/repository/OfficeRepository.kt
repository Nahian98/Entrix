package com.apexbytestudio.entrix.domain.repository

import com.apexbytestudio.entrix.core.model.Office
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for the configured offices. The app can track any number of offices;
 * [observeOffices] emits the full list (empty when none is configured).
 */
interface OfficeRepository {
    fun observeOffices(): Flow<List<Office>>

    fun observeOffice(id: Long): Flow<Office?>

    suspend fun getOffices(): List<Office>

    suspend fun getOffice(id: Long): Office?

    suspend fun count(): Int

    /** Inserts or updates the office and returns it with its persisted id. */
    suspend fun saveOffice(office: Office): Office

    suspend fun deleteOffice(id: Long)
}
