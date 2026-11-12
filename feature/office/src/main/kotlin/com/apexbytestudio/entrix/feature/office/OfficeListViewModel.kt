package com.apexbytestudio.entrix.feature.office

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexbytestudio.entrix.domain.usecase.ArchiveOfficeUseCase
import com.apexbytestudio.entrix.domain.usecase.ObserveOfficesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class OfficeListViewModel(
    observeOffices: ObserveOfficesUseCase,
    private val archiveOffice: ArchiveOfficeUseCase,
) : ViewModel() {

    val uiState: StateFlow<OfficeListUiState> = observeOffices()
        .map { offices ->
            OfficeListUiState(
                isLoading = false,
                offices = offices.map {
                    OfficeListItemUi(
                        id = it.id,
                        name = it.name,
                        coordinates = "%.5f, %.5f".format(Locale.US, it.latitude, it.longitude),
                        radius = "${it.radiusMeters.toInt()} m",
                    )
                },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = OfficeListUiState(isLoading = true),
        )

    fun delete(officeId: Long) {
        viewModelScope.launch { archiveOffice(officeId) }
    }
}
