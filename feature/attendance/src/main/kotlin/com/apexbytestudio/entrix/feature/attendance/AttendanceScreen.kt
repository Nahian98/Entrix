package com.apexbytestudio.entrix.feature.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexbytestudio.entrix.core.designsystem.component.EmptyState
import com.apexbytestudio.entrix.core.designsystem.component.LoadingState
import com.apexbytestudio.entrix.core.designsystem.icon.EntrixIcons
import com.apexbytestudio.entrix.core.designsystem.theme.EntrixSpacing
import com.apexbytestudio.entrix.core.designsystem.theme.EntrixTheme
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
            title = "No visits yet",
            description = "When you arrive at one of your offices, the visit will show up here " +
                "automatically.",
            modifier = modifier.padding(contentPadding),
        )

        else -> HistoryList(
            days = uiState.days,
            onDeleteVisit = { pendingDelete = it },
            modifier = modifier,
            contentPadding = contentPadding,
        )
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
            text = {
                Text("This permanently removes your ${visit.officeName} visit (${visit.timeRange}).")
            },
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
private fun HistoryList(
    days: List<AttendanceDayUi>,
    onDeleteVisit: (AttendanceVisitUi) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val layoutDirection = LocalLayoutDirection.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = EntrixSpacing.xs + contentPadding.calculateTopPadding(),
            bottom = EntrixSpacing.md + contentPadding.calculateBottomPadding(),
            start = EntrixSpacing.md + contentPadding.calculateStartPadding(layoutDirection),
            end = EntrixSpacing.md + contentPadding.calculateEndPadding(layoutDirection),
        ),
        verticalArrangement = Arrangement.spacedBy(EntrixSpacing.lg),
    ) {
        items(days, key = { it.key }) { day ->
            DaySection(day = day, onDeleteVisit = onDeleteVisit)
        }
    }
}

@Composable
private fun DaySection(day: AttendanceDayUi, onDeleteVisit: (AttendanceVisitUi) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(EntrixSpacing.sm)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = EntrixSpacing.xs)
                .semantics(mergeDescendants = true) { heading() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = day.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (day.total != null) {
                Text(
                    text = day.total,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column {
                day.visits.forEachIndexed { index, visit ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                    VisitRow(visit = visit, onDelete = { onDeleteVisit(visit) })
                }
            }
        }
    }
}

@Composable
private fun VisitRow(visit: AttendanceVisitUi, onDelete: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = EntrixSpacing.xs, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {},
        ) {
            Text(
                text = visit.officeName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = visit.timeRange,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            if (visit.needsReview) {
                Spacer(Modifier.height(EntrixSpacing.xs))
                Text(
                    text = "Exit time looks off — this may be a missed clock-out.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.error,
                )
            }
        }
        Spacer(Modifier.width(EntrixSpacing.md))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = visit.duration,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (visit.needsReview) colors.error else colors.onSurface,
            )
            if (visit.inProgress) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).background(colors.primary, CircleShape))
                    Spacer(Modifier.width(EntrixSpacing.xs))
                    Text(
                        text = "In progress",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary,
                    )
                }
            }
        }
        VisitMenu(onDelete = onDelete)
    }
}

/** Secondary actions for a visit; keeps the destructive one out of the way of everyday scanning. */
@Composable
private fun VisitMenu(onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                EntrixIcons.More,
                contentDescription = "More actions",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Delete visit", color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(
                        EntrixIcons.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryListPreview() {
    EntrixTheme {
        HistoryList(
            days = listOf(
                AttendanceDayUi(
                    key = "2026-11-15",
                    label = "Today",
                    total = "3h 40m",
                    visits = listOf(
                        AttendanceVisitUi(1, "HQ — Gulshan", "17:39 – now", "3h 40m", inProgress = true),
                    ),
                ),
                AttendanceDayUi(
                    key = "2026-11-12",
                    label = "Thu, 12 Nov",
                    total = "10h 2m",
                    visits = listOf(
                        AttendanceVisitUi(2, "Client site", "14:10 – 15:30", "1h 20m"),
                        AttendanceVisitUi(3, "HQ — Gulshan", "09:21 – 18:03", "8h 42m"),
                    ),
                ),
                AttendanceDayUi(
                    key = "2026-11-11",
                    label = "Wed, 11 Nov",
                    total = "17h 5m",
                    visits = listOf(
                        AttendanceVisitUi(4, "HQ — Gulshan", "08:00 – 01:05", "17h 5m", needsReview = true),
                    ),
                ),
            ),
            onDeleteVisit = {},
        )
    }
}
