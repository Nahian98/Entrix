package com.apexbytestudio.entrix.feature.office

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexbytestudio.entrix.core.designsystem.component.EmptyState
import com.apexbytestudio.entrix.core.designsystem.component.EntrixButton
import com.apexbytestudio.entrix.core.designsystem.component.EntrixCard
import com.apexbytestudio.entrix.core.designsystem.component.LoadingState
import com.apexbytestudio.entrix.core.designsystem.theme.EntrixSpacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun OfficeListScreen(
    onAddOffice: () -> Unit,
    onEditOffice: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: OfficeListViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<OfficeListItemUi?>(null) }

    when {
        uiState.isLoading -> LoadingState(modifier.padding(contentPadding))
        uiState.isEmpty -> EmptyState(
            title = "No offices yet",
            description = "Add an office location to start tracking attendance automatically.",
            modifier = modifier.padding(contentPadding),
            action = { EntrixButton(text = "Add office", onClick = onAddOffice) },
        )

        else -> Column(modifier.fillMaxSize()) {
            val layoutDirection = LocalLayoutDirection.current
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = EntrixSpacing.md + contentPadding.calculateTopPadding(),
                    bottom = EntrixSpacing.md + contentPadding.calculateBottomPadding(),
                    start = EntrixSpacing.md + contentPadding.calculateStartPadding(layoutDirection),
                    end = EntrixSpacing.md + contentPadding.calculateEndPadding(layoutDirection),
                ),
                verticalArrangement = Arrangement.spacedBy(EntrixSpacing.md),
            ) {
                item(key = "add") {
                    EntrixButton(
                        text = "Add office",
                        onClick = onAddOffice,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                items(uiState.offices, key = { it.id }) { office ->
                    OfficeCard(
                        office = office,
                        onClick = { onEditOffice(office.id) },
                        onDelete = { pendingDelete = office },
                    )
                }
            }
        }
    }

    pendingDelete?.let { office ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete ${office.name}?") },
            text = { Text("This stops tracking ${office.name}. Its recorded attendance is kept in your history and reports.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(office.id)
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
private fun OfficeCard(
    office: OfficeListItemUi,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    EntrixCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = office.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${office.coordinates}  ·  ${office.radius}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}
