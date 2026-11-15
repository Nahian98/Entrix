package com.apexbytestudio.entrix.domain.usecase

import com.apexbytestudio.entrix.core.model.Attendance
import com.apexbytestudio.entrix.core.model.Office
import com.apexbytestudio.entrix.domain.model.OfficeBreakdown
import com.apexbytestudio.entrix.domain.model.ReportBar
import com.apexbytestudio.entrix.domain.model.ReportPeriod
import com.apexbytestudio.entrix.domain.model.ReportSnapshot
import com.apexbytestudio.entrix.domain.repository.AttendanceRepository
import com.apexbytestudio.entrix.domain.repository.OfficeRepository
import com.apexbytestudio.entrix.domain.time.minuteTicks
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Aggregates attendance into a [ReportSnapshot] for the given [ReportPeriod] and [offset]
 * (0 = current period, -1 = previous, +1 = next). Reacts to attendance and office changes.
 *
 * A visit that is still open counts with its time so far, so the report agrees with Home and
 * History; while one exists the snapshot is rebuilt every minute. The [clock] is injected so
 * periods and "now" are deterministic in tests.
 */
class ObserveReportUseCase(
    private val attendanceRepository: AttendanceRepository,
    private val officeRepository: OfficeRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(period: ReportPeriod, offset: Int): Flow<ReportSnapshot> = combine(
        attendanceRepository.observeHistory(),
        officeRepository.observeAllOffices(),
    ) { history, offices -> history to offices }
        .flatMapLatest { (history, offices) ->
            val now = if (history.any { it.isOpen }) minuteTicks(clock) else flowOf(clock.instant())
            now.map { buildSnapshot(period, offset, history, offices, it) }
        }

    private fun buildSnapshot(
        period: ReportPeriod,
        offset: Int,
        history: List<Attendance>,
        offices: List<Office>,
        now: Instant,
    ): ReportSnapshot {
        val today = now.atZone(clock.zone).toLocalDate()
        val (start, end) = rangeFor(period, offset, today)
        val (prevStart, prevEnd) = rangeFor(period, offset - 1, today)
        val rangeLabel = labelFor(period, start, end)

        // Ignore anything that (by clock skew) starts after "now"; it has no time yet.
        val counted = history.filter { !it.enterTime.isAfter(now) }
        val inRange = counted.filter { it.enterDate() in start..end }
        val previousTotal = counted.filter { it.enterDate() in prevStart..prevEnd }.totalDuration(now)
        val hasVisitInProgress = inRange.any { it.isOpen }
        if (inRange.isEmpty()) {
            return ReportSnapshot.empty(period, start, end, rangeLabel).copy(
                previousTotal = previousTotal,
                bars = barsFor(period, start, end, emptyList(), now),
            )
        }

        val namesById = offices.associate { it.id to it.name }
        val daysPresent = inRange.map { it.enterDate() }.distinct().size
        val total = inRange.totalDuration(now)

        return ReportSnapshot(
            period = period,
            start = start,
            end = end,
            rangeLabel = rangeLabel,
            totalDuration = total,
            visitCount = inRange.size,
            daysPresent = daysPresent,
            averagePerActiveDay = if (daysPresent > 0) total.dividedBy(daysPresent.toLong()) else Duration.ZERO,
            previousTotal = previousTotal,
            bars = barsFor(period, start, end, inRange, now),
            offices = inRange.groupBy { it.officeId }
                .map { (officeId, visits) ->
                    OfficeBreakdown(
                        officeName = namesById[officeId] ?: "Office #$officeId",
                        duration = visits.totalDuration(now),
                        visitCount = visits.size,
                    )
                }
                .sortedByDescending { it.duration },
            hasVisitInProgress = hasVisitInProgress,
        )
    }

    // ---- range / bucketing helpers ----

    private fun rangeFor(period: ReportPeriod, offset: Int, today: LocalDate): Pair<LocalDate, LocalDate> =
        when (period) {
            ReportPeriod.Week -> {
                val start = today.with(DayOfWeek.MONDAY).plusWeeks(offset.toLong())
                start to start.plusDays(6)
            }
            ReportPeriod.Month -> {
                val start = today.withDayOfMonth(1).plusMonths(offset.toLong())
                start to start.plusMonths(1).minusDays(1)
            }
        }

    private fun barsFor(
        period: ReportPeriod,
        start: LocalDate,
        end: LocalDate,
        visits: List<Attendance>,
        now: Instant,
    ): List<ReportBar> = when (period) {
        ReportPeriod.Week -> (0..6).map { i ->
            val day = start.plusDays(i.toLong())
            ReportBar(
                label = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                duration = visits.filter { it.enterDate() == day }.totalDuration(now),
                start = day,
                end = day,
            )
        }
        ReportPeriod.Month ->
            // One bar per calendar week (Monday–Sunday) touching the month, trimmed to its edges,
            // so a month's weeks line up with the Week view.
            generateSequence(start.with(DayOfWeek.MONDAY)) { it.plusWeeks(1) }
                .takeWhile { !it.isAfter(end) }
                .map { monday ->
                    val from = maxOf(monday, start)
                    val to = minOf(monday.plusDays(6), end)
                    ReportBar(
                        label = if (from == to) "${from.dayOfMonth}" else "${from.dayOfMonth}–${to.dayOfMonth}",
                        duration = visits.filter { it.enterDate() in from..to }.totalDuration(now),
                        start = from,
                        end = to,
                    )
                }
                .toList()
    }

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

    /** Completed duration, or time so far (never negative) for an open visit. */
    private fun Attendance.timeSpent(now: Instant): Duration =
        duration ?: Duration.between(enterTime, now).takeIf { !it.isNegative } ?: Duration.ZERO

    private fun List<Attendance>.totalDuration(now: Instant): Duration =
        fold(Duration.ZERO) { acc, v -> acc + v.timeSpent(now) }

    private companion object {
        val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
        val MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    }
}
