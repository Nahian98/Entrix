package com.apexbytestudio.entrix.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexbytestudio.entrix.core.designsystem.component.EntrixBarEntry
import com.apexbytestudio.entrix.domain.model.ReportPeriod
import com.apexbytestudio.entrix.domain.model.ReportSnapshot
import com.apexbytestudio.entrix.domain.usecase.ExportTimesheetUseCase
import com.apexbytestudio.entrix.domain.usecase.ObserveReportUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(
    observeReport: ObserveReportUseCase,
    private val exportTimesheet: ExportTimesheetUseCase,
) : ViewModel() {

    private data class Params(val period: ReportPeriod, val offset: Int)

    private val params = MutableStateFlow(Params(ReportPeriod.Week, 0))
    private val isExporting = MutableStateFlow(false)
    private var latest: ReportSnapshot? = null

    private val _events = Channel<ReportsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val uiState: StateFlow<ReportsUiState> = combine(
        params.flatMapLatest { (period, offset) -> observeReport(period, offset) },
        isExporting,
    ) { snapshot, exporting ->
        latest = snapshot
        snapshot.toUiState(exporting)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = ReportsUiState(isLoading = true),
    )

    fun selectPeriod(period: ReportPeriod) = params.update { Params(period, 0) }
    fun previousPeriod() = params.update { it.copy(offset = it.offset - 1) }
    fun nextPeriod() = params.update { it.copy(offset = it.offset + 1) }

    fun exportPdf() {
        val snapshot = latest?.takeIf { it.hasData } ?: return
        viewModelScope.launch {
            isExporting.value = true
            exportTimesheet(
                startDate = snapshot.start,
                endDate = snapshot.end,
                zone = ZoneId.systemDefault(),
                periodLabel = snapshot.rangeLabel,
            )
                .onSuccess { doc -> _events.send(ReportsEvent.Share(doc.absolutePath, doc.mimeType)) }
                .onFailure { _events.send(ReportsEvent.Error("Couldn't create the PDF.")) }
            isExporting.value = false
        }
    }
}

private fun ReportSnapshot.toUiState(exporting: Boolean) = ReportsUiState(
    isLoading = false,
    period = period,
    rangeLabel = rangeLabel,
    total = totalDuration.formatHoursMinutes(),
    visitCount = visitCount,
    daysPresent = daysPresent,
    averagePerDay = averagePerActiveDay.formatHoursMinutes(),
    trendPercent = trendPercent,
    hasData = hasData,
    bars = bars.map { EntrixBarEntry(label = it.label, value = it.duration.toMinutes().toFloat()) },
    offices = offices.map {
        OfficeBreakdownUi(
            name = it.officeName,
            duration = it.duration.formatHoursMinutes(),
            visitCount = it.visitCount,
        )
    },
    isExporting = exporting,
)

private fun Duration.formatHoursMinutes(): String {
    val hours = toHours()
    val minutes = toMinutes() % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
