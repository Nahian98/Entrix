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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(
    observeReport: ObserveReportUseCase,
    private val exportTimesheet: ExportTimesheetUseCase,
    private val clock: Clock,
) : ViewModel() {

    private data class Params(val period: ReportPeriod, val offset: Int)

    private val params = MutableStateFlow(Params(ReportPeriod.Week, 0))
    private val isExporting = MutableStateFlow(false)
    private var latest: ReportSnapshot? = null

    private val _events = Channel<ReportsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val uiState: StateFlow<ReportsUiState> = combine(
        params.flatMapLatest { p -> observeReport(p.period, p.offset).map { p.offset to it } },
        isExporting,
    ) { (offset, snapshot), exporting ->
        latest = snapshot
        snapshot.toUiState(offset, exporting, LocalDate.now(clock))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = ReportsUiState(isLoading = true),
    )

    fun selectPeriod(period: ReportPeriod) = params.update { Params(period, 0) }
    fun previousPeriod() = params.update { it.copy(offset = it.offset - 1) }

    /** No-op on the current period: future periods can't have attendance yet. */
    fun nextPeriod() = params.update { if (it.offset < 0) it.copy(offset = it.offset + 1) else it }

    fun exportPdf() {
        val snapshot = latest?.takeIf { it.hasData } ?: return
        if (isExporting.value) return
        viewModelScope.launch {
            isExporting.value = true
            exportTimesheet(
                startDate = snapshot.start,
                endDate = snapshot.end,
                zone = clock.zone,
                periodLabel = snapshot.rangeLabel,
            )
                .onSuccess { doc -> _events.send(ReportsEvent.Share(doc.absolutePath, doc.mimeType)) }
                .onFailure { _events.send(ReportsEvent.Error("Couldn't create the PDF.")) }
            isExporting.value = false
        }
    }
}

internal fun ReportSnapshot.toUiState(offset: Int, exporting: Boolean, today: LocalDate): ReportsUiState {
    val unit = if (period == ReportPeriod.Week) "week" else "month"
    val relativeTitle = when (offset) {
        0 -> "This $unit"
        -1 -> "Last $unit"
        else -> null
    }
    val totalMinutes = totalDuration.toMinutes()
    val withTime = bars.filter { !it.duration.isZero }
    val average = withTime.takeIf { it.isNotEmpty() }
        ?.let { list -> list.fold(Duration.ZERO) { acc, bar -> acc + bar.duration }.dividedBy(list.size.toLong()) }

    return ReportsUiState(
        isLoading = false,
        period = period,
        title = relativeTitle ?: rangeLabel,
        dateRange = rangeLabel.takeIf { relativeTitle != null },
        canGoNext = offset < 0,
        total = totalDuration.formatHoursMinutes(),
        trend = trendPercent?.let { ReportTrend(it, comparedTo = "vs previous $unit") },
        stats = listOf(
            ReportStat(if (daysPresent == 1) "Day at office" else "Days at office", daysPresent.toString()),
            ReportStat("Avg per day", averagePerActiveDay.formatHoursMinutes()),
            ReportStat(if (visitCount == 1) "Visit" else "Visits", visitCount.toString()),
        ),
        hasData = hasData,
        bars = bars.map {
            EntrixBarEntry(
                label = it.label,
                value = it.duration.toMinutes().toFloat(),
                valueLabel = if (it.duration.isZero) "No time" else it.duration.formatHoursMinutes(),
            )
        },
        currentBarIndex = currentBarIndex(today),
        barAverage = average?.toMinutes()?.toFloat(),
        barAverageLabel = average?.let { "avg ${it.formatHoursMinutes()}" },
        offices = offices.map {
            val share = if (totalMinutes > 0) it.duration.toMinutes().toFloat() / totalMinutes else 0f
            OfficeBreakdownUi(
                name = it.officeName,
                duration = it.duration.formatHoursMinutes(),
                visits = if (it.visitCount == 1) "1 visit" else "${it.visitCount} visits",
                share = share,
                sharePercent = "${(share * 100).roundToInt()}%",
            )
        },
        hasVisitInProgress = hasVisitInProgress,
        isExporting = exporting,
    )
}

/** Index of the bar containing [today]: a day in a week report, a 7-day chunk in a month report. */
private fun ReportSnapshot.currentBarIndex(today: LocalDate): Int? {
    if (today !in start..end) return null
    val index = when (period) {
        ReportPeriod.Week -> ChronoUnit.DAYS.between(start, today).toInt()
        ReportPeriod.Month -> (today.dayOfMonth - 1) / 7
    }
    return index.takeIf { it in bars.indices }
}

private fun Duration.formatHoursMinutes(): String {
    val hours = toHours()
    val minutes = toMinutes() % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
