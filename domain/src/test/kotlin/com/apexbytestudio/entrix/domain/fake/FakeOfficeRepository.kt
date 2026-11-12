package com.apexbytestudio.entrix.domain.fake

import com.apexbytestudio.entrix.core.model.Office
import com.apexbytestudio.entrix.domain.repository.OfficeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakeOfficeRepository : OfficeRepository {
    private val offices = MutableStateFlow<List<Office>>(emptyList())
    private var nextId = 1L

    override fun observeOffices(): Flow<List<Office>> = offices.asStateFlow()

    override fun observeOffice(id: Long): Flow<Office?> =
        offices.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getOffices(): List<Office> = offices.value

    override suspend fun getOffice(id: Long): Office? = offices.value.firstOrNull { it.id == id }

    override suspend fun count(): Int = offices.value.size

    override suspend fun saveOffice(office: Office): Office {
        val persisted = if (office.id == Office.NO_ID) office.copy(id = nextId++) else office
        offices.value = offices.value.filterNot { it.id == persisted.id } + persisted
        return persisted
    }

    override suspend fun deleteOffice(id: Long) {
        offices.value = offices.value.filterNot { it.id == id }
    }

    // Test helpers
    fun current(): List<Office> = offices.value
    fun seed(vararg office: Office) {
        offices.value = office.toList()
        nextId = (office.maxOfOrNull { it.id } ?: 0L) + 1L
    }
}
