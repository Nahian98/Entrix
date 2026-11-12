package com.apexbytestudio.entrix.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.apexbytestudio.entrix.core.database.dao.AttendanceDao
import com.apexbytestudio.entrix.core.database.dao.OfficeDao
import com.apexbytestudio.entrix.core.database.entity.AttendanceEntity
import com.apexbytestudio.entrix.core.database.entity.OfficeEntity

@Database(
    entities = [OfficeEntity::class, AttendanceEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class EntrixDatabase : RoomDatabase() {
    abstract fun officeDao(): OfficeDao
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        const val NAME = "entrix.db"

        /**
         * v1 → v2: adds the `archived` flag so deleting an office soft-deletes it (keeping its
         * attendance history) instead of cascading the visits away. Additive column, default 0.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE office ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
