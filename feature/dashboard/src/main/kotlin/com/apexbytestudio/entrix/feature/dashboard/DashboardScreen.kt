package com.apexbytestudio.entrix.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.apexbytestudio.entrix.core.designsystem.component.EntrixCard
import com.apexbytestudio.entrix.core.designsystem.component.EntrixOutlinedButton
import com.apexbytestudio.entrix.core.designsystem.component.EmptyState
import com.apexbytestudio.entrix.core.designsystem.component.LoadingState
import com.apexbytestudio.entrix.core.designsystem.component.EntrixButton
import com.apexbytestudio.entrix.core.designsystem.theme.EntrixSpacing
import com.apexbytestudio.entrix.core.designsystem.theme.EntrixTheme

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onManageOffices: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading -> LoadingState(modifier)
        !uiState.officeConfigured -> EmptyState(
            title = "No office configured",
            description = "Set up your office location to start tracking attendance automatically.",
            modifier = modifier,
            action = { EntrixButton(text = "Set up office", onClick = onManageOffices) },
        )

        else -> DashboardContent(uiState = uiState, onManageOffices = onManageOffices, modifier = modifier)
    }
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onManageOffices: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(EntrixSpacing.md),
        verticalArrangement = Arrangement.spacedBy(EntrixSpacing.md),
    ) {
        uiState.offices.forEach { office ->
            OfficeStatusCard(office)
        }
        TotalsCard(total = uiState.todayTotal, trackingEnabled = uiState.trackingEnabled)
        EntrixOutlinedButton(text = "Manage offices", onClick = onManageOffices)
    }
}

@Composable
private fun OfficeStatusCard(office: DashboardOfficeUi) {
    EntrixCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = office.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (office.insideOffice) "Inside" else "Outside",
                style = MaterialTheme.typography.labelLarge,
                color = if (office.insideOffice) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        LabeledValue(label = "Entry", value = office.todayEnter ?: "—")
        LabeledValue(label = "Exit", value = office.todayExit ?: "—")
        LabeledValue(label = "Today", value = office.todayDuration ?: "—")
    }
}

@Composable
private fun TotalsCard(total: String?, trackingEnabled: Boolean) {
    EntrixCard {
        LabeledValue(label = "Today, all offices", value = total ?: "—")
        LabeledValue(label = "Tracking", value = if (trackingEnabled) "Enabled" else "Disabled")
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

@Preview
@Composable
private fun DashboardContentPreview() {
    EntrixTheme {
        DashboardContent(
            uiState = DashboardUiState(
                isLoading = false,
                officeConfigured = true,
                trackingEnabled = true,
                insideAnyOffice = true,
                todayTotal = "6h 10m",
                offices = listOf(
                    DashboardOfficeUi(1, "HQ", true, "08:02", null, "1h 30m"),
                    DashboardOfficeUi(2, "Client site", false, "13:00", "16:40", "3h 40m"),
                ),
            ),
            onManageOffices = {},
        )
    }
}
