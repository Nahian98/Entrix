package com.apexbytestudio.entrix.feature.reports

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexbytestudio.entrix.core.designsystem.component.EntrixBarChart
import com.apexbytestudio.entrix.core.designsystem.component.EntrixButton
import com.apexbytestudio.entrix.core.designsystem.component.EntrixCard
import com.apexbytestudio.entrix.core.designsystem.component.LoadingState
import com.apexbytestudio.entrix.core.designsystem.theme.EntrixSpacing
import com.apexbytestudio.entrix.domain.model.ReportPeriod
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import java.io.File

@Composable
fun ReportsScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: ReportsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ReportsEvent.Share -> context.shareFile(event.path, event.mimeType)
                is ReportsEvent.Error -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (uiState.isLoading) {
        LoadingState(modifier.padding(contentPadding))
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(EntrixSpacing.md),
        verticalArrangement = Arrangement.spacedBy(EntrixSpacing.md),
    ) {
        PeriodSelector(
            selected = uiState.period,
            onSelect = viewModel::selectPeriod,
        )

        PeriodNavigator(
            label = uiState.rangeLabel,
            onPrevious = viewModel::previousPeriod,
            onNext = viewModel::nextPeriod,
        )

        SummaryCard(uiState)

        EntrixCard {
            Text(
                text = "Hours",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = EntrixSpacing.sm),
            )
            if (uiState.hasData) {
                EntrixBarChart(entries = uiState.bars)
            } else {
                Text(
                    text = "No visits recorded in this period.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (uiState.offices.isNotEmpty()) {
            EntrixCard {
                Text(
                    text = "By office",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = EntrixSpacing.sm),
                )
                uiState.offices.forEach { office ->
                    LabeledValue(
                        label = "${office.name} · ${office.visitCount} visit${if (office.visitCount == 1) "" else "s"}",
                        value = office.duration,
                    )
                }
            }
        }

        EntrixButton(
            text = if (uiState.isExporting) "Exporting…" else "Export PDF",
            onClick = viewModel::exportPdf,
            enabled = uiState.canExport,
        )
    }
}

@Composable
private fun PeriodSelector(selected: ReportPeriod, onSelect: (ReportPeriod) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(EntrixSpacing.sm)) {
        ReportPeriod.entries.forEach { period ->
            FilterChip(
                selected = selected == period,
                onClick = { onSelect(period) },
                label = { Text(if (period == ReportPeriod.Week) "Week" else "Month") },
            )
        }
    }
}

@Composable
private fun PeriodNavigator(label: String, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onPrevious) { Text("‹ Prev") }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        TextButton(onClick = onNext) { Text("Next ›") }
    }
}

@Composable
private fun SummaryCard(uiState: ReportsUiState) {
    EntrixCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = uiState.total,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            uiState.trendPercent?.let { trend ->
                val up = trend >= 0
                Text(
                    text = "${if (up) "▲" else "▼"} ${kotlin.math.abs(trend)}% vs prev",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (up) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
        }
        LabeledValue(label = "Visits", value = uiState.visitCount.toString())
        LabeledValue(label = "Days present", value = uiState.daysPresent.toString())
        LabeledValue(label = "Average / active day", value = uiState.averagePerDay)
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = EntrixSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun android.content.Context.shareFile(path: String, mimeType: String) {
    val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", File(path))
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(
        Intent.createChooser(intent, "Share timesheet").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
