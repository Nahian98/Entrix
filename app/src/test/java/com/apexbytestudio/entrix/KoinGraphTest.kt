package com.apexbytestudio.entrix

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import com.apexbytestudio.entrix.core.common.di.coroutinesModule
import com.apexbytestudio.entrix.core.export.di.exportModule
import com.apexbytestudio.entrix.core.location.di.locationModule
import com.apexbytestudio.entrix.data.di.dataModule
import com.apexbytestudio.entrix.domain.di.domainModule
import com.apexbytestudio.entrix.feature.attendance.di.attendanceModule
import com.apexbytestudio.entrix.feature.dashboard.di.dashboardModule
import com.apexbytestudio.entrix.feature.office.di.officeModule
import com.apexbytestudio.entrix.feature.reports.di.reportsModule
import com.apexbytestudio.entrix.feature.settings.di.settingsModule
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify

/**
 * Statically verifies that the full application Koin graph can be satisfied — every declared
 * dependency has a definition. This catches missing/renamed bindings at build time (in CI) instead
 * of as a runtime crash on app launch. Android-provided types (Context/Application, injected via
 * `androidContext()`) are declared as external.
 */
class KoinGraphTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `application dependency graph is valid`() {
        module {
            includes(
                coroutinesModule,
                domainModule,
                dataModule,
                locationModule,
                exportModule,
                dashboardModule,
                attendanceModule,
                officeModule,
                reportsModule,
                settingsModule,
            )
        }.verify(
            extraTypes = listOf(
                Context::class,
                Application::class,
                // Created inline from androidContext() inside module lambdas, not as Koin definitions.
                DataStore::class,
            ),
        )
    }
}
