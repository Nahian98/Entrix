package com.apexbytestudio.entrix.feature.office

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexbytestudio.entrix.core.designsystem.component.EntrixButton
import com.apexbytestudio.entrix.core.designsystem.component.EntrixOutlinedButton
import com.apexbytestudio.entrix.core.designsystem.theme.EntrixSpacing
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun OfficeScreen(
    officeId: Long,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OfficeViewModel = koinViewModel { parametersOf(officeId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                OfficeEvent.Saved -> onSaved()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.useCurrentLocation() else viewModel.onLocationPermissionDenied()
    }

    fun requestCurrentLocation() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.useCurrentLocation()
        else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(EntrixSpacing.md),
        verticalArrangement = Arrangement.spacedBy(EntrixSpacing.md),
    ) {
        OutlinedTextField(
            value = uiState.name,
            onValueChange = viewModel::onNameChange,
            label = { Text("Office name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = "Search or tap the map to place your office, then size the geofence with the radius slider.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LocationSearchBar(
            isSearching = uiState.isSearching,
            onSearch = viewModel::onSearchLocation,
        )
        OfficeLocationMap(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            latitude = uiState.latitude.toDoubleOrNull(),
            longitude = uiState.longitude.toDoubleOrNull(),
            radiusMeters = uiState.radiusMeters,
            onLocationPicked = viewModel::onLocationPicked,
        )
        EntrixOutlinedButton(
            text = "Open full map",
            onClick = { expanded = true },
        )

        EntrixOutlinedButton(
            text = if (uiState.isLoadingLocation) "Getting location…" else "Use my current location",
            onClick = ::requestCurrentLocation,
            enabled = !uiState.isLoadingLocation,
        )
        if (uiState.isLoadingLocation) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
            }
        }

        OutlinedTextField(
            value = uiState.latitude,
            onValueChange = viewModel::onLatitudeChange,
            label = { Text("Latitude") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.longitude,
            onValueChange = viewModel::onLongitudeChange,
            label = { Text("Longitude") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )

        Column {
            Text(
                text = "Radius: ${uiState.radiusMeters.toInt()} m",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = uiState.radiusMeters,
                onValueChange = viewModel::onRadiusChange,
                valueRange = 100f..2000f,
            )
        }

        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        EntrixButton(
            text = if (uiState.isSaving) "Saving…" else "Save office",
            onClick = viewModel::save,
            enabled = uiState.canSave,
        )
    }

    if (expanded) {
        OfficeMapDialog(
            latitude = uiState.latitude.toDoubleOrNull(),
            longitude = uiState.longitude.toDoubleOrNull(),
            radiusMeters = uiState.radiusMeters,
            isSearching = uiState.isSearching,
            onSearch = viewModel::onSearchLocation,
            onLocationPicked = viewModel::onLocationPicked,
            onRadiusChange = viewModel::onRadiusChange,
            onDismiss = { expanded = false },
        )
    }
}

/** Free-text location search (Android Geocoder); submitting moves the pin to the first match. */
@Composable
private fun LocationSearchBar(
    isSearching: Boolean,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text("Search a place or address") },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
        trailingIcon = {
            if (isSearching) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                TextButton(onClick = { onSearch(query) }) { Text("Search") }
            }
        },
    )
}

/** Full-screen version of the picker: search bar + a large map + radius control. */
@Composable
private fun OfficeMapDialog(
    latitude: Double?,
    longitude: Double?,
    radiusMeters: Float,
    isSearching: Boolean,
    onSearch: (String) -> Unit,
    onLocationPicked: (Double, Double) -> Unit,
    onRadiusChange: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(EntrixSpacing.md),
                verticalArrangement = Arrangement.spacedBy(EntrixSpacing.md),
            ) {
                LocationSearchBar(isSearching = isSearching, onSearch = onSearch)
                OfficeLocationMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    latitude = latitude,
                    longitude = longitude,
                    radiusMeters = radiusMeters,
                    onLocationPicked = onLocationPicked,
                )
                Text(
                    text = "Radius: ${radiusMeters.toInt()} m",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = radiusMeters,
                    onValueChange = onRadiusChange,
                    valueRange = 100f..2000f,
                )
                EntrixButton(text = "Done", onClick = onDismiss)
            }
        }
    }
}

/** A sensible starting view when no office location has been picked yet. */
private val DEFAULT_LOCATION = LatLng(23.8103, 90.4125)

/**
 * Interactive map for picking the office location. Tapping the map reports the point back via
 * [onLocationPicked] (which the ViewModel mirrors into the latitude/longitude fields), and the
 * geofence [radiusMeters] is drawn as a circle so the tracked area is visible. The manual
 * coordinate fields remain the offline fallback when no Maps API key / network is available.
 */
@Composable
private fun OfficeLocationMap(
    modifier: Modifier,
    latitude: Double?,
    longitude: Double?,
    radiusMeters: Float,
    onLocationPicked: (Double, Double) -> Unit,
) {
    val picked = remember(latitude, longitude) {
        if (latitude != null && longitude != null) LatLng(latitude, longitude) else null
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(picked ?: DEFAULT_LOCATION, 15f)
    }

    // Recenter when the location changes from outside the map (e.g. "use my current location").
    androidx.compose.runtime.LaunchedEffect(picked) {
        if (picked != null) cameraPositionState.animate(CameraUpdateFactory.newLatLng(picked))
    }

    GoogleMap(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        cameraPositionState = cameraPositionState,
        onMapClick = { latLng -> onLocationPicked(latLng.latitude, latLng.longitude) },
    ) {
        if (picked != null) {
            Marker(state = rememberMarkerState(key = picked.toString(), position = picked))
            Circle(
                center = picked,
                radius = radiusMeters.toDouble(),
                strokeColor = MaterialTheme.colorScheme.primary,
                strokeWidth = 4f,
                fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            )
        }
    }
}
