package com.apexbytestudio.entrix.domain.fake

import com.apexbytestudio.entrix.domain.repository.TrackingStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant

class FakeTrackingStateRepository(
    enabled: Boolean = true,
) : TrackingStateRepository {
    private val enabledFlow = MutableStateFlow(enabled)
    private val pending = mutableMapOf<Long, Instant>()

    override val trackingEnabled: Flow<Boolean> = enabledFlow

    override suspend fun isTrackingEnabled(): Boolean = enabledFlow.value

    override suspend fun setTrackingEnabled(enabled: Boolean) {
        enabledFlow.value = enabled
    }

    override suspend fun getPendingEnterTime(officeId: Long): Instant? = pending[officeId]

    override suspend fun setPendingEnterTime(officeId: Long, time: Instant?) {
        if (time == null) pending.remove(officeId) else pending[officeId] = time
    }
}
