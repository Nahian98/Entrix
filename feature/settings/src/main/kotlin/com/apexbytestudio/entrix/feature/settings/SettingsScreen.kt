package com.apexbytestudio.entrix.feature.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexbytestudio.entrix.core.designsystem.icon.EntrixIcons
import com.apexbytestudio.entrix.core.designsystem.theme.EntrixSpacing
import com.apexbytestudio.entrix.core.designsystem.theme.EntrixTheme
import com.apexbytestudio.entrix.domain.model.ExportFormat
import org.koin.androidx.compose.koinViewModel
import java.io.File

private const val GITHUB_URL = "https://github.com/Nahian98/Entrix"

@Composable
fun SettingsScreen(
    onManageOffices: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val trackingEnabled by viewModel.trackingEnabled.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.Share -> context.shareFile(event.path, event.mimeType)
                is SettingsEvent.Error -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Permissions and battery state are owned by the system and can change in system Settings, so
    // re-read them on every resume and after each permission result.
    var refreshKey by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refreshKey++ }
    val facts = remember(refreshKey) { context.readSetupFacts() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refreshKey++ }

    SettingsContent(
        trackingEnabled = trackingEnabled,
        setupItems = setupItems(facts),
        appVersion = remember { context.appVersion() },
        onTrackingChange = viewModel::setTracking,
        onFixSetupItem = { id ->
            when (id) {
                SetupItemId.Location ->
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                SetupItemId.BackgroundLocation ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        permissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                SetupItemId.Battery -> context.openBatterySettings()
            }
        },
        onManageOffices = onManageOffices,
        onExport = viewModel::exportData,
        onOpenSource = { context.openUrl(GITHUB_URL) },
        modifier = modifier,
        contentPadding = contentPadding,
    )
}

@Composable
private fun SettingsContent(
    trackingEnabled: Boolean,
    setupItems: List<SetupItem>,
    appVersion: String,
    onTrackingChange: (Boolean) -> Unit,
    onFixSetupItem: (SetupItemId) -> Unit,
    onManageOffices: () -> Unit,
    onExport: (ExportFormat) -> Unit,
    onOpenSource: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val health = trackingHealth(trackingEnabled, setupItems)
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = EntrixSpacing.md)
            .padding(top = EntrixSpacing.xs, bottom = EntrixSpacing.md),
        verticalArrangement = Arrangement.spacedBy(EntrixSpacing.lg),
    ) {
        TrackingSwitchTile(enabled = trackingEnabled, health = health, onChange = onTrackingChange)

        Section(
            title = "Tracking setup",
            trailing = setupSummary(setupItems),
        ) {
            setupItems.forEachIndexed { index, item ->
                if (index > 0) RowDivider()
                SetupRow(item = item, onFix = { onFixSetupItem(item.id) })
            }
        }

        Section(title = "Offices") {
            NavigationRow(
                title = "Manage offices",
                subtitle = "Add, edit or remove the places you track",
                trailingIcon = EntrixIcons.Forward,
                onClick = onManageOffices,
            )
        }

        Section(title = "Your data") {
            NavigationRow(
                title = "Export as CSV",
                subtitle = "Every visit, ready for Sheets or Excel",
                trailingIcon = EntrixIcons.Export,
                onClick = { onExport(ExportFormat.Csv) },
            )
            RowDivider()
            NavigationRow(
                title = "Export as JSON",
                subtitle = "A complete raw backup of your records",
                trailingIcon = EntrixIcons.Export,
                onClick = { onExport(ExportFormat.Json) },
            )
        }

        Section(title = "About") {
            InfoRow(title = "Version", value = appVersion)
            RowDivider()
            NavigationRow(
                title = "Source code",
                subtitle = "View Entrix on GitHub",
                trailingIcon = EntrixIcons.Forward,
                onClick = onOpenSource,
            )
        }
    }
}

// region Main switch

/**
 * Android's "main switch" pattern: one prominent, fully tappable tile for the setting the whole
 * screen revolves around, tinted when on, with a subtitle that says whether it's actually working.
 */
