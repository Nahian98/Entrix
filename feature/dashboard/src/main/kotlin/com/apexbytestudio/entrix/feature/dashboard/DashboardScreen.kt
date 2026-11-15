package com.apexbytestudio.entrix.feature.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apexbytestudio.entrix.core.designsystem.component.EntrixButton
import com.apexbytestudio.entrix.core.designsystem.component.EntrixStatTile
import com.apexbytestudio.entrix.core.designsystem.component.LoadingState
import com.apexbytestudio.entrix.core.designsystem.theme.EntrixSpacing
import com.apexbytestudio.entrix.core.designsystem.theme.EntrixTheme

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onAddOffice: () -> Unit,
    onManageOffices: () -> Unit,
    onOpenOffice: (Long) -> Unit,
    onTurnOnTracking: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    when {
        uiState.isLoading -> LoadingState(modifier.padding(contentPadding))
        !uiState.officeConfigured -> ScrollingColumn(modifier, contentPadding) {
            WelcomeCard(onAddOffice = onAddOffice)
        }

        else -> ScrollingColumn(modifier, contentPadding) {
            StatusCard(status = uiState.status, onTurnOnTracking = onTurnOnTracking)
            if (uiState.todayFacts.isNotEmpty()) {
                TodayFacts(facts = uiState.todayFacts)
            }
            OfficesSection(
                offices = uiState.offices,
                onManageOffices = onManageOffices,
                onOpenOffice = onOpenOffice,
            )
        }
    }
}

@Composable
private fun ScrollingColumn(
    modifier: Modifier,
    contentPadding: PaddingValues,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = EntrixSpacing.md)
            .padding(top = EntrixSpacing.xs, bottom = EntrixSpacing.md),
        verticalArrangement = Arrangement.spacedBy(EntrixSpacing.lg),
    ) {
        content()
    }
}

// region Status card

@Composable
private fun StatusCard(status: TodayStatus, onTurnOnTracking: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (status) {
        TodayStatus.TrackingPaused -> colors.errorContainer to colors.onErrorContainer
        is TodayStatus.AtOffice -> colors.primaryContainer to colors.onPrimaryContainer
        is TodayStatus.DoneForNow -> colors.secondaryContainer to colors.onSecondaryContainer
        TodayStatus.NotInYet -> colors.surfaceContainerHigh to colors.onSurface
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(28.dp),
        color = container,
        contentColor = content,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(EntrixSpacing.xs),
        ) {
            when (status) {
                TodayStatus.TrackingPaused -> {
                    StatusLabel("Tracking is paused")
                    StatusBody("Entrix won't record your visits until tracking is turned on.")
                    Spacer(Modifier.height(EntrixSpacing.sm))
                    Button(
                        onClick = onTurnOnTracking,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.error,
                            contentColor = colors.onError,
                        ),
                    ) { Text("Turn on tracking") }
                }

                is TodayStatus.AtOffice -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LiveDot(color = colors.primary)
                        Spacer(Modifier.width(EntrixSpacing.sm))
                        StatusLabel("At ${status.officeName}")
                    }
                    BigFigure(status.totalToday)
                    StatusBody("today · checked in at ${status.checkedInAt}")
                }

                is TodayStatus.DoneForNow -> {
                    StatusLabel("Done for now")
                    BigFigure(status.totalToday)
                    StatusBody(
                        if (status.leftAt != null) "today · left at ${status.leftAt}" else "today",
                    )
                }

                TodayStatus.NotInYet -> {
                    StatusLabel("Not in yet")
                    StatusBody(
                        "You'll be checked in automatically when you arrive at one of your offices.",
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun StatusBody(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.graphicsLayer { alpha = 0.8f },
    )
}

@Composable
private fun BigFigure(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = EntrixSpacing.xs),
    )
}

/** A softly pulsing dot signalling "live / in progress". Animates in the draw phase only. */
@Composable
private fun LiveDot(color: Color) {
    val transition = rememberInfiniteTransition(label = "liveDot")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "liveDotAlpha",
    )
    Box(
        Modifier
            .size(10.dp)
            .graphicsLayer { this.alpha = alpha }
            .background(color, CircleShape),
    )
}

