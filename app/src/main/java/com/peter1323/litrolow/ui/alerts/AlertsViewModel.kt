package com.peter1323.litrolow.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peter1323.litrolow.data.model.PriceAlert
import com.peter1323.litrolow.data.repository.AlertRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado de la pantalla de alertas. */
data class AlertsUiState(
    val isLoading: Boolean = true,
    val alerts: List<PriceAlert> = emptyList(),
)

/** Lista las alertas de precio y permite borrarlas. */
class AlertsViewModel(private val repository: AlertRepository) : ViewModel() {
    val uiState: StateFlow<AlertsUiState> = repository.alerts
        .map { AlertsUiState(isLoading = false, alerts = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlertsUiState())

    /** Borra una alerta. */
    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }
}
