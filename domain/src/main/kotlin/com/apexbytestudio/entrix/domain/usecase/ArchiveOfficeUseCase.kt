package com.apexbytestudio.entrix.domain.usecase

import com.apexbytestudio.entrix.domain.repository.OfficeRepository

/**
 * Archives an office (soft-delete) and re-syncs the geofences so the removed office stops being
 * tracked immediately. The office's recorded attendance is intentionally kept so history and
 * timesheets stay intact and correctly labelled.
 */
class ArchiveOfficeUseCase(
    private val officeRepository: OfficeRepository,
    private val syncGeofences: SyncGeofencesUseCase,
) {
    suspend operator fun invoke(officeId: Long) {
        officeRepository.archiveOffice(officeId)
        syncGeofences()
    }
}
