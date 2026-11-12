package com.apexbytestudio.entrix.domain.usecase

import com.apexbytestudio.entrix.core.model.Attendance
import com.apexbytestudio.entrix.core.model.Office
import com.apexbytestudio.entrix.domain.model.OfficeBreakdown
import com.apexbytestudio.entrix.domain.model.ReportBar
import com.apexbytestudio.entrix.domain.model.ReportPeriod
import com.apexbytestudio.entrix.domain.model.ReportSnapshot
import com.apexbytestudio.entrix.domain.repository.AttendanceRepository
import com.apexbytestudio.entrix.domain.repository.OfficeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Aggregates completed attendance into a [ReportSnapshot] for the given [ReportPeriod] and [offset]
 * (0 = current period, -1 = previous, +1 = next). Reacts to attendance and office changes. The
 * [clock] is injected so periods are deterministic in tests.
 */
class ObserveReportUseCase(
    private val attendanceRepository: AttendanceRepository,
    private val officeRepository: OfficeRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    operator fun invoke(period: ReportPeriod, offset: Int): Flow<ReportSnapshot> = combine(
        attendanceRepository.observeHistory(),
        officeRepository.observeAllOffices(),
    ) { history, offices ->
        buildSnapshot(period, offset, history, offices)
    }

    private fun buildSnapshot(
        period: ReportPeriod,
        offset: Int,
        history: List<Attendance>,
        offices: List<Office>,
    ): ReportSnapshot {
        val (start, end) = rangeFor(period, offset)
        val (prevStart, prevEnd) = rangeFor(period, offset - 1)
        val rangeLabel = labelFor(period, start, end)

        val completed = history.filter { it.exitTime != null }
        val inRange = completed.filter { it.enterDate() in start..end }
        if (inRange.isEmpty()) {
            return ReportSnapshot.empty(period, start, end, rangeLabel).copy(
                previousTotal = completed.filter { it.enterDate() in prevStart..prevEnd }.totalDuration(),
                bars = emptyBars(period, start, end),
            )
        }

        val namesById = offices.associate { it.id to it.name }
        val daysPresent = inRange.map { it.enterDate() }.distinct().size
        val total = inRange.totalDuration()

        return ReportSnapshot(
            period = period,
            start = start,
            end = end,
            rangeLabel = rangeLabel,
            totalDuration = total,
            visitCount = inRange.size,
            daysPresent = daysPresent,
            averagePerActiveDay = if (daysPresent > 0) total.dividedBy(daysPresent.toLong()) else Duration.ZERO,
            previousTotal = completed.filter { it.enterDate() in prevStart..prevEnd }.totalDuration(),
            bars = barsFor(period, start, end, inRange),
            offices = inRange.groupBy { it.officeId }
                .map { (officeId, visits) ->
                    OfficeBreakdown(
                        officeName = namesById[officeId] ?: "Office #$officeId",
                        duration = visits.totalDuration(),
                        visitCount = visits.size,
                    )
                }
                .sortedByDescending { it.duration },
        )
    }

    // ---- range / bucketing helpers ----

    private fun rangeFor(period: ReportPeriod, offset: Int): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now(clock)
        return when (period) {
            ReportPeriod.Week -> {
                val start = today.with(DayOfWeek.MONDAY).plusWeeks(offset.toLong())
                start to start.plusDays(6)
            }
            ReportPeriod.Month -> {
                val start = today.withDayOfMonth(1).plusMonths(offset.toLong())
                start to start.plusMonths(1).minusDays(1)
            }
        }
    }

    private fun barsFor(
        period: ReportPeriod,
        start: LocalDate,
        end: LocalDate,
        visits: List<Attendance>,
    ): List<ReportBar> = when (period) {
        ReportPeriod.Week -> (0..6).map { i ->
            val day = start.plusDays(i.toLong())
            ReportBar(
                label = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                duration = visits.filter { it.enterDate() == day }.totalDuration(),
            )
        }
        ReportPeriod.Month -> {
            // One bar per calendar week chunk of the month (days 1–7, 8–14, …).
            val weeks = ((end.dayOfMonth - 1) / 7) + 1
            (0 until weeks).map { w ->
                val from = start.plusDays((w * 7).toLong())
                val to = minOf(from.plusDays(6), end)
                ReportBar(
                    label = "${from.dayOfMonth}–${to.dayOfMonth}",
                    duration = visits.filter { it.enterDate() in from..to }.totalDuration(),
                )
            }
        }
    }

    private fun emptyBars(period: ReportPeriod, start: LocalDate, end: LocalDate): List<ReportBar> =
        barsFor(period, start, end, emptyList())

    private fun labelFor(period: ReportPeriod, start: LocalDate, end: LocalDate): String =
        when (period) {
            ReportPeriod.Week ->
                if (start.month == end.month) {
                    "${start.dayOfMonth}–${end.format(DAY_MONTH)}"
                } else {
                    "${start.format(DAY_MONTH)} – ${end.format(DAY_MONTH)}"
                }
            ReportPeriod.Month -> start.format(MONTH_YEAR)
        }

    private fun Attendance.enterDate(): LocalDate = enterTime.atZone(clock.zone).toLocalDate()

    private fun List<Attendance>.totalDuration(): Duration =
        fold(Duration.ZERO) { acc, v -> acc + (v.duration ?: Duration.ZERO) }

    private companion object {
        val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
        val MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    }
}
