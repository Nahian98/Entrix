package com.apexbytestudio.entrix.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "office")
data class OfficeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    /**
     * Archived offices are hidden from the office list and stop being geofenced, but their row is
     * retained so attendance history and timesheets keep their real office name. Deleting an office
     * from the UI archives it; attendance is only ever hard-deleted via [OfficeDao.deleteAll].
     */
    val archived: Boolean = false,
)
