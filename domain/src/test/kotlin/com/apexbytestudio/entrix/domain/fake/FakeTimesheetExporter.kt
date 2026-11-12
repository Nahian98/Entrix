package com.apexbytestudio.entrix.domain.fake

import com.apexbytestudio.entrix.domain.export.TimesheetExporter
import com.apexbytestudio.entrix.domain.model.ExportedDocument
import com.apexbytestudio.entrix.domain.model.Timesheet

/** Captures the [Timesheet] it is handed so tests can assert on the computed content. */
class FakeTimesheetExporter : TimesheetExporter {
    var lastTimesheet: Timesheet? = null
        private set

    override suspend fun export(timesheet: Timesheet): Result<ExportedDocument> {
        lastTimesheet = timesheet
        return Result.success(
            ExportedDocument(
                absolutePath = "/tmp/${timesheet.periodLabel}.pdf",
                fileName = "timesheet.pdf",
                mimeType = "application/pdf",
            ),
        )
    }
}
