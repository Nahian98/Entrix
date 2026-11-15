package com.apexbytestudio.entrix.feature.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexbytestudio.entrix.core.common.time.minuteTicks
import com.apexbytestudio.entrix.domain.model.HistoryVisit
import com.apexbytestudio.entrix.domain.usecase.DeleteAttendanceHistoryUseCase
import com.apexbytestudio.entrix.domain.usecase.DeleteVisitUseCase
import com.apexbytestudio.entrix.domain.usecase.ObserveAttendanceHistoryUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AttendanceViewModel(
    observeHistory: ObserveAttendanceHistoryUseCase,
    private val deleteHistory: DeleteAttendanceHistoryUseCase,
    private val deleteVisit: DeleteVisitUseCase,
    private val clock: Clock,
) : ViewModel() {

    // An open visit's "time so far" (and its day's total) grows, so re-map once a minute while one
    // exists; otherwise map once. WhileSubscribed stops the ticker when the screen isn't visible.
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AttendanceUiState> = observeHistory()
        .flatMapLatest { history ->
            val hasOpenVisit = history.any { it.attendance.isOpen }
            val now = if (hasOpenVisit) minuteTicks(clock) else flowOf(clock.instant())
            now.map { history.toUiState(it, clock) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = AttendanceUiState(isLoading = true),
        )

    fun clearHistory() {
        viewModelScope.launch { deleteHistory() }
    }

    fun delete(id: Long) {
        viewModelScope.launch { deleteVisit(id) }
    }
}

private val sameYearDate = DateTimeFormatter.ofPattern("EEE, d MMM")
private val otherYearDate = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")
private val timePattern = DateTimeFormatter.ofPattern("HH:mm")

internal fun List<HistoryVisit>.toUiState(now: Instant, clock: Clock): AttendanceUiState {
    val zone = clock.zone
    val today = now.atZone(zone).toLocalDate()
    val time = timePattern.withZone(zone)

    val days = groupBy { it.attendance.enterTime.atZone(zone).toLocalDate() }
        .toSortedMap(reverseOrder())
        .map { (date, visits) ->
            val sorted = visits.sortedByDescending { it.attendance.enterTime }
            val total = sorted.fold(Duration.ZERO) { acc, visit -> acc + visit.timeSpent(now) }
            AttendanceDayUi(
                key = date.toString(),
                label = date.label(today),
                total = total.takeIf { !it.isZero }?.formatHoursMinutes(),
                visits = sorted.map { it.toVisitUi(now, time) },
            )
        }
    return AttendanceUiState(isLoading = false, days = days)
}

private fun LocalDate.label(today: LocalDate): String = when (this) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> format(if (year == today.year) sameYearDate else otherYearDate)
}

/** Completed duration, or time so far (never negative) for a still-open visit. */
private fun HistoryVisit.timeSpent(now: Instant): Duration =
    attendance.duration
        ?: Duration.between(attendance.enterTime, now).takeIf { !it.isNegative }
        ?: Duration.ZERO

private fun HistoryVisit.toVisitUi(now: Instant, time: DateTimeFormatter): AttendanceVisitUi {
    val start = time.format(attendance.enterTime)
    val end = attendance.exitTime?.let(time::format) ?: "now"
    return AttendanceVisitUi(
        id = attendance.id,
        officeName = officeName ?: "Unknown office",
        timeRange = "$start – $end",
        duration = timeSpent(now).formatHoursMinutes(),
        inProgress = attendance.isOpen,
        needsReview = needsReview,
    )
}

private fun Duration.formatHoursMinutes(): String {
    val hours = toHours()
    val minutes = toMinutes() % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
