package com.apexbytestudio.entrix.domain.di

import com.apexbytestudio.entrix.domain.usecase.ArchiveOfficeUseCase
import com.apexbytestudio.entrix.domain.usecase.DeleteAttendanceHistoryUseCase
import com.apexbytestudio.entrix.domain.usecase.DeleteVisitUseCase
import com.apexbytestudio.entrix.domain.usecase.ExportAttendanceUseCase
import com.apexbytestudio.entrix.domain.usecase.ExportTimesheetUseCase
import com.apexbytestudio.entrix.domain.usecase.GetCurrentLocationUseCase
import com.apexbytestudio.entrix.domain.usecase.ObserveAttendanceHistoryUseCase
import com.apexbytestudio.entrix.domain.usecase.ObserveDashboardUseCase
import com.apexbytestudio.entrix.domain.usecase.ObserveOfficeUseCase
import com.apexbytestudio.entrix.domain.usecase.ObserveOfficesUseCase
import com.apexbytestudio.entrix.domain.usecase.ObserveReportUseCase
import com.apexbytestudio.entrix.domain.usecase.ObserveTrackingEnabledUseCase
import com.apexbytestudio.entrix.domain.usecase.ReconcileOpenVisitsUseCase
import com.apexbytestudio.entrix.domain.usecase.RecordGeofenceTransitionUseCase
import com.apexbytestudio.entrix.domain.usecase.SaveOfficeUseCase
import com.apexbytestudio.entrix.domain.usecase.SearchLocationUseCase
import com.apexbytestudio.entrix.domain.usecase.SetTrackingEnabledUseCase
import com.apexbytestudio.entrix.domain.usecase.SyncGeofencesUseCase
import org.koin.dsl.module
import java.time.Clock

/** Use cases are stateless; declaring them as `factory` keeps them cheap and side-effect free. */
val domainModule = module {
    // Factory, not single: Clock.systemDefaultZone() captures the zone, so a fresh one picks up a
    // device time-zone change.
    factory<Clock> { Clock.systemDefaultZone() }
    factory { SaveOfficeUseCase(get()) }
    factory { ObserveOfficeUseCase(get()) }
    factory { ObserveOfficesUseCase(get()) }
    factory { ArchiveOfficeUseCase(get(), get()) }
    factory { ObserveAttendanceHistoryUseCase(get(), get()) }
    factory { DeleteAttendanceHistoryUseCase(get()) }
    factory { DeleteVisitUseCase(get()) }
    factory { ReconcileOpenVisitsUseCase(get()) }
    factory { SyncGeofencesUseCase(get(), get(), get()) }
    factory { SetTrackingEnabledUseCase(get(), get()) }
    factory { ObserveDashboardUseCase(get(), get(), get()) }
    factory { RecordGeofenceTransitionUseCase(get(), get()) }
    factory { GetCurrentLocationUseCase(get()) }
    factory { SearchLocationUseCase(get()) }
    factory { ObserveTrackingEnabledUseCase(get()) }
    factory { ExportTimesheetUseCase(get(), get(), get()) }
    factory { ExportAttendanceUseCase(get(), get(), get()) }
    factory { ObserveReportUseCase(get(), get()) }
}
