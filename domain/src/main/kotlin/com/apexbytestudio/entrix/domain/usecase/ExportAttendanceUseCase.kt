package com.apexbytestudio.entrix.domain.usecase

import com.apexbytestudio.entrix.domain.export.AttendanceExporter
import com.apexbytestudio.entrix.domain.model.AttendanceExportRow
import com.apexbytestudio.entrix.domain.model.AttendanceRules
import com.apexbytestudio.entrix.domain.model.ExportFormat
import com.apexbytestudio.entrix.domain.model.ExportedDocument
import com.apexbytestudio.entrix.domain.repository.AttendanceRepository
import com.apexbytestudio.entrix.domain.repository.OfficeRepository

/**
 * Gathers the user's full attendance history (raw, all records) with office names attached and hands
 * it to the [AttendanceExporter] as CSV or JSON. This is the **free** data-portability export; the
 * formatted PDF timesheet ([ExportTimesheetUseCase]) is the premium counterpart.
 */
class ExportAttendanceUseCase(
    private val officeRepository: OfficeRepository,
    private val attendanceRepository: AttendanceRepository,
    private val exporter: AttendanceExporter,
) {
    suspend operator fun invoke(format: ExportFormat): Result<ExportedDocument> {
        val namesById = officeRepository.getAllOffices().associate { it.id to it.name }
        val rows = attendanceRepository.getHistory().map { record ->
            AttendanceExportRow(
                officeId = record.officeId,
                officeName = namesById[record.officeId],
                enterTime = record.enterTime,
                exitTime = record.exitTime,
                durationMinutes = record.duration?.toMinutes(),
                needsReview = AttendanceRules.needsReview(record),
            )
        }
        return exporter.export(rows, format)
    }
}
