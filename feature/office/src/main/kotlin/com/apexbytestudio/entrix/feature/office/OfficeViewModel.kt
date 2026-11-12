package com.apexbytestudio.entrix.feature.office

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexbytestudio.entrix.core.model.Office
import com.apexbytestudio.entrix.domain.usecase.GetCurrentLocationUseCase
import com.apexbytestudio.entrix.domain.usecase.ObserveOfficeUseCase
import com.apexbytestudio.entrix.domain.usecase.SaveOfficeUseCase
import com.apexbytestudio.entrix.domain.usecase.SearchLocationUseCase
import com.apexbytestudio.entrix.domain.usecase.SyncGeofencesUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Backs the add/edit office form. [officeId] is [Office.NO_ID] when adding a new office; otherwise
 * the existing office is loaded once to seed the form.
 */
class OfficeViewModel(
    private val officeId: Long,
    observeOffice: ObserveOfficeUseCase,
    private val saveOffice: SaveOfficeUseCase,
    private val syncGeofences: SyncGeofencesUseCase,
    private val getCurrentLocation: GetCurrentLocationUseCase,
    private val searchLocation: SearchLocationUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OfficeUiState(isEditing = officeId != Office.NO_ID))
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<OfficeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        if (officeId != Office.NO_ID) {
            viewModelScope.launch {
                observeOffice(officeId).first()?.let { office ->
                    _uiState.update {
                        it.copy(
                            name = office.name,
                            latitude = office.latitude.toCoordinateString(),
                            longitude = office.longitude.toCoordinateString(),
                            radiusMeters = office.radiusMeters,
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, errorMessage = null) }
    fun onLatitudeChange(value: String) = _uiState.update { it.copy(latitude = value, errorMessage = null) }
    fun onLongitudeChange(value: String) = _uiState.update { it.copy(longitude = value, errorMessage = null) }
    fun onRadiusChange(value: Float) = _uiState.update { it.copy(radiusMeters = value) }

    /** Called when the user picks a point on the map; mirrors into the coordinate fields. */
    fun onLocationPicked(latitude: Double, longitude: Double) = _uiState.update {
        it.copy(
            latitude = latitude.toCoordinateString(),
            longitude = longitude.toCoordinateString(),
            errorMessage = null,
        )
    }

    /** Resolves a typed place/address to coordinates and moves the pin there. */
    fun onSearchLocation(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, errorMessage = null) }
            searchLocation(trimmed)
                .onSuccess { coordinates ->
                    _uiState.update {
                        it.copy(
                            latitude = coordinates.latitude.toCoordinateString(),
                            longitude = coordinates.longitude.toCoordinateString(),
                            isSearching = false,
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isSearching = false, errorMessage = "Couldn't find that location.")
                    }
                }
        }
    }

    fun onLocationPermissionDenied() = _uiState.update {
        it.copy(errorMessage = "Location permission is needed to use your current location.")
    }

    fun useCurrentLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLocation = true, errorMessage = null) }
            getCurrentLocation()
                .onSuccess { coordinates ->
                    _uiState.update {
                        it.copy(
                            latitude = coordinates.latitude.toCoordinateString(),
                            longitude = coordinates.longitude.toCoordinateString(),
                            isLoadingLocation = false,
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoadingLocation = false,
                            errorMessage = "Couldn't get your current location.",
                        )
                    }
                }
        }
    }

    fun save() {
        val state = _uiState.value
        val latitude = state.latitude.toDoubleOrNull()
        val longitude = state.longitude.toDoubleOrNull()
        if (latitude == null || longitude == null) {
            _uiState.update { it.copy(errorMessage = "Enter a valid latitude and longitude.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val office = Office(
                id = officeId,
                name = state.name.trim(),
                latitude = latitude,
                longitude = longitude,
                radiusMeters = state.radiusMeters,
            )
            saveOffice(office)
                .onSuccess {
                    syncGeofences()
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(OfficeEvent.Saved)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = error.message ?: "Couldn't save the office.")
                    }
                }
        }
    }
}

private fun Double.toCoordinateString(): String = String.format(java.util.Locale.US, "%.6f", this)
