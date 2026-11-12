package com.apexbytestudio.entrix.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class EntrixDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EntrixDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2_keepsExistingData_andDefaultsArchivedToFalse() {
        // Seed the v1 schema: `office` has no `archived` column yet.
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO office (id, name, latitude, longitude, radiusMeters) " +
                    "VALUES (1, 'HQ', 52.52, 13.405, 150.0)",
            )
            execSQL(
                "INSERT INTO attendance (id, officeId, enterTime, exitTime, createdTime) " +
                    "VALUES (1, 1, 1000, 2000, 1000)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            2,
            /* validateDroppedTables = */ true,
            EntrixDatabase.MIGRATION_1_2,
        )

        // The office survived and the new column defaults to not-archived (0).
        db.query("SELECT archived FROM office WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        // Attendance survived — the migration must not drop or cascade it away.
        db.query("SELECT COUNT(*) FROM attendance").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        db.close()
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
