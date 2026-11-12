package com.apexbytestudio.entrix.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.apexbytestudio.entrix.core.database.entity.OfficeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfficeDao {
    @Query("SELECT * FROM office ORDER BY name COLLATE NOCASE ASC")
    fun observeOffices(): Flow<List<OfficeEntity>>

    @Query("SELECT * FROM office WHERE id = :id")
    fun observeOffice(id: Long): Flow<OfficeEntity?>

    @Query("SELECT * FROM office ORDER BY name COLLATE NOCASE ASC")
    suspend fun getOffices(): List<OfficeEntity>

    @Query("SELECT * FROM office WHERE id = :id")
    suspend fun getOffice(id: Long): OfficeEntity?

    @Query("SELECT COUNT(*) FROM office")
    suspend fun countOffices(): Int

    /** Inserts a new office or updates an existing one; returns the row id. */
    @Upsert
    suspend fun upsert(office: OfficeEntity): Long

    @Query("DELETE FROM office WHERE id = :id")
    suspend fun deleteOffice(id: Long)

    @Query("DELETE FROM office")
    suspend fun deleteAll()
}
