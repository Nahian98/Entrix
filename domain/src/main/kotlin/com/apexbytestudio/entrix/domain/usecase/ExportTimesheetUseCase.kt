package com.apexbytestudio.entrix.domain.usecase

import com.apexbytestudio.entrix.domain.export.TimesheetExporter
import com.apexbytestudio.entrix.domain.model.ExportedDocument
import com.apexbytestudio.entrix.domain.model.Timesheet
import com.apexbytestudio.entrix.domain.model.TimesheetOffice
import com.apexbytestudio.entrix.domain.model.TimesheetRow
import com.apexbytestudio.entrix.domain.repository.AttendanceRepository
import com.apexbytestudio.entrix.domain.repository.OfficeRepository
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

/**
 * Gathers completed visits in a date range, groups them by office and day with per-office and grand
 * totals into a [Timesheet], then hands it to the [TimesheetExporter]. Only completed visits (with
 * an exit) count toward totals; an open visit has no known duration.
 */
class ExportTimesheetUseCase(
    private val officeRepository: OfficeRepository,
    private val attendanceRepository: AttendanceRepository,
    private val exporter: TimesheetExporter,
) {
    suspend operator fun invoke(
        startDate: LocalDate,
        endDate: LocalDate,
        zone: ZoneId,
        title: String = "Entrix Timesheet",
        periodLabel: String,
    ): Result<ExportedDocument> {
        val namesById = officeRepository.getOffices().associate { it.id to it.name }
        val inRange = attendanceRepository.getHistory()
            .filter { it.exitTime != null }
            .filter {
                val date = it.enterTime.atZone(zone).toLocalDate()
                !date.isBefore(startDate) && !date.isAfter(endDate)
            }

        val offices = inRange
            .groupBy { it.officeId }
            .map { (officeId, visits) ->
                val rows = visits
                    .sortedBy { it.enterTime }
                    .map {
                        TimesheetRow(
                            date = it.enterTime.atZone(zone).toLocalDate(),
                            enter = it.enterTime,
                            exit = it.exitTime,
                            duration = it.duration,
                        )
                    }
                TimesheetOffice(
                    officeName = namesById[officeId] ?: "Office #$officeId",
                    rows = rows,
                    total = rows.fold(Duration.ZERO) { acc, r -> acc + (r.duration ?: Duration.ZERO) },
                )
            }
            .sortedBy { it.officeName.lowercase() }

        val timesheet = Timesheet(
            title = title,
            periodLabel = periodLabel,
            offices = offices,
            grandTotal = offices.fold(Duration.ZERO) { acc, o -> acc + o.total },
        )

        return exporter.export(timesheet)
    }
}
