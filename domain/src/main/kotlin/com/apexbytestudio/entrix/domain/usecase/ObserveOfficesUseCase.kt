package com.apexbytestudio.entrix.domain.usecase

import com.apexbytestudio.entrix.core.model.Office
import com.apexbytestudio.entrix.domain.repository.OfficeRepository
import kotlinx.coroutines.flow.Flow

/** Streams all configured offices (empty list when none), newest sort handled by the data layer. */
class ObserveOfficesUseCase(
    private val officeRepository: OfficeRepository,
) {
    operator fun invoke(): Flow<List<Office>> = officeRepository.observeOffices()
}
