package com.peter1323.litrolow.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peter1323.litrolow.data.model.FuelType
import com.peter1323.litrolow.data.model.PricePoint
import com.peter1323.litrolow.data.model.Station
import com.peter1323.litrolow.data.repository.StationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Un punto de la gráfica: un precio y cuándo se registró. */
data class ChartPoint(val recordedAt: Long, val price: Double)

/** Estado de la pantalla de detalle. */
data class DetailUiState(
    val isLoading: Boolean = true,
    val station: Station? = null,
    val isFavorite: Boolean = false,
    val chartFuel: FuelType = FuelType.GASOLINE_95,
    val chartPoints: List<ChartPoint> = emptyList(),
)

/** Muestra una gasolinera, su historial de precios y permite marcarla como favorita. */
class DetailViewModel(
    private val stationId: String,
    private val repository: StationRepository,
) : ViewModel() {
    private val chartFuel = MutableStateFlow(FuelType.GASOLINE_95)

    val uiState: StateFlow<DetailUiState> = combine(
        repository.station(stationId),
        repository.isFavorite(stationId),
        repository.history(stationId),
        chartFuel,
    ) { station, isFavorite, history, fuel ->
        DetailUiState(
            isLoading = false,
            station = station,
            isFavorite = isFavorite,
            chartFuel = fuel,
            chartPoints = history.toChartPoints(fuel),
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

    /** Se queda con los registros que tienen precio para ese combustible. */
    private fun List<PricePoint>.toChartPoints(fuel: FuelType): List<ChartPoint> =
        mapNotNull { point -> point.prices.of(fuel)?.let { ChartPoint(point.recordedAt, it) } }
}
