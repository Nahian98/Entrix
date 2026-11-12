package com.apexbytestudio.entrix.core.location.di

import com.apexbytestudio.entrix.core.location.FusedLocationProvider
import com.apexbytestudio.entrix.core.location.GeocodingApiLocationSearcher
import com.apexbytestudio.entrix.core.location.geofence.PlayGeofenceScheduler
import com.apexbytestudio.entrix.domain.geofence.GeofenceScheduler
import com.apexbytestudio.entrix.domain.location.LocationProvider
import com.apexbytestudio.entrix.domain.location.LocationSearcher
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val locationModule = module {
    single<GeofenceScheduler> { PlayGeofenceScheduler(androidContext()) }
    single<LocationProvider> { FusedLocationProvider(androidContext()) }
    single<LocationSearcher> { GeocodingApiLocationSearcher(androidContext()) }
}
