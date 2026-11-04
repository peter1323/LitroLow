package com.peter1323.litrolow.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peter1323.litrolow.data.model.FuelType
import com.peter1323.litrolow.data.model.PriceAlert
import com.peter1323.litrolow.data.repository.AlertRepository
import com.peter1323.litrolow.data.repository.StationRepository
import com.peter1323.litrolow.data.repository.parseAlertPrice
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado de la pantalla de alertas. [soldFuels] indica qué combustibles se pueden elegir al editar cada gasolinera. */
data class AlertsUiState(
    val isLoading: Boolean = true,
    val alerts: List<PriceAlert> = emptyList(),
    val soldFuels: Map<String, List<FuelType>> = emptyMap(),
)

/** Lista las alertas de precio y permite editarlas y borrarlas. */
class AlertsViewModel(
    private val repository: AlertRepository,
    stations: StationRepository,
) : ViewModel() {
    val uiState: StateFlow<AlertsUiState> =
        combine(repository.alerts, stations.stations) { alerts, stations ->
            val alertStationIds = alerts.map { it.stationId }.toSet()
            val soldFuels = stations.filter { it.id in alertStationIds }.associate { station ->
                station.id to FuelType.entries.filter { station.prices.of(it) != null }
            }
            AlertsUiState(isLoading = false, alerts = alerts, soldFuels = soldFuels)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlertsUiState())

    /** Borra una alerta. */
    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    /**
     * Guarda los cambios de una alerta y la rearma para que vuelva a avisar con el nuevo precio.
     * Devuelve false si el precio no es válido, para que el diálogo lo indique.
     */
    fun update(alert: PriceAlert, fuel: FuelType, maxPriceText: String): Boolean {
        val maxPrice = parseAlertPrice(maxPriceText) ?: return false
        viewModelScope.launch { repository.save(alert.copy(fuel = fuel, maxPrice = maxPrice, notified = false)) }
        return true
    }
}
