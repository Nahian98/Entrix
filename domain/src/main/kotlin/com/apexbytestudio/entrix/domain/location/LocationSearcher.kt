package com.apexbytestudio.entrix.domain.location

import com.apexbytestudio.entrix.core.model.Coordinates

/**
 * Resolves a free-text place/address query to [Coordinates], abstracted from Android so the office
 * picker can offer location search without the domain depending on a geocoder. Implementations
 * return a failed [Result] when nothing matches or the lookup fails (e.g. offline).
 */
interface LocationSearcher {
    suspend fun search(query: String): Result<Coordinates>
}
