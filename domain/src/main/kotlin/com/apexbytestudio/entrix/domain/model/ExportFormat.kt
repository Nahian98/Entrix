package com.apexbytestudio.entrix.domain.model

/** Raw data-export formats offered for free (data portability). */
enum class ExportFormat(val extension: String, val mimeType: String) {
    Csv("csv", "text/csv"),
    Json("json", "application/json"),
}
