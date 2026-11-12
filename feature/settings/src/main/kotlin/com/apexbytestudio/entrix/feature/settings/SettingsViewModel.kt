package com.apexbytestudio.entrix.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexbytestudio.entrix.domain.model.ExportFormat
import com.apexbytestudio.entrix.domain.usecase.ExportAttendanceUseCase
import com.apexbytestudio.entrix.domain.usecase.ObserveTrackingEnabledUseCase
import com.apexbytestudio.entrix.domain.usecase.SetTrackingEnabledUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    observeTrackingEnabled: ObserveTrackingEnabledUseCase,
    private val setTrackingEnabled: SetTrackingEnabledUseCase,
    private val exportAttendance: ExportAttendanceUseCase,
) : ViewModel() {

    val trackingEnabled: StateFlow<Boolean> = observeTrackingEnabled()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = false,
        )

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun setTracking(enabled: Boolean) {
        viewModelScope.launch { setTrackingEnabled(enabled) }
    }

    /** Exports the full raw attendance history as CSV/JSON and emits it for sharing (free feature). */
    fun exportData(format: ExportFormat) {
        viewModelScope.launch {
            exportAttendance(format)
                .onSuccess { doc -> _events.send(SettingsEvent.Share(doc.absolutePath, doc.mimeType)) }
                .onFailure { _events.send(SettingsEvent.Error("Couldn't export your data.")) }
        }
    }
}

sealed interface SettingsEvent {
    data class Share(val path: String, val mimeType: String) : SettingsEvent
    data class Error(val message: String) : SettingsEvent
}
