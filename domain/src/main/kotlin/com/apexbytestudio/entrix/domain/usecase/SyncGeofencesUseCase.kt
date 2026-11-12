package com.apexbytestudio.entrix.domain.usecase

import com.apexbytestudio.entrix.domain.geofence.GeofenceScheduler
import com.apexbytestudio.entrix.domain.repository.OfficeRepository
import com.apexbytestudio.entrix.domain.repository.TrackingStateRepository

/**
 * Reconciles the registered geofences with the desired state. A single idempotent entry point used
 * after enabling/disabling tracking, after offices change, and on device boot — so the geofences
 * can never drift from "tracking enabled AND at least one office is configured".
 */
class SyncGeofencesUseCase(
    private val officeRepository: OfficeRepository,
    private val trackingStateRepository: TrackingStateRepository,
    private val geofenceScheduler: GeofenceScheduler,
) {
    suspend operator fun invoke(): Result<Unit> {
        val enabled = trackingStateRepository.isTrackingEnabled()
        val offices = officeRepository.getOffices()

        return if (enabled && offices.isNotEmpty()) {
            geofenceScheduler.registerAll(offices)
        } else {
            geofenceScheduler.clear()
        }
    }
}
