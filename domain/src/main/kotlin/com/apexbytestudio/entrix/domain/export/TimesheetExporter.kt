package com.apexbytestudio.entrix.domain.export

import com.apexbytestudio.entrix.domain.model.ExportedDocument
import com.apexbytestudio.entrix.domain.model.Timesheet

/**
 * Renders a [Timesheet] to a shareable document. Defined in the domain so use cases can trigger an
 * export without depending on Android; the PDF implementation lives in `:core:export` (mirrors how
 * `:core:location` implements the geofence scheduler). Returns a failed [Result] on any I/O error.
 */
interface TimesheetExporter {
    suspend fun export(timesheet: Timesheet): Result<ExportedDocument>
}
