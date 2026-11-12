package com.apexbytestudio.entrix.domain.model

import java.time.Duration
import java.time.LocalDate

/** The unit a report is bucketed and navigated by. */
enum class ReportPeriod { Week, Month }

/** One bar in the report chart (a day for Week reports, a week-of-month for Month reports). */
data class ReportBar(
    val label: String,
    val duration: Duration,
)

/** Time spent at one office within the report period. */
data class OfficeBreakdown(
    val officeName: String,
    val duration: Duration,
    val visitCount: Int,
)

/**
 * An aggregated attendance report for a single period. Totals count completed visits only. The
 * period is identified by its [start]/[end] dates; presentation formats them for display.
 */
data class ReportSnapshot(
    val period: ReportPeriod,
    val start: LocalDate,
    val end: LocalDate,
    val rangeLabel: String,
    val totalDuration: Duration,
    val visitCount: Int,
    val daysPresent: Int,
    val averagePerActiveDay: Duration,
    val previousTotal: Duration,
    val bars: List<ReportBar>,
    val offices: List<OfficeBreakdown>,
) {
    val hasData: Boolean get() = visitCount > 0

    /**
     * Percentage change vs the previous period, or null when there is no baseline (previous period
     * had no time) — a change from zero can't be expressed as a percentage.
     */
    val trendPercent: Int?
        get() {
            val prev = previousTotal.toMinutes()
            if (prev <= 0L) return null
            val now = totalDuration.toMinutes()
            return (((now - prev).toDouble() / prev) * 100).toInt()
        }

    companion object {
        fun empty(period: ReportPeriod, start: LocalDate, end: LocalDate, rangeLabel: String) =
            ReportSnapshot(
                period = period,
                start = start,
                end = end,
                rangeLabel = rangeLabel,
                totalDuration = Duration.ZERO,
                visitCount = 0,
                daysPresent = 0,
                averagePerActiveDay = Duration.ZERO,
                previousTotal = Duration.ZERO,
                bars = emptyList(),
                offices = emptyList(),
            )
    }
}
