package com.peter1323.litrolow.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peter1323.litrolow.data.model.DEFAULT_TANK_LITRES
import com.peter1323.litrolow.data.model.FuelType
import com.peter1323.litrolow.data.model.PriceComparison
import com.peter1323.litrolow.data.model.PricePoint
import com.peter1323.litrolow.data.model.Station
import com.peter1323.litrolow.data.model.comparePrices
import com.peter1323.litrolow.data.repository.AlertRepository
import com.peter1323.litrolow.data.repository.AlertSaveResult
import com.peter1323.litrolow.data.repository.StationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Un punto de la gráfica: un precio y cuándo se registró. */
data class ChartPoint(val recordedAt: Long, val price: Double)

/** Estado de la pantalla de detalle. [comparisons] tiene los combustibles que se pueden comparar en su localidad. */
data class DetailUiState(
    val isLoading: Boolean = true,
    val station: Station? = null,
    val isFavorite: Boolean = false,
    val chartFuel: FuelType = FuelType.GASOLINE_95,
    val chartPoints: List<ChartPoint> = emptyList(),
    val comparisons: Map<FuelType, PriceComparison> = emptyMap(),
    val tankLitres: Int = DEFAULT_TANK_LITRES,
)

/** Muestra una gasolinera, su historial de precios y su ahorro, y permite marcarla como favorita o crear alertas. */
class DetailViewModel(
    private val stationId: String,
    private val repository: StationRepository,
    private val alerts: AlertRepository,
    tankLitres: Flow<Int> = flowOf(DEFAULT_TANK_LITRES),
) : ViewModel() {
    private val chartFuel = MutableStateFlow(FuelType.GASOLINE_95)

    // Va aparte para no volver a recorrer todas las gasolineras cada vez que se cambia el combustible de la gráfica.
    private val comparisons = combine(repository.station(stationId), repository.stations) { station, stations ->
        station?.let { compareInLocality(it, stations) }.orEmpty()
    }
    private val stationInfo = combine(repository.station(stationId), comparisons, tankLitres) { station, comparisons, litres ->
        Triple(station, comparisons, litres)
    }

    val uiState: StateFlow<DetailUiState> = combine(
        stationInfo,
        repository.isFavorite(stationId),
        repository.history(stationId),
        chartFuel,
    ) { (station, comparisons, litres), isFavorite, history, fuel ->
        DetailUiState(
            isLoading = false,
            station = station,
            isFavorite = isFavorite,
            chartFuel = fuel,
            chartPoints = history.toChartPoints(fuel),
            comparisons = comparisons,
            tankLitres = litres,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    /** Invierte el estado de favorita de la gasolinera. */
    fun toggleFavorite() {
        viewModelScope.launch { repository.setFavorite(stationId, !uiState.value.isFavorite) }
    }

    /** Cambia el combustible que muestra la gráfica. */
    fun selectChartFuel(fuel: FuelType) {
        chartFuel.value = fuel
    }

    /** Crea una alerta en esta gasolinera y devuelve el resultado para que el diálogo lo explique. */
    suspend fun createAlert(station: Station, fuel: FuelType, maxPriceText: String): AlertSaveResult =
        alerts.create(station, fuel, maxPriceText)

    /** Se queda con los registros que tienen precio para ese combustible. */
    private fun List<PricePoint>.toChartPoints(fuel: FuelType): List<ChartPoint> =
        mapNotNull { point -> point.prices.of(fuel)?.let { ChartPoint(point.recordedAt, it) } }

    /** Calcula las referencias de cada combustible entre las gasolineras de la misma localidad. */
    private fun compareInLocality(station: Station, stations: List<Station>): Map<FuelType, PriceComparison> {
        val local = stations.filter { it.locality == station.locality && it.province == station.province }
        return FuelType.entries.mapNotNull { fuel ->
            comparePrices(local.mapNotNull { it.prices.of(fuel) })?.let { fuel to it }
        }.toMap()
    }
}
