package com.apexbytestudio.entrix.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.apexbytestudio.entrix.core.designsystem.component.EntrixTopAppBar
import com.apexbytestudio.entrix.core.designsystem.icon.EntrixIcons
import com.apexbytestudio.entrix.core.navigation.EntrixRoute
import com.apexbytestudio.entrix.feature.attendance.AttendanceViewModel
import com.apexbytestudio.entrix.feature.reports.ReportsViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.koin.androidx.compose.koinViewModel

/** Header content for the screen currently on top of the back stack. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrixTopBar(
    backStackEntry: NavBackStackEntry?,
    scrollBehavior: TopAppBarScrollBehavior,
    onNavigateUp: () -> Unit,
    onClearHistory: () -> Unit,
) {
    val destination = backStackEntry?.destination
    when {
        destination == null || destination.hasRoute(EntrixRoute.Dashboard::class) -> {
            val now = rememberNowOnResume()
            EntrixTopAppBar(
                title = greetingFor(now.hour),
                subtitle = now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())),
                scrollBehavior = scrollBehavior,
            )
        }

        destination.hasRoute(EntrixRoute.Attendance::class) -> {
            // Same ViewModel instance the History screen uses (scoped to its back-stack entry), so
            // the action only shows when there is something to clear.
            val viewModel: AttendanceViewModel = koinViewModel(viewModelStoreOwner = backStackEntry)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            EntrixTopAppBar(
                title = "History",
                scrollBehavior = scrollBehavior,
                actions = {
                    if (!uiState.isLoading && !uiState.isEmpty) {
                        IconButton(onClick = onClearHistory) {
                            Icon(EntrixIcons.Delete, contentDescription = "Clear history")
                        }
                    }
                },
            )
        }

        destination.hasRoute(EntrixRoute.Reports::class) -> {
            // Same ViewModel instance as the Reports screen, which collects the share/error events.
            val viewModel: ReportsViewModel = koinViewModel(viewModelStoreOwner = backStackEntry)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            EntrixTopAppBar(
                title = "Reports",
                scrollBehavior = scrollBehavior,
                actions = {
                    if (uiState.isExporting) {
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    } else if (uiState.hasData) {
                        IconButton(onClick = viewModel::exportPdf) {
                            Icon(EntrixIcons.Export, contentDescription = "Export PDF")
                        }
                    }
                },
            )
        }

        destination.hasRoute(EntrixRoute.Settings::class) ->
            EntrixTopAppBar(title = "Settings", scrollBehavior = scrollBehavior)

        destination.hasRoute(EntrixRoute.OfficeList::class) ->
            EntrixTopAppBar(
                title = "Offices",
                scrollBehavior = scrollBehavior,
                onNavigateUp = onNavigateUp,
            )

        destination.hasRoute(EntrixRoute.OfficeEdit::class) -> {
            val isNew = backStackEntry.toRoute<EntrixRoute.OfficeEdit>().officeId == 0L
            EntrixTopAppBar(
                title = if (isNew) "Add office" else "Edit office",
                scrollBehavior = scrollBehavior,
                onNavigateUp = onNavigateUp,
            )
        }
    }
}

/** The current time, refreshed whenever the screen resumes so the greeting never goes stale. */
@Composable
private fun rememberNowOnResume(): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LifecycleResumeEffect(Unit) {
        now = LocalDateTime.now()
        onPauseOrDispose { }
    }
    return now
}

internal fun greetingFor(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}
