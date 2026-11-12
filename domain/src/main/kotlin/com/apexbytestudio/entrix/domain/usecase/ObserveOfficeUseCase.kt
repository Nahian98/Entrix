package com.apexbytestudio.entrix.domain.usecase

import com.apexbytestudio.entrix.core.model.Office
import com.apexbytestudio.entrix.domain.repository.OfficeRepository
import kotlinx.coroutines.flow.Flow

/** Streams a single office by id — used by the add/edit form to load an existing office. */
class ObserveOfficeUseCase(
    private val officeRepository: OfficeRepository,
) {
    operator fun invoke(id: Long): Flow<Office?> = officeRepository.observeOffice(id)
}
