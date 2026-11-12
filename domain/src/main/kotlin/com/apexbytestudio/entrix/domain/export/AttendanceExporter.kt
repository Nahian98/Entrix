package com.apexbytestudio.entrix.domain.export

import com.apexbytestudio.entrix.domain.model.AttendanceExportRow
import com.apexbytestudio.entrix.domain.model.ExportFormat
import com.apexbytestudio.entrix.domain.model.ExportedDocument

/**
 * Serializes raw attendance rows to a shareable CSV/JSON document. Defined in the domain so the
 * export use case stays Android-free; the file implementation lives in `:core:export` (mirrors
 * [TimesheetExporter]). Returns a failed [Result] on any I/O error.
 */
interface AttendanceExporter {
    suspend fun export(rows: List<AttendanceExportRow>, format: ExportFormat): Result<ExportedDocument>
}
