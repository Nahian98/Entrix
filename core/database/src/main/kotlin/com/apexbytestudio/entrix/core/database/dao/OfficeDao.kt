package com.apexbytestudio.entrix.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.apexbytestudio.entrix.core.database.entity.OfficeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfficeDao {
    // "Active" queries exclude archived offices — these back the office list, the dashboard and
    // geofence registration.
    @Query("SELECT * FROM office WHERE archived = 0 ORDER BY name COLLATE NOCASE ASC")
    fun observeOffices(): Flow<List<OfficeEntity>>

    @Query("SELECT * FROM office WHERE id = :id")
    fun observeOffice(id: Long): Flow<OfficeEntity?>

    @Query("SELECT * FROM office WHERE archived = 0 ORDER BY name COLLATE NOCASE ASC")
    suspend fun getOffices(): List<OfficeEntity>

    // "All" queries include archived offices — used to resolve office names for history/reports
    // so a deleted office's past visits stay labelled.
    @Query("SELECT * FROM office ORDER BY name COLLATE NOCASE ASC")
    fun observeAllOffices(): Flow<List<OfficeEntity>>

    @Query("SELECT * FROM office ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllOffices(): List<OfficeEntity>

    @Query("SELECT * FROM office WHERE id = :id")
    suspend fun getOffice(id: Long): OfficeEntity?

    @Query("SELECT COUNT(*) FROM office WHERE archived = 0")
    suspend fun countOffices(): Int

    /** Inserts a new office or updates an existing one; returns the row id. */
    @Upsert
    suspend fun upsert(office: OfficeEntity): Long

    /** Soft-deletes an office: hides it from active queries while keeping its attendance history. */
    @Query("UPDATE office SET archived = 1 WHERE id = :id")
    suspend fun archiveOffice(id: Long)

    @Query("DELETE FROM office")
    suspend fun deleteAll()
}
