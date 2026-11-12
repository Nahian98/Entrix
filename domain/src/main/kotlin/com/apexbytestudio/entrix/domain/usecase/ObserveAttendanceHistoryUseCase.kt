package com.apexbytestudio.entrix.domain.usecase

import com.apexbytestudio.entrix.domain.model.AttendanceRules
import com.apexbytestudio.entrix.domain.model.HistoryVisit
import com.apexbytestudio.entrix.domain.repository.AttendanceRepository
import com.apexbytestudio.entrix.domain.repository.OfficeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Full attendance history, newest first, with each visit's office name attached so the UI can show
 * which site a visit belongs to across multiple offices.
 */
class ObserveAttendanceHistoryUseCase(
    private val attendanceRepository: AttendanceRepository,
    private val officeRepository: OfficeRepository,
) {
    operator fun invoke(): Flow<List<HistoryVisit>> = combine(
        attendanceRepository.observeHistory(),
        officeRepository.observeAllOffices(),
    ) { history, offices ->
        val namesById = offices.associate { it.id to it.name }
        history.map {
            HistoryVisit(
                attendance = it,
                officeName = namesById[it.officeId],
                needsReview = AttendanceRules.needsReview(it),
            )
        }
    }
}