@Composable
private fun TrackingSwitchTile(enabled: Boolean, health: TrackingHealth, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = if (enabled) colors.primaryContainer else colors.surfaceContainerHigh,
        contentColor = if (enabled) colors.onPrimaryContainer else colors.onSurface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = enabled, role = Role.Switch, onValueChange = onChange)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Automatic tracking",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = health.message(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (health is TrackingHealth.NeedsAction) colors.error else LocalContentColor.current.copy(alpha = 0.8f),
                )
            }
            Spacer(Modifier.width(EntrixSpacing.md))
            // The whole tile is the toggle; the Switch is just its visual.
            Switch(checked = enabled, onCheckedChange = null)
        }
    }
}

private fun TrackingHealth.message(): String = when (this) {
    TrackingHealth.Off -> "Off. Turn on to log office visits automatically."
    TrackingHealth.Ready -> "On. Your visits are logged automatically."
    is TrackingHealth.NeedsAction ->
        if (count == 1) "On, but 1 step below is needed to work." else "On, but $count steps below are needed to work."
    is TrackingHealth.Suggestions ->
        if (count == 1) "On. 1 tip below can make it more reliable." else "On. $count tips below can make it more reliable."
}

private fun setupSummary(items: List<SetupItem>): String {
    val open = items.count { it.state != SetupItemState.Done }
    return if (open == 0) "All set" else "$open to review"
}

// endregion

// region Rows

@Composable
private fun Section(
    title: String,
    trailing: String? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(EntrixSpacing.sm)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = EntrixSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            if (trailing != null) {
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

@Composable
private fun SetupRow(item: SetupItem, onFix: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (icon, tint) = when (item.state) {
            SetupItemState.Done -> EntrixIcons.CheckCircle to colors.primary
            SetupItemState.Required -> EntrixIcons.Alert to colors.error
            SetupItemState.Suggested -> EntrixIcons.Alert to colors.onSurfaceVariant
        }
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(EntrixSpacing.md))
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {},
        ) {
            Text(item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
        if (item.actionLabel != null) {
            Spacer(Modifier.width(EntrixSpacing.sm))
            FilledTonalButton(onClick = onFix, enabled = item.actionEnabled) { Text(item.actionLabel) }
        }
    }
}

@Composable
private fun NavigationRow(
    title: String,
    subtitle: String,
    trailingIcon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(EntrixSpacing.md))
        Icon(
            trailingIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// endregion

// region Platform helpers

private fun Context.readSetupFacts(): SetupFacts = SetupFacts(
    locationGranted = isGranted(Manifest.permission.ACCESS_FINE_LOCATION),
    backgroundLocationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        isGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
    batteryUnrestricted = isIgnoringBatteryOptimizations(),
    needsBackgroundPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
)

private fun Context.isGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Context.isIgnoringBatteryOptimizations(): Boolean {
    val powerManager = getSystemService(PowerManager::class.java) ?: return true
    return powerManager.isIgnoringBatteryOptimizations(packageName)
}

private fun Context.appVersion(): String =
    runCatching { packageManager.getPackageInfo(packageName, 0).versionName }
        .getOrNull() ?: "—"

private fun Context.openBatterySettings() {
    runCatching {
        startActivity(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private fun Context.openUrl(url: String) {
    runCatching {
        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private fun Context.shareFile(path: String, mimeType: String) {
    val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", File(path))
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(
        Intent.createChooser(intent, "Export attendance").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

// endregion

@Preview(showBackground = true)
@Composable
private fun SettingsContentPreview() {
    EntrixTheme {
        SettingsContent(
            trackingEnabled = true,
            setupItems = setupItems(
                SetupFacts(
                    locationGranted = true,
                    backgroundLocationGranted = true,
                    batteryUnrestricted = false,
                    needsBackgroundPermission = true,
                ),
            ),
            appVersion = "1.0",
            onTrackingChange = {},
            onFixSetupItem = {},
            onManageOffices = {},
            onExport = {},
            onOpenSource = {},
        )
    }
}
