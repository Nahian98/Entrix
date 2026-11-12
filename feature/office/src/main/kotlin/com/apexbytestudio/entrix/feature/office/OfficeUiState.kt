package com.apexbytestudio.entrix.feature.office

data class OfficeUiState(
    val isEditing: Boolean = false,
    val name: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val radiusMeters: Float = 150f,
    val isLoadingLocation: Boolean = false,
    val isSearching: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) {
    val canSave: Boolean
        get() = name.isNotBlank() &&
            latitude.toDoubleOrNull() != null &&
            longitude.toDoubleOrNull() != null &&
            !isSaving
}

sealed interface OfficeEvent {
    data object Saved : OfficeEvent
}
