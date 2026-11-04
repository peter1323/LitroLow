package com.peter1323.litrolow.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peter1323.litrolow.data.model.FuelType
import com.peter1323.litrolow.data.model.PriceAlert
import com.peter1323.litrolow.data.repository.AlertRepository
import com.peter1323.litrolow.data.repository.AlertSaveResult
import com.peter1323.litrolow.data.repository.StationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado de la pantalla de alertas. [soldFuels] indica qué combustibles se pueden elegir al editar cada gasolinera
 * y [currentPrices], el precio actual de cada alerta según su id; falta si su gasolinera ya no está en los datos.
 */
data class AlertsUiState(
    val isLoading: Boolean = true,
    val alerts: List<PriceAlert> = emptyList(),
    val soldFuels: Map<String, List<FuelType>> = emptyMap(),
    val currentPrices: Map<Long, Double> = emptyMap(),
) {
    /** Cuántas alertas tienen ya un precio igual o inferior a su máximo. */
    val metCount: Int get() = alerts.count { alert -> currentPrices[alert.id]?.let { it <= alert.maxPrice } == true }
}

/** Lista las alertas de precio con su precio actual y permite editarlas y borrarlas. */
class AlertsViewModel(
    private val repository: AlertRepository,
    stations: StationRepository,
) : ViewModel() {
    val uiState: StateFlow<AlertsUiState> =
        combine(repository.alerts, stations.stations) { alerts, stations ->
            val alertStationIds = alerts.map { it.stationId }.toSet()
            val alertStations = stations.filter { it.id in alertStationIds }.associateBy { it.id }
            val soldFuels = alertStations.mapValues { (_, station) ->
                FuelType.entries.filter { station.prices.of(it) != null }
            }
            val currentPrices = alerts.mapNotNull { alert ->
                alertStations[alert.stationId]?.prices?.of(alert.fuel)?.let { alert.id to it }
            }.toMap()
            AlertsUiState(isLoading = false, alerts = alerts, soldFuels = soldFuels, currentPrices = currentPrices)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlertsUiState())

    /** Borra una alerta. */
    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    /** Guarda los cambios de una alerta y devuelve el resultado para que el diálogo lo explique. */
    suspend fun update(alert: PriceAlert, fuel: FuelType, maxPriceText: String): AlertSaveResult =
        repository.update(alert, fuel, maxPriceText)
}
