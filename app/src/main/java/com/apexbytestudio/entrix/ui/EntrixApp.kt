package com.apexbytestudio.entrix.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.apexbytestudio.entrix.core.designsystem.component.EntrixNavigationBar
import com.apexbytestudio.entrix.core.designsystem.component.EntrixNavigationBarItem
import com.apexbytestudio.entrix.core.navigation.EntrixRoute
import com.apexbytestudio.entrix.feature.attendance.AttendanceScreen
import com.apexbytestudio.entrix.feature.dashboard.DashboardScreen
import com.apexbytestudio.entrix.feature.dashboard.DashboardViewModel
import com.apexbytestudio.entrix.feature.office.OfficeListScreen
import com.apexbytestudio.entrix.feature.office.OfficeScreen
import com.apexbytestudio.entrix.feature.reports.ReportsScreen
import com.apexbytestudio.entrix.feature.settings.SettingsScreen
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrixApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // Fresh (expanded) header state per destination, so a collapsed bar on one tab doesn't carry
    // over to the next.
    val topBarState = key(backStackEntry?.id) { rememberTopAppBarState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topBarState)

    // "Clear history" is triggered from the header but confirmed by the History screen's dialog.
    var showClearHistoryDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            EntrixTopBar(
                backStackEntry = backStackEntry,
                scrollBehavior = scrollBehavior,
                onNavigateUp = { navController.navigateUp() },
                onClearHistory = { showClearHistoryDialog = true },
            )
        },
        bottomBar = {
            EntrixNavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.hasRoute(destination.route::class)
                    } == true
                    EntrixNavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = destination.label,
                        selectedIcon = destination.selectedIcon,
                        unselectedIcon = destination.unselectedIcon,
                    )
                }
            }
        },
    ) { innerPadding ->
        // Only the top and side insets are applied to the NavHost: the bottom (floating nav bar +
        // system bar) is handed to each screen as scrollable content padding, so content scrolls
        // behind the transparent area around the pill instead of being cut off above it.
        val layoutDirection = LocalLayoutDirection.current
        val contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding())
        NavHost(
            navController = navController,
            startDestination = EntrixRoute.Dashboard,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    end = innerPadding.calculateEndPadding(layoutDirection),
                ),
        ) {
            composable<EntrixRoute.Dashboard> {
                val viewModel: DashboardViewModel = koinViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                DashboardScreen(
                    uiState = uiState,
                    onManageOffices = { navController.navigate(EntrixRoute.OfficeList) },
                    contentPadding = contentPadding,
                )
            }
            composable<EntrixRoute.Attendance> {
                AttendanceScreen(
                    showClearDialog = showClearHistoryDialog,
                    onClearDialogDismiss = { showClearHistoryDialog = false },
                    contentPadding = contentPadding,
                )
            }
            composable<EntrixRoute.Reports> {
                ReportsScreen(contentPadding = contentPadding)
            }
            composable<EntrixRoute.Settings> {
                SettingsScreen(
                    onManageOffices = { navController.navigate(EntrixRoute.OfficeList) },
                    contentPadding = contentPadding,
                )
            }
            composable<EntrixRoute.OfficeList> {
                OfficeListScreen(
                    onAddOffice = { navController.navigate(EntrixRoute.OfficeEdit()) },
                    onEditOffice = { id -> navController.navigate(EntrixRoute.OfficeEdit(id)) },
                    contentPadding = contentPadding,
                )
            }
            composable<EntrixRoute.OfficeEdit> { backStackEntry ->
                val route = backStackEntry.toRoute<EntrixRoute.OfficeEdit>()
                OfficeScreen(
                    officeId = route.officeId,
                    onSaved = { navController.popBackStack() },
                    contentPadding = contentPadding,
                )
            }
        }
    }
}
