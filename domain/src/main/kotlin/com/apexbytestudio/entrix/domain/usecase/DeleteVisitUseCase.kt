package com.apexbytestudio.entrix.domain.usecase

import com.apexbytestudio.entrix.domain.repository.AttendanceRepository

/** Deletes a single attendance record — free basic correction of the user's own data. */
class DeleteVisitUseCase(
    private val attendanceRepository: AttendanceRepository,
) {
    suspend operator fun invoke(id: Long) = attendanceRepository.deleteVisit(id)
}
