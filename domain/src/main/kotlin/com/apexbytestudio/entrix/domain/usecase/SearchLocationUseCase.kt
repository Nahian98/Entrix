package com.apexbytestudio.entrix.domain.usecase

import com.apexbytestudio.entrix.core.model.Coordinates
import com.apexbytestudio.entrix.domain.location.LocationSearcher

/** Resolves a place/address query to coordinates for the office map picker. */
class SearchLocationUseCase(
    private val locationSearcher: LocationSearcher,
) {
    suspend operator fun invoke(query: String): Result<Coordinates> = locationSearcher.search(query)
}
