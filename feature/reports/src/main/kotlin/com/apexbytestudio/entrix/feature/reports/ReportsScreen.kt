package com.apexbytestudio.entrix.feature.reports

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexbytestudio.entrix.core.designsystem.component.EntrixBarChart
import com.apexbytestudio.entrix.core.designsystem.component.EntrixBarEntry
import com.apexbytestudio.entrix.core.designsystem.component.EntrixStatTile
import com.apexbytestudio.entrix.core.designsystem.component.EntrixToggle
import com.apexbytestudio.entrix.core.designsystem.component.LoadingState
import com.apexbytestudio.entrix.core.designsystem.icon.EntrixIcons
import com.apexbytestudio.entrix.core.designsystem.theme.EntrixSpacing
import com.apexbytestudio.entrix.core.designsystem.theme.EntrixTheme
import com.apexbytestudio.entrix.domain.model.ReportPeriod
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import java.io.File
import kotlin.math.abs

/**
 * Reports. Export lives in the app header (it reads this screen's ViewModel); this screen still
 * collects the resulting share/error events.
 */
@Composable
fun ReportsScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: ReportsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
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

    ReportsContent(
        uiState = uiState,
        onSelectPeriod = viewModel::selectPeriod,
        onPrevious = viewModel::previousPeriod,
        onNext = viewModel::nextPeriod,
        modifier = modifier,
        contentPadding = contentPadding,
    )
}

@Composable
private fun ReportsContent(
    uiState: ReportsUiState,
    onSelectPeriod: (ReportPeriod) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = EntrixSpacing.md)
            .padding(top = EntrixSpacing.xs, bottom = EntrixSpacing.md),
        verticalArrangement = Arrangement.spacedBy(EntrixSpacing.md),
    ) {
        EntrixToggle(
            options = ReportPeriod.entries,
            selected = uiState.period,
            onSelect = onSelectPeriod,
            label = { if (it == ReportPeriod.Week) "Week" else "Month" },
        )
        PeriodNavigator(
            title = uiState.title,
            dateRange = uiState.dateRange,
            canGoNext = uiState.canGoNext,
            onPrevious = onPrevious,
            onNext = onNext,
        )
        TotalCard(total = uiState.total, trend = uiState.trend)
        if (uiState.hasData) {
            Row(horizontalArrangement = Arrangement.spacedBy(EntrixSpacing.sm)) {
                uiState.stats.forEach { stat ->
                    EntrixStatTile(value = stat.value, label = stat.label, modifier = Modifier.weight(1f))
                }
            }
        }
        ChartCard(uiState)
        if (uiState.offices.isNotEmpty()) {
            OfficesCard(uiState.offices)
        }
    }
}

@Composable
private fun PeriodNavigator(
    title: String,
    dateRange: String?,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(EntrixIcons.Back, contentDescription = "Previous period")
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) { heading() },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (dateRange != null) {
                Text(
                    text = dateRange,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(EntrixIcons.Forward, contentDescription = "Next period")
        }
    }
}

@Composable
private fun TotalCard(total: String, trend: ReportTrend?) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
            Text(
                text = "Time at the office",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = total,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = EntrixSpacing.xs),
            )
            if (trend != null) {
                Spacer(Modifier.height(EntrixSpacing.sm))
                TrendPill(trend)
            }
        }
    }
}

/** Neutral when down: fewer office hours isn't a failure, just a change. */
@Composable
private fun TrendPill(trend: ReportTrend) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = if (trend.isUp) colors.primaryContainer else colors.surfaceContainerHighest,
        contentColor = if (trend.isUp) colors.onPrimaryContainer else colors.onSurfaceVariant,
    ) {
        Text(
            text = "${if (trend.isUp) "▲" else "▼"} ${abs(trend.percent)}% ${trend.comparedTo}",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun ChartCard(uiState: ReportsUiState) {
    val unit = if (uiState.period == ReportPeriod.Week) "week" else "month"
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = if (uiState.period == ReportPeriod.Week) "By day" else "By week",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(bottom = EntrixSpacing.md)
                    .semantics { heading() },
            )
            if (uiState.hasData) {
                EntrixBarChart(
                    entries = uiState.bars,
                    initialSelection = uiState.currentBarIndex,
                    average = uiState.barAverage,
                    averageLabel = uiState.barAverageLabel,
                )
            } else {
                Text(
                    text = "No visits this $unit",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "Time you spend at your offices will show up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (uiState.hasVisitInProgress) {
                Spacer(Modifier.height(EntrixSpacing.md))
                InProgressNote()
            }
        }
    }
}

@Composable
private fun OfficesCard(offices: List<OfficeBreakdownUi>) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(vertical = EntrixSpacing.sm)) {
            Text(
                text = "By office",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .semantics { heading() },
            )
            offices.forEachIndexed { index, office ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }
                OfficeRow(office)
            }
        }
    }
}

@Composable
private fun OfficeRow(office: OfficeBreakdownUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = office.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(EntrixSpacing.md))
            Text(
                text = office.duration,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(EntrixSpacing.sm))
        ShareBar(share = office.share)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${office.sharePercent} of total · ${office.visits}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ShareBar(share: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Box(
            Modifier
                .fillMaxWidth(share.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
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

@Preview(showBackground = true)
@Composable
private fun ReportsContentPreview() {
    EntrixTheme {
        ReportsContent(
            uiState = ReportsUiState(
                isLoading = false,
                title = "This week",
                dateRange = "9–15 Nov",
                total = "34h 12m",
                trend = ReportTrend(12, "vs previous week"),
                stats = listOf(
                    ReportStat("Days at office", "4"),
                    ReportStat("Avg per day", "8h 33m"),
                    ReportStat("Visits", "4"),
                ),
                hasData = true,
                bars = listOf("Mon" to 524f, "Tue" to 480f, "Wed" to 526f, "Thu" to 522f, "Fri" to 0f, "Sat" to 0f, "Sun" to 232f)
                    .map { (day, minutes) -> EntrixBarEntry(day, minutes, "${minutes.toInt() / 60}h ${minutes.toInt() % 60}m") },
                currentBarIndex = 6,
                barAverage = 457f,
                barAverageLabel = "avg 7h 37m",
                offices = listOf(
                    OfficeBreakdownUi("HQ — Gulshan", "25h 26m", "3 visits", 0.74f, "74%"),
                    OfficeBreakdownUi("Client site — Banani", "8h 46m", "1 visit", 0.26f, "26%"),
                ),
            ),
            onSelectPeriod = {},
            onPrevious = {},
            onNext = {},
        )
    }
}

/** Explains why a visit happening right now isn't in the numbers yet. */
@Composable
private fun InProgressNote() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(Modifier.width(EntrixSpacing.sm))
        Text(
            text = "Your visit in progress is added when you leave.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
