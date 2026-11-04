package com.peter1323.litrolow.ui.stations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peter1323.litrolow.data.model.FuelType
import com.peter1323.litrolow.data.model.LoadError
import com.peter1323.litrolow.data.model.Station
import com.peter1323.litrolow.data.model.filterAndSort
import com.peter1323.litrolow.data.model.popularBrands
import com.peter1323.litrolow.data.repository.AlertRepository
import com.peter1323.litrolow.data.repository.StationRepository
import com.peter1323.litrolow.data.repository.priceAlertFor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado completo de la pantalla de lista. */
data class StationsUiState(
    val isLoading: Boolean = true,
    val error: LoadError? = null,
    val hasSavedData: Boolean = false,
    val stations: List<Station> = emptyList(),
    val brands: List<String> = emptyList(),
    val selectedBrand: String? = null,
    val fuel: FuelType = FuelType.GASOLINE_95,
    val favoriteIds: Set<String> = emptySet(),
    val stationIdsWithAlert: Set<String> = emptySet(),
)

/** Carga las gasolineras, aplica los filtros y permite marcar favoritas y crear alertas desde la lista. */
class StationsViewModel(
    private val repository: StationRepository,
    private val alerts: AlertRepository,
    filterDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val brand = MutableStateFlow<String?>(null)
    private val fuel = MutableStateFlow(FuelType.GASOLINE_95)
    private val isRefreshing = MutableStateFlow(true)
    private val error = MutableStateFlow<LoadError?>(null)
    private val _query = MutableStateFlow("")

    /** Va fuera de [uiState] porque el campo de texto necesita el valor al instante para no perder pulsaciones. */
    val query: StateFlow<String> = _query.asStateFlow()

    private val filters = combine(brand, fuel, query) { brand, fuel, query -> Triple(brand, fuel, query) }
    private val status = combine(isRefreshing, error) { refreshing, error -> refreshing to error }

    private val marks = combine(repository.favorites, alerts.alerts) { favorites, alerts ->
        favorites.map { it.id }.toSet() to alerts.map { it.stationId }.toSet()
    }

    val uiState: StateFlow<StationsUiState> =
        combine(repository.stations, filters, status, marks) { stations, (brand, fuel, query), (refreshing, error), (favoriteIds, alertIds) ->
            StationsUiState(
                favoriteIds = favoriteIds,
                stationIdsWithAlert = alertIds,
                isLoading = refreshing,
                error = error,
                hasSavedData = stations.isNotEmpty(),
                stations = stations.filterAndSort(brand, fuel, query),
                brands = popularBrands(stations),
                selectedBrand = brand,
                fuel = fuel,
            )
        }
            // Filtrar miles de gasolineras en el hilo principal bloquearía la interfaz.
            .flowOn(filterDispatcher)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StationsUiState())

    init {
        load { repository.refreshIfStale() }
    }

    /** Fuerza una descarga nueva; lo usan «Reintentar» y el gesto de deslizar para actualizar. */
    fun refresh() = load { repository.refreshAll() }

    /** Filtra por marca; null muestra todas. */
    fun selectBrand(value: String?) {
        brand.value = value
    }

    /** Cambia el combustible por el que se filtra y se ordena. */
    fun selectFuel(value: FuelType) {
        fuel.value = value
    }

    /** Actualiza el texto de búsqueda. */
    fun updateQuery(value: String) {
        _query.value = value
    }

    /** Marca o quita una favorita sin entrar en el detalle. */
    fun toggleFavorite(stationId: String) {
        val isFavorite = stationId in uiState.value.favoriteIds
        viewModelScope.launch { repository.setFavorite(stationId, !isFavorite) }
    }

    /** Crea una alerta desde la lista. Devuelve false si el precio no es válido, para que el diálogo lo indique. */
    fun createAlert(station: Station, fuel: FuelType, maxPriceText: String): Boolean {
        val alert = priceAlertFor(station, fuel, maxPriceText) ?: return false
        viewModelScope.launch { alerts.add(alert) }
        return true
    }

    /** Ejecuta una descarga reflejando en el estado que está en curso y cómo termina. */
    private fun load(download: suspend () -> LoadError?) {
        viewModelScope.launch {
            isRefreshing.value = true
            error.value = download()
            isRefreshing.value = false
        }
    }
}
