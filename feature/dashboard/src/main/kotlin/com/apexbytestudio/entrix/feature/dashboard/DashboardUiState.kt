package com.apexbytestudio.entrix.feature.dashboard

data class DashboardUiState(
    val isLoading: Boolean = true,
    val officeConfigured: Boolean = false,
    val trackingEnabled: Boolean = false,
    val insideAnyOffice: Boolean = false,
    val todayTotal: String? = null,
    val offices: List<DashboardOfficeUi> = emptyList(),
)

data class DashboardOfficeUi(
    val id: Long,
    val name: String,
    val insideOffice: Boolean,
    val todayEnter: String?,
    val todayExit: String?,
    val todayDuration: String?,
)
