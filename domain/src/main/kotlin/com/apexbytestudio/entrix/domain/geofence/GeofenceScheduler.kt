package com.apexbytestudio.entrix.domain.geofence

import com.apexbytestudio.entrix.core.model.Office

/**
 * Abstraction over the platform geofencing engine. Defined in the domain so use cases can drive
 * registration without depending on Android / Play Services; the implementation lives in
 * `:core:location`. Implementations must be idempotent — [registerAll] replaces any prior geofences.
 */
interface GeofenceScheduler {
    suspend fun registerAll(offices: List<Office>): Result<Unit>

    suspend fun clear(): Result<Unit>
}