// endregion

// region Today facts

@Composable
private fun TodayFacts(facts: List<TodayFact>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(EntrixSpacing.sm),
    ) {
        facts.forEach { fact ->
            EntrixStatTile(value = fact.value, label = fact.label, modifier = Modifier.weight(1f))
        }
    }
}

// endregion

// region Offices

@Composable
private fun OfficesSection(
    offices: List<DashboardOfficeUi>,
    onManageOffices: () -> Unit,
    onOpenOffice: (Long) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Your offices",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(start = EntrixSpacing.xs)
                    .semantics { heading() },
            )
            TextButton(onClick = onManageOffices) { Text("Manage") }
        }
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column {
                offices.forEachIndexed { index, office ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                    OfficeRow(office = office, onClick = { onOpenOffice(office.id) })
                }
            }
        }
    }
}

@Composable
private fun OfficeRow(office: DashboardOfficeUi, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = office.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(EntrixSpacing.md))
        PresenceLabel(office.presence)
    }
}

@Composable
private fun PresenceLabel(presence: OfficePresence) {
    val colors = MaterialTheme.colorScheme
    when (presence) {
        OfficePresence.HereNow -> Surface(
            shape = CircleShape,
            color = colors.primaryContainer,
            contentColor = colors.onPrimaryContainer,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(colors.primary))
                Spacer(Modifier.width(6.dp))
                Text("Here now", style = MaterialTheme.typography.labelLarge)
            }
        }

        is OfficePresence.VisitedToday -> Text(
            text = presence.duration?.let { "$it today" } ?: "Visited today",
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
        )

        OfficePresence.NotToday -> Text(
            text = "Not today",
            style = MaterialTheme.typography.labelLarge,
            color = colors.outline,
        )
    }
}

// endregion

// region Welcome (no offices yet)

@Composable
private fun WelcomeCard(onAddOffice: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(EntrixSpacing.md),
        ) {
            Text(
                text = "Welcome to Entrix",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = "Your office hours, logged automatically. Set it up once and Entrix " +
                    "takes care of the rest.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(EntrixSpacing.xs))
            WelcomeStep(1, "Add your office", "Pick it on the map and choose how close counts as \"in\".")
            WelcomeStep(2, "Allow location", "So Entrix can tell when you arrive and leave.")
            WelcomeStep(3, "That's it", "Visits are logged on their own. Check back anytime.")
            Spacer(Modifier.height(EntrixSpacing.xs))
            EntrixButton(text = "Add your first office", onClick = onAddOffice)
        }
    }
}

@Composable
private fun WelcomeStep(number: Int, title: String, description: String) {
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.width(EntrixSpacing.md))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// endregion

@Preview(showBackground = true)
@Composable
private fun DashboardAtOfficePreview() {
    EntrixTheme(dynamicColor = false) {
        DashboardScreen(
            uiState = DashboardUiState(
                isLoading = false,
                officeConfigured = true,
                trackingEnabled = true,
                status = TodayStatus.AtOffice("HQ", checkedInAt = "09:02", totalToday = "3h 21m"),
                todayFacts = listOf(TodayFact("First in", "09:02"), TodayFact("Visits", "2")),
                offices = listOf(
                    DashboardOfficeUi(1, "HQ", OfficePresence.HereNow),
                    DashboardOfficeUi(2, "Client site", OfficePresence.VisitedToday("1h 10m")),
                    DashboardOfficeUi(3, "Co-working", OfficePresence.NotToday),
                ),
            ),
            onAddOffice = {},
            onManageOffices = {},
            onOpenOffice = {},
            onTurnOnTracking = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardWelcomePreview() {
    EntrixTheme(dynamicColor = false) {
        DashboardScreen(
            uiState = DashboardUiState(isLoading = false),
            onAddOffice = {},
            onManageOffices = {},
            onOpenOffice = {},
            onTurnOnTracking = {},
        )
    }
}
