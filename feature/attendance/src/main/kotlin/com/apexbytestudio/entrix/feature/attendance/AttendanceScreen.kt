package com.apexbytestudio.entrix.feature.attendance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexbytestudio.entrix.core.designsystem.component.EmptyState
import com.apexbytestudio.entrix.core.designsystem.component.EntrixCard
import com.apexbytestudio.entrix.core.designsystem.component.LoadingState
import com.apexbytestudio.entrix.core.designsystem.theme.EntrixSpacing
import org.koin.androidx.compose.koinViewModel

/**
 * Attendance history. The "Clear history" trigger lives in the app header, so the confirm dialog's
 * visibility is hoisted ([showClearDialog] / [onClearDialogDismiss]); the dialog and the delete
 * itself stay here with the feature.
 */
@Composable
fun AttendanceScreen(
    showClearDialog: Boolean,
    onClearDialogDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: AttendanceViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<AttendanceVisitUi?>(null) }

    when {
        uiState.isLoading -> LoadingState(modifier.padding(contentPadding))
        uiState.isEmpty -> EmptyState(
            title = "No attendance yet",
            description = "Your visits will appear here once tracking records them.",
            modifier = modifier.padding(contentPadding),
        )

        else -> {
            val layoutDirection = LocalLayoutDirection.current
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = EntrixSpacing.md + contentPadding.calculateTopPadding(),
                    bottom = EntrixSpacing.md + contentPadding.calculateBottomPadding(),
                    start = EntrixSpacing.md + contentPadding.calculateStartPadding(layoutDirection),
                    end = EntrixSpacing.md + contentPadding.calculateEndPadding(layoutDirection),
                ),
                verticalArrangement = Arrangement.spacedBy(EntrixSpacing.md),
            ) {
                uiState.days.forEach { day ->
                    item(key = day.date) {
                        Text(
                            text = day.date,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(day.visits, key = { it.id }) { visit ->
                        VisitCard(visit = visit, onDelete = { pendingDelete = visit })
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = onClearDialogDismiss,
            title = { Text("Clear history?") },
            text = { Text("This permanently deletes all recorded attendance.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    onClearDialogDismiss()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = onClearDialogDismiss) { Text("Cancel") }
            },
        )
    }

    pendingDelete?.let { visit ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this visit?") },
            text = { Text("This permanently removes the ${visit.officeName} record on ${visit.entry}.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(visit.id)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun VisitCard(visit: AttendanceVisitUi, onDelete: () -> Unit) {
    EntrixCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = visit.officeName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = onDelete) { Text("Delete") }
        }
        if (visit.needsReview) {
            Text(
                text = "Exit time looks off — this visit may be missing a clock-out. Delete it if it's wrong.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = EntrixSpacing.xs),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Entry", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(visit.entry, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
            Column {
                Text("Exit", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(visit.exit, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Duration", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(visit.duration, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
        }
    }
}
