package com.apexbytestudio.entrix.ui

import androidx.compose.ui.graphics.vector.ImageVector
import com.apexbytestudio.entrix.core.designsystem.icon.EntrixIcons
import com.apexbytestudio.entrix.core.navigation.EntrixRoute

/** The bottom-bar tabs. Office setup/edit is intentionally not here — it is a pushed screen. */
enum class TopLevelDestination(
    val route: EntrixRoute,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    DASHBOARD(EntrixRoute.Dashboard, "Home", EntrixIcons.HomeFilled, EntrixIcons.HomeOutlined),
    HISTORY(EntrixRoute.Attendance, "History", EntrixIcons.HistoryFilled, EntrixIcons.HistoryOutlined),
    REPORTS(EntrixRoute.Reports, "Reports", EntrixIcons.ReportsFilled, EntrixIcons.ReportsOutlined),
    SETTINGS(EntrixRoute.Settings, "Settings", EntrixIcons.SettingsFilled, EntrixIcons.SettingsOutlined),
}
