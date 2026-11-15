package com.apexbytestudio.entrix.domain.usecase

import com.apexbytestudio.entrix.core.model.Attendance
import com.apexbytestudio.entrix.domain.model.DashboardSnapshot
import com.apexbytestudio.entrix.domain.model.OfficeStatus
import com.apexbytestudio.entrix.domain.repository.AttendanceRepository
import com.apexbytestudio.entrix.domain.repository.OfficeRepository
import com.apexbytestudio.entrix.domain.repository.TrackingStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

/**
 * Combines offices, open visits, history and tracking streams into a single [DashboardSnapshot],
 * producing one [OfficeStatus] per configured office. The [clock] is injected so "today" is
 * deterministic in tests.
 */
class ObserveDashboardUseCase(
    private val officeRepository: OfficeRepository,
    private val attendanceRepository: AttendanceRepository,
    private val trackingStateRepository: TrackingStateRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    operator fun invoke(): Flow<DashboardSnapshot> = combine(
        officeRepository.observeOffices(),
        attendanceRepository.observeOpenVisits(),
        attendanceRepository.observeHistory(),
        trackingStateRepository.trackingEnabled,
    ) { offices, openVisits, history, trackingEnabled ->
        val today = today()
        val openOfficeIds = openVisits.map { it.officeId }.toSet()
        val todayByOffice = history.filter { it.startedOn(today) }.groupBy { it.officeId }

        DashboardSnapshot(
            offices = offices.map { office ->
                val todayVisits = todayByOffice[office.id].orEmpty()
                OfficeStatus(
                    office = office,
                    isInside = office.id in openOfficeIds,
                    todayFirstEnter = todayVisits.minByOrNull { it.enterTime }?.enterTime,
                    todayLastExit = todayVisits.mapNotNull { it.exitTime }.maxOrNull(),
                    todayCompletedDuration = todayVisits.fold(Duration.ZERO) { acc, visit ->
                        acc + (visit.duration ?: Duration.ZERO)
                    },
                    currentVisitStart = openVisits
                        .filter { it.officeId == office.id }
                        .minOfOrNull { it.enterTime },
                    todayVisitCount = todayVisits.size,
                )
            },
            trackingEnabled = trackingEnabled,
        )
    }

    private fun today(): LocalDate = LocalDate.now(clock)

    private fun Attendance.startedOn(date: LocalDate): Boolean {
        val zone: ZoneId = clock.zone
        return enterTime.atZone(zone).toLocalDate() == date
    }
}
