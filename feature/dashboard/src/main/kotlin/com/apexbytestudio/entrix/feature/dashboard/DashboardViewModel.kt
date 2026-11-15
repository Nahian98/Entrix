package com.apexbytestudio.entrix.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexbytestudio.entrix.domain.model.DashboardSnapshot
import com.apexbytestudio.entrix.domain.model.OfficeStatus
import com.apexbytestudio.entrix.domain.usecase.ObserveDashboardUseCase
import com.apexbytestudio.entrix.domain.usecase.ReconcileOpenVisitsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter

class DashboardViewModel(
    observeDashboard: ObserveDashboardUseCase,
    reconcileOpenVisits: ReconcileOpenVisitsUseCase,
    private val clock: Clock,
) : ViewModel() {

    // While inside an office the "today" total grows, so re-map the latest snapshot once a minute.
    // Outside, nothing changes with time and the ticker is not running at all. WhileSubscribed
    // also stops it whenever the screen isn't visible.
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = observeDashboard()
        .flatMapLatest { snapshot ->
            val now = if (snapshot.isInsideAnyOffice) minuteTicks() else flowOf(clock.instant())
            now.map { snapshot.toUiState(it, clock) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = DashboardUiState(isLoading = true),
        )

    init {
        // Heal any visit left open too long (missed EXIT) each time the app lands on the dashboard.
        viewModelScope.launch { reconcileOpenVisits() }
    }

    /** Emits now, then again at each wall-clock minute boundary so the display flips on time. */
    private fun minuteTicks(): Flow<Instant> = flow {
        while (true) {
            val now = clock.instant()
            emit(now)
            delay(MINUTE_MILLIS - now.toEpochMilli() % MINUTE_MILLIS)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val MINUTE_MILLIS = 60_000L
    }
}

private val timePattern: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

internal fun DashboardSnapshot.toUiState(now: Instant, clock: Clock): DashboardUiState {
    val zone = clock.zone
    val time = timePattern.withZone(zone)
    val startOfToday = now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
    val totalToday = todayTotalAt(now, startOfToday)
    val visitedToday = offices.any { it.todayVisitCount > 0 }

    val status = when {
        !trackingEnabled -> TodayStatus.TrackingPaused
        isInsideAnyOffice -> {
            // If geofences overlap, the visit that started first is the one we report.
            val current = offices
                .filter { it.currentVisitStart != null }
                .minBy { it.currentVisitStart ?: Instant.MAX }
            TodayStatus.AtOffice(
                officeName = current.office.name,
                checkedInAt = current.currentVisitStart?.let(time::format).orEmpty(),
                totalToday = totalToday.formatHoursMinutes(),
            )
        }
        visitedToday -> TodayStatus.DoneForNow(
            totalToday = totalToday.formatHoursMinutes(),
            leftAt = offices.mapNotNull { it.todayLastExit }.maxOrNull()?.let(time::format),
        )
        else -> TodayStatus.NotInYet
    }

    return DashboardUiState(
        isLoading = false,
        officeConfigured = isOfficeConfigured,
        trackingEnabled = trackingEnabled,
        status = status,
        todayFacts = todayFacts(time),
        // With tracking paused an open visit is no longer being tracked (no EXIT will arrive), so
        // don't claim the user is "here now".
        offices = offices.map { it.toUi(trustOpenVisit = trackingEnabled) },
    )
}

private fun DashboardSnapshot.todayFacts(time: DateTimeFormatter): List<TodayFact> {
    val visits = offices.sumOf { it.todayVisitCount }
    if (visits == 0) return emptyList()
    return buildList {
        offices.mapNotNull { it.todayFirstEnter }.minOrNull()?.let {
            add(TodayFact("First in", time.format(it)))
        }
        add(TodayFact(if (visits == 1) "Visit" else "Visits", visits.toString()))
        // "Last out" only means something once you're no longer inside.
        if (!isInsideAnyOffice) {
            offices.mapNotNull { it.todayLastExit }.maxOrNull()?.let {
                add(TodayFact("Last out", time.format(it)))
            }
        }
    }
}

private fun OfficeStatus.toUi(trustOpenVisit: Boolean): DashboardOfficeUi = DashboardOfficeUi(
    id = office.id,
    name = office.name,
    presence = when {
        isInside && trustOpenVisit -> OfficePresence.HereNow
        todayVisitCount > 0 -> OfficePresence.VisitedToday(
            todayCompletedDuration.takeIf { !it.isZero }?.formatHoursMinutes(),
        )
        else -> OfficePresence.NotToday
    },
)

private fun Duration.formatHoursMinutes(): String {
    val hours = toHours()
    val minutes = toMinutes() % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
