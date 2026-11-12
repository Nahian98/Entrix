package com.apexbytestudio.entrix.domain.usecase

import com.apexbytestudio.entrix.core.model.Office
import com.apexbytestudio.entrix.domain.fake.FakeGeofenceScheduler
import com.apexbytestudio.entrix.domain.fake.FakeOfficeRepository
import com.apexbytestudio.entrix.domain.fake.FakeTrackingStateRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncGeofencesUseCaseTest {

    private val officeRepo = FakeOfficeRepository()
    private val trackingRepo = FakeTrackingStateRepository(enabled = false)
    private val scheduler = FakeGeofenceScheduler()
    private val sync = SyncGeofencesUseCase(officeRepo, trackingRepo, scheduler)

    private suspend fun configureOffice(name: String = "HQ") = officeRepo.saveOffice(
        Office(name = name, latitude = 52.52, longitude = 13.405, radiusMeters = 150f),
    )

    @Test
    fun `registers when tracking is enabled and an office exists`() = runTest {
        val office = configureOffice()
        trackingRepo.setTrackingEnabled(true)

        val result = sync()

        assertEquals(Result.success(Unit), result)
        assertEquals(listOf(office), scheduler.registeredOffices)
        assertEquals(1, scheduler.registerCount)
    }

    @Test
    fun `registers every configured office`() = runTest {
        configureOffice("HQ")
        configureOffice("Client site")
        trackingRepo.setTrackingEnabled(true)

        sync()

        assertEquals(2, scheduler.registeredOffices.size)
        assertEquals(1, scheduler.registerCount)
    }

    @Test
    fun `clears when tracking is disabled`() = runTest {
        configureOffice()
        trackingRepo.setTrackingEnabled(false)

        sync()

        assertTrue(scheduler.registeredOffices.isEmpty())
        assertEquals(1, scheduler.clearCount)
        assertEquals(0, scheduler.registerCount)
    }

    @Test
    fun `clears when enabled but no office is configured`() = runTest {
        trackingRepo.setTrackingEnabled(true)

        sync()

        assertEquals(0, scheduler.registerCount)
        assertEquals(1, scheduler.clearCount)
    }

    @Test
    fun `set tracking enabled persists the flag and triggers registration`() = runTest {
        val office = configureOffice()
        val setTracking = SetTrackingEnabledUseCase(trackingRepo, sync)

        val result = setTracking(true)

        assertEquals(Result.success(Unit), result)
        assertEquals(true, trackingRepo.isTrackingEnabled())
        assertEquals(listOf(office), scheduler.registeredOffices)
    }
}
