package com.apexbytestudio.entrix.feature.dashboard

data class DashboardUiState(
    val isLoading: Boolean = true,
    val officeConfigured: Boolean = false,
    val trackingEnabled: Boolean = false,
    /** The one-glance answer to "what's my situation right now?". */
    val status: TodayStatus = TodayStatus.NotInYet,
    /** Small supporting facts about today; empty when nothing happened yet. */
    val todayFacts: List<TodayFact> = emptyList(),
    val offices: List<DashboardOfficeUi> = emptyList(),
) {
    val insideAnyOffice: Boolean get() = offices.any { it.presence == OfficePresence.HereNow }
}

/** The hero card's state, in priority order: a paused tracker matters more than anything else. */
sealed interface TodayStatus {
    /** Tracking is off, so nothing is being recorded. */
    data object TrackingPaused : TodayStatus

    /** Currently inside [officeName]; [totalToday] ticks live. */
    data class AtOffice(
        val officeName: String,
        val checkedInAt: String,
        val totalToday: String,
    ) : TodayStatus

    /** Was at the office today but has left. */
    data class DoneForNow(
        val totalToday: String,
        val leftAt: String?,
    ) : TodayStatus

    /** No visits yet today. */
    data object NotInYet : TodayStatus
}

data class TodayFact(val label: String, val value: String)

data class DashboardOfficeUi(
    val id: Long,
    val name: String,
    val presence: OfficePresence,
)

sealed interface OfficePresence {
    data object HereNow : OfficePresence
    /** [duration] is null when no visit today has finished yet. */
    data class VisitedToday(val duration: String?) : OfficePresence
    data object NotToday : OfficePresence
}
