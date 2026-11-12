package com.apexbytestudio.entrix.domain.usecase

import com.apexbytestudio.entrix.core.model.Office
import com.apexbytestudio.entrix.domain.fake.FakeGeofenceScheduler
import com.apexbytestudio.entrix.domain.fake.FakeOfficeRepository
import com.apexbytestudio.entrix.domain.fake.FakeTrackingStateRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveOfficeUseCaseTest {

    private val officeRepo = FakeOfficeRepository()
    private val trackingRepo = FakeTrackingStateRepository(enabled = true)
    private val scheduler = FakeGeofenceScheduler()
    private val sync = SyncGeofencesUseCase(officeRepo, trackingRepo, scheduler)
    private val useCase = ArchiveOfficeUseCase(officeRepo, sync)

    private fun office(id: Long, name: String) =
        Office(id = id, name = name, latitude = 1.0, longitude = 2.0, radiusMeters = 150f)

    @Test
    fun `archiving removes the office from active lists but keeps it for history`() = runTest {
        officeRepo.seed(office(1, "HQ"), office(2, "Client"))

        useCase(1L)

        // Gone from the active list (office list, dashboard, geofencing)...
        assertEquals(listOf(2L), officeRepo.getOffices().map { it.id })
        // ...but still resolvable by history/reports so past visits stay labelled.
        assertEquals(setOf(1L, 2L), officeRepo.getAllOffices().map { it.id }.toSet())
        assertTrue(1L in officeRepo.archivedIds())
    }

    @Test
    fun `archiving re-syncs geofences to the remaining active offices`() = runTest {
        officeRepo.seed(office(1, "HQ"), office(2, "Client"))

        useCase(1L)

        assertEquals(listOf(2L), scheduler.registeredOffices.map { it.id })
        assertEquals(1, scheduler.registerCount)
    }

    @Test
    fun `archiving the last office clears all geofences`() = runTest {
        officeRepo.seed(office(1, "HQ"))

        useCase(1L)

        assertTrue(officeRepo.getOffices().isEmpty())
        assertTrue(scheduler.registeredOffices.isEmpty())
        assertEquals(1, scheduler.clearCount)
    }
}
