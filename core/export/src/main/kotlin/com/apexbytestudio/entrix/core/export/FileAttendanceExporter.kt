package com.apexbytestudio.entrix.core.export

import android.content.Context
import com.apexbytestudio.entrix.domain.export.AttendanceExporter
import com.apexbytestudio.entrix.domain.model.AttendanceExportRow
import com.apexbytestudio.entrix.domain.model.ExportFormat
import com.apexbytestudio.entrix.domain.model.ExportedDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.format.DateTimeFormatter

/**
 * [AttendanceExporter] that writes raw attendance to a CSV or JSON file in the app cache under
 * `exports/`, from where the UI shares it via a `FileProvider` (same as the PDF export). Timestamps
 * are ISO-8601 (UTC). No third-party dependency — org.json is part of the platform. Runs on
 * [Dispatchers.IO].
 */
class FileAttendanceExporter(
    private val context: Context,
) : AttendanceExporter {

    override suspend fun export(
        rows: List<AttendanceExportRow>,
        format: ExportFormat,
    ): Result<ExportedDocument> = withContext(Dispatchers.IO) {
        runCatching {
            val content = when (format) {
                ExportFormat.Csv -> toCsv(rows)
                ExportFormat.Json -> toJson(rows)
            }
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val fileName = "entrix-attendance.${format.extension}"
            val file = File(dir, fileName)
            file.writeText(content)

            ExportedDocument(
                absolutePath = file.absolutePath,
                fileName = fileName,
                mimeType = format.mimeType,
            )
        }
    }

    private fun toCsv(rows: List<AttendanceExportRow>): String = buildString {
        append("officeId,officeName,enterTime,exitTime,durationMinutes\n")
        rows.forEach { row ->
            append(row.officeId).append(',')
            append(csvEscape(row.officeName.orEmpty())).append(',')
            append(ISO.format(row.enterTime)).append(',')
            append(row.exitTime?.let(ISO::format).orEmpty()).append(',')
            append(row.durationMinutes?.toString().orEmpty()).append('\n')
        }
    }

    private fun toJson(rows: List<AttendanceExportRow>): String {
        val array = JSONArray()
        rows.forEach { row ->
            array.put(
                JSONObject()
                    .put("officeId", row.officeId)
                    .put("officeName", row.officeName ?: JSONObject.NULL)
                    .put("enterTime", ISO.format(row.enterTime))
                    .put("exitTime", row.exitTime?.let(ISO::format) ?: JSONObject.NULL)
                    .put("durationMinutes", row.durationMinutes ?: JSONObject.NULL),
            )
        }
        return array.toString(2)
    }

    /** Wraps a field in quotes (doubling any inner quotes) when it contains a CSV-special char. */
    private fun csvEscape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    private companion object {
        val ISO: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT
    }
}
