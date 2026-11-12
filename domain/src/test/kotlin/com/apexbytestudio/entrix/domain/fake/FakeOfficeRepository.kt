package com.apexbytestudio.entrix.domain.fake

import com.apexbytestudio.entrix.core.model.Office
import com.apexbytestudio.entrix.domain.repository.OfficeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class FakeOfficeRepository : OfficeRepository {
    private val allOffices = MutableStateFlow<List<Office>>(emptyList())
    private val archivedIds = MutableStateFlow<Set<Long>>(emptySet())
    private var nextId = 1L

    private fun active(list: List<Office>, archived: Set<Long>) = list.filterNot { it.id in archived }

    override fun observeOffices(): Flow<List<Office>> =
        combine(allOffices, archivedIds) { list, archived -> active(list, archived) }

    override fun observeOffice(id: Long): Flow<Office?> =
        allOffices.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getOffices(): List<Office> = active(allOffices.value, archivedIds.value)

    override fun observeAllOffices(): Flow<List<Office>> = allOffices.asStateFlow()

    override suspend fun getAllOffices(): List<Office> = allOffices.value

    override suspend fun getOffice(id: Long): Office? = allOffices.value.firstOrNull { it.id == id }

    override suspend fun count(): Int = active(allOffices.value, archivedIds.value).size

    override suspend fun saveOffice(office: Office): Office {
        val persisted = if (office.id == Office.NO_ID) office.copy(id = nextId++) else office
        allOffices.value = allOffices.value.filterNot { it.id == persisted.id } + persisted
        return persisted
    }

    override suspend fun archiveOffice(id: Long) {
        archivedIds.value = archivedIds.value + id
    }

    // Test helpers
    fun current(): List<Office> = active(allOffices.value, archivedIds.value)
    fun archivedIds(): Set<Long> = archivedIds.value
    fun seed(vararg office: Office) {
        allOffices.value = office.toList()
        nextId = (office.maxOfOrNull { it.id } ?: 0L) + 1L
    }
}
