package com.apexbytestudio.entrix.feature.reports

import com.apexbytestudio.entrix.core.designsystem.component.EntrixBarEntry
import com.apexbytestudio.entrix.domain.model.ReportPeriod

data class ReportsUiState(
    val isLoading: Boolean = true,
    val period: ReportPeriod = ReportPeriod.Week,
    val rangeLabel: String = "",
    val total: String = "0m",
    val visitCount: Int = 0,
    val daysPresent: Int = 0,
    val averagePerDay: String = "0m",
    val trendPercent: Int? = null,
    val hasData: Boolean = false,
    val bars: List<EntrixBarEntry> = emptyList(),
    val offices: List<OfficeBreakdownUi> = emptyList(),
    val isExporting: Boolean = false,
) {
    val canExport: Boolean get() = hasData && !isExporting
}

data class OfficeBreakdownUi(
    val name: String,
    val duration: String,
    val visitCount: Int,
)

sealed interface ReportsEvent {
    data class Share(val path: String, val mimeType: String) : ReportsEvent
    data class Error(val message: String) : ReportsEvent
}
