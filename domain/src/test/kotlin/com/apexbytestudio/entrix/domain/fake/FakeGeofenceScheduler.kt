package com.apexbytestudio.entrix.domain.fake

import com.apexbytestudio.entrix.core.model.Office
import com.apexbytestudio.entrix.domain.geofence.GeofenceScheduler

class FakeGeofenceScheduler(
    private val registerResult: Result<Unit> = Result.success(Unit),
) : GeofenceScheduler {
    var registeredOffices: List<Office> = emptyList()
        private set
    var clearCount = 0
        private set
    var registerCount = 0
        private set

    override suspend fun registerAll(offices: List<Office>): Result<Unit> {
        registerCount++
        registeredOffices = offices
        return registerResult
    }

    override suspend fun clear(): Result<Unit> {
        clearCount++
        registeredOffices = emptyList()
        return Result.success(Unit)
    }
}
