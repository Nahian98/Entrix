package com.apexbytestudio.entrix.feature.reports

import com.apexbytestudio.entrix.core.designsystem.component.EntrixBarEntry
import com.apexbytestudio.entrix.domain.model.ReportPeriod

data class ReportsUiState(
    val isLoading: Boolean = true,
    val period: ReportPeriod = ReportPeriod.Week,
    /** "This week", "Last month", or the plain range for older periods. */
    val title: String = "",
    /** The dates behind [title], e.g. "9–15 Nov"; null when the title already is the range. */
    val dateRange: String? = null,
    /** False on the current period: there is nothing to report in the future. */
    val canGoNext: Boolean = false,
    val total: String = "0m",
    val trend: ReportTrend? = null,
    val stats: List<ReportStat> = emptyList(),
    val hasData: Boolean = false,
    val bars: List<EntrixBarEntry> = emptyList(),
    /** Bar for "today" (or this week, in a month) when it falls inside the period. */
    val currentBarIndex: Int? = null,
    /** Average across bars that have time, in the same unit as the bar values (minutes). */
    val barAverage: Float? = null,
    val barAverageLabel: String? = null,
    val offices: List<OfficeBreakdownUi> = emptyList(),
    /** An open visit in this period isn't in the totals yet. */
    val hasVisitInProgress: Boolean = false,
    val isExporting: Boolean = false,
) {
    val canExport: Boolean get() = hasData && !isExporting
}

/** Change vs the previous period of the same kind. */
data class ReportTrend(
    val percent: Int,
    /** e.g. "vs previous week". */
    val comparedTo: String,
) {
    val isUp: Boolean get() = percent >= 0
}

data class ReportStat(val label: String, val value: String)

data class OfficeBreakdownUi(
    val name: String,
    val duration: String,
    val visits: String,
    /** Share of the period's total, 0..1. */
    val share: Float,
    val sharePercent: String,
)

sealed interface ReportsEvent {
    data class Share(val path: String, val mimeType: String) : ReportsEvent
    data class Error(val message: String) : ReportsEvent
}
