package com.apexbytestudio.entrix.core.export

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.apexbytestudio.entrix.domain.export.TimesheetExporter
import com.apexbytestudio.entrix.domain.model.ExportedDocument
import com.apexbytestudio.entrix.domain.model.Timesheet
import com.apexbytestudio.entrix.domain.model.TimesheetOffice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * [TimesheetExporter] that renders to a PDF using the framework [PdfDocument] — no third-party
 * dependency, works fully offline. The file is written to the app cache under `exports/`, from
 * where the UI shares it via a `FileProvider`. Rendering runs on [Dispatchers.IO].
 */
class PdfTimesheetExporter(
    private val context: Context,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : TimesheetExporter {

    override suspend fun export(timesheet: Timesheet): Result<ExportedDocument> =
        withContext(Dispatchers.IO) {
            runCatching {
                val document = PdfDocument()
                Renderer(document).render(timesheet)

                val dir = File(context.cacheDir, "exports").apply { mkdirs() }
                val fileName = fileNameFor(timesheet)
                val file = File(dir, fileName)
                FileOutputStream(file).use { document.writeTo(it) }
                document.close()

                ExportedDocument(
                    absolutePath = file.absolutePath,
                    fileName = fileName,
                    mimeType = MIME_PDF,
                )
            }
        }

    private fun fileNameFor(timesheet: Timesheet): String {
        val slug = timesheet.periodLabel
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifEmpty { "timesheet" }
        return "entrix-timesheet-$slug.pdf"
    }

    /** Draws a timesheet across as many pages as needed, tracking a vertical cursor. */
    private inner class Renderer(private val document: PdfDocument) {
        private var pageNumber = 1
        private var page: PdfDocument.Page = newPage()
        private var y = MARGIN

        private val titlePaint = paint(20f, bold = true)
        private val subtitlePaint = paint(12f, color = COLOR_MUTED)
        private val officePaint = paint(14f, bold = true)
        private val bodyPaint = paint(11f)
        private val bodyMutedPaint = paint(11f, color = COLOR_MUTED)
        private val totalPaint = paint(12f, bold = true)

        fun render(timesheet: Timesheet) {
            drawText(timesheet.title, MARGIN, titlePaint)
            y += LINE
            drawText(timesheet.periodLabel, MARGIN, subtitlePaint)
            y += LINE * 1.5f

            if (timesheet.isEmpty) {
                drawText("No completed visits in this period.", MARGIN, bodyMutedPaint)
                finish()
                return
            }

            timesheet.offices.forEach { office -> drawOffice(office) }

            y += LINE * 0.5f
            drawText("Grand total", MARGIN, totalPaint)
            drawTextRight(formatDuration(timesheet.grandTotal), totalPaint)
            finish()
        }

        private fun drawOffice(office: TimesheetOffice) {
            ensureSpace(LINE * 3)
            y += LINE * 0.5f
            drawText(office.officeName, MARGIN, officePaint)
            y += LINE
            // Column header
            drawText("Date", COL_DATE, bodyMutedPaint)
            drawText("In", COL_IN, bodyMutedPaint)
            drawText("Out", COL_OUT, bodyMutedPaint)
            drawTextRight("Hours", bodyMutedPaint)
            y += LINE

            office.rows.forEach { row ->
                ensureSpace(LINE)
                drawText(DATE_FMT.format(row.date), COL_DATE, bodyPaint)
                drawText(row.enter.let(TIME_FMT::format), COL_IN, bodyPaint)
                drawText(row.exit?.let(TIME_FMT::format) ?: "—", COL_OUT, bodyPaint)
                drawTextRight(row.duration?.let(::formatDuration) ?: "—", bodyPaint)
                y += LINE
            }

            drawText("Subtotal", COL_DATE, totalPaint)
            drawTextRight(formatDuration(office.total), totalPaint)
            y += LINE
        }

        // ---- low-level drawing ----

        private fun newPage(): PdfDocument.Page {
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            return document.startPage(info)
        }

        private fun ensureSpace(needed: Float) {
            if (y + needed <= PAGE_HEIGHT - MARGIN) return
            document.finishPage(page)
            pageNumber++
            page = newPage()
            y = MARGIN
        }

        private fun drawText(text: String, x: Float, paint: Paint) {
            page.canvas.drawText(text, x, y, paint)
        }

        private fun drawTextRight(text: String, paint: Paint) {
            val x = PAGE_WIDTH - MARGIN - paint.measureText(text)
            page.canvas.drawText(text, x, y, paint)
        }

        private fun finish() {
            document.finishPage(page)
        }
    }

    private fun paint(size: Float, bold: Boolean = false, color: Int = COLOR_TEXT) = Paint().apply {
        isAntiAlias = true
        textSize = size
        this.color = color
        typeface = if (bold) {
            android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        } else {
            android.graphics.Typeface.DEFAULT
        }
    }

    private fun formatDuration(duration: Duration): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    private companion object {
        const val MIME_PDF = "application/pdf"

        // A4 at 72 dpi (points).
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 40f
        const val LINE = 18f

        const val COL_DATE = MARGIN
        const val COL_IN = 200f
        const val COL_OUT = 280f

        const val COLOR_TEXT = 0xFF202124.toInt()
        const val COLOR_MUTED = 0xFF5F6368.toInt()

        val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM")
    }

    private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(zone)
}
