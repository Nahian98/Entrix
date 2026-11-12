package com.apexbytestudio.entrix.data.repository

import com.apexbytestudio.entrix.core.database.dao.OfficeDao
import com.apexbytestudio.entrix.core.model.Office
import com.apexbytestudio.entrix.data.mapper.toEntity
import com.apexbytestudio.entrix.data.mapper.toModel
import com.apexbytestudio.entrix.domain.repository.OfficeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfficeRepositoryImpl(
    private val officeDao: OfficeDao,
) : OfficeRepository {

    override fun observeOffices(): Flow<List<Office>> =
        officeDao.observeOffices().map { entities -> entities.map { it.toModel() } }

    override fun observeOffice(id: Long): Flow<Office?> =
        officeDao.observeOffice(id).map { it?.toModel() }

    override suspend fun getOffices(): List<Office> =
        officeDao.getOffices().map { it.toModel() }

    override suspend fun getOffice(id: Long): Office? = officeDao.getOffice(id)?.toModel()

    override suspend fun count(): Int = officeDao.countOffices()

    override suspend fun saveOffice(office: Office): Office {
        val newId = officeDao.upsert(office.toEntity())
        return if (office.id == Office.NO_ID) office.copy(id = newId) else office
    }

    override suspend fun deleteOffice(id: Long) = officeDao.deleteOffice(id)
}
