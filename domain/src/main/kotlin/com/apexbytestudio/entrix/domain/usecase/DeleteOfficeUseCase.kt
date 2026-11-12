package com.apexbytestudio.entrix.domain.usecase

import com.apexbytestudio.entrix.domain.repository.OfficeRepository

/**
 * Deletes an office (its attendance rows cascade in the DB) and re-syncs the geofences so the
 * removed office stops being tracked immediately.
 */
class DeleteOfficeUseCase(
    private val officeRepository: OfficeRepository,
    private val syncGeofences: SyncGeofencesUseCase,
) {
    suspend operator fun invoke(officeId: Long) {
        officeRepository.deleteOffice(officeId)
        syncGeofences()
    }
}
