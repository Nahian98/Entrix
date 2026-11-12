package com.apexbytestudio.entrix.feature.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexbytestudio.entrix.domain.model.HistoryVisit
import com.apexbytestudio.entrix.domain.usecase.DeleteAttendanceHistoryUseCase
import com.apexbytestudio.entrix.domain.usecase.DeleteVisitUseCase
import com.apexbytestudio.entrix.domain.usecase.ObserveAttendanceHistoryUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AttendanceViewModel(
    observeHistory: ObserveAttendanceHistoryUseCase,
    private val deleteHistory: DeleteAttendanceHistoryUseCase,
    private val deleteVisit: DeleteVisitUseCase,
) : ViewModel() {

    val uiState: StateFlow<AttendanceUiState> = observeHistory()
        .map { it.toUiState() }
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

private val zone: ZoneId = ZoneId.systemDefault()
private val dateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(zone)

private fun List<HistoryVisit>.toUiState(): AttendanceUiState {
    val days = groupBy { it.attendance.enterTime.atZone(zone).toLocalDate() }
        .toSortedMap(reverseOrder())
        .map { (date, visits) ->
            AttendanceDayUi(
                date = date.format(dateFormatter),
                visits = visits.sortedByDescending { it.attendance.enterTime }.map { it.toVisitUi() },
            )
        }
    return AttendanceUiState(isLoading = false, days = days)
}

private fun HistoryVisit.toVisitUi(): AttendanceVisitUi = AttendanceVisitUi(
    id = attendance.id,
    officeName = officeName ?: "Unknown office",
    entry = timeFormatter.format(attendance.enterTime),
    exit = attendance.exitTime?.let(timeFormatter::format) ?: "In progress",
    duration = attendance.duration?.formatHoursMinutes() ?: "—",
    needsReview = needsReview,
)

private fun Duration.formatHoursMinutes(): String {
    val hours = toHours()
    val minutes = toMinutes() % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
