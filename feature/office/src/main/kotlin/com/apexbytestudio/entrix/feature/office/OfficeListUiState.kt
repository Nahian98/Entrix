package com.apexbytestudio.entrix.feature.office

data class OfficeListUiState(
    val isLoading: Boolean = true,
    val offices: List<OfficeListItemUi> = emptyList(),
) {
    val isEmpty: Boolean get() = !isLoading && offices.isEmpty()
}

data class OfficeListItemUi(
    val id: Long,
    val name: String,
    val coordinates: String,
    val radius: String,
)
