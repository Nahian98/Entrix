package com.apexbytestudio.entrix.domain.usecase

import com.apexbytestudio.entrix.domain.model.AttendanceRules
import com.apexbytestudio.entrix.domain.repository.AttendanceRepository
import java.time.Clock

/**
 * Caps visits that have been left open too long (a missed EXIT), closing them at
 * `enterTime + `[AttendanceRules.MAX_VISIT_DURATION] so they can't grow into multi-day "visits".
 * A capped visit's duration lands exactly at the cap, so it is flagged for review in history and
 * exports. Intended to run at app launch; [clock] is injected for deterministic tests.
 */
class ReconcileOpenVisitsUseCase(
    private val attendanceRepository: AttendanceRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    suspend operator fun invoke() {
        val now = clock.instant()
        attendanceRepository.getHistory()
            .filter { it.isOpen }
            .forEach { visit ->
                val cappedExit = visit.enterTime.plus(AttendanceRules.MAX_VISIT_DURATION)
                if (!now.isBefore(cappedExit)) {
                    attendanceRepository.closeVisit(visit.id, cappedExit)
                }
            }
    }
}
