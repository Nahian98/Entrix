package com.apexbytestudio.entrix.domain.fake

import com.apexbytestudio.entrix.domain.export.AttendanceExporter
import com.apexbytestudio.entrix.domain.model.AttendanceExportRow
import com.apexbytestudio.entrix.domain.model.ExportFormat
import com.apexbytestudio.entrix.domain.model.ExportedDocument

/** Captures the rows/format it is handed so tests can assert on what the use case produced. */
class FakeAttendanceExporter : AttendanceExporter {
    var lastRows: List<AttendanceExportRow>? = null
        private set
    var lastFormat: ExportFormat? = null
        private set

    override suspend fun export(
        rows: List<AttendanceExportRow>,
        format: ExportFormat,
    ): Result<ExportedDocument> {
        lastRows = rows
        lastFormat = format
        return Result.success(
            ExportedDocument(
                absolutePath = "/tmp/export.${format.extension}",
                fileName = "export.${format.extension}",
                mimeType = format.mimeType,
            ),
        )
    }
}
