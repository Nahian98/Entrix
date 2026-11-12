package com.apexbytestudio.entrix.domain.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * A fully-computed timesheet ready to be rendered to a document. All grouping and totalling is done
 * in the domain ([com.apexbytestudio.entrix.domain.usecase.ExportTimesheetUseCase]) so the exporter
 * only lays out data it is handed — no business logic in the rendering layer.
 */
data class Timesheet(
    val title: String,
    val periodLabel: String,
    val offices: List<TimesheetOffice>,
    val grandTotal: Duration,
) {
    val isEmpty: Boolean get() = offices.isEmpty()
}

data class TimesheetOffice(
    val officeName: String,
    val rows: List<TimesheetRow>,
    val total: Duration,
)

data class TimesheetRow(
    val date: LocalDate,
    val enter: Instant,
    val exit: Instant?,
    val duration: Duration?,
)

/** A document produced by a [com.apexbytestudio.entrix.domain.export.TimesheetExporter]. */
data class ExportedDocument(
    val absolutePath: String,
    val fileName: String,
    val mimeType: String,
)
