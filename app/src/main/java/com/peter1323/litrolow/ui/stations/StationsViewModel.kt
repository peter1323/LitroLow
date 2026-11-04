package com.peter1323.litrolow.ui.stations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peter1323.litrolow.data.model.DEFAULT_TANK_LITRES
import com.peter1323.litrolow.data.model.FuelType
import com.peter1323.litrolow.data.model.LoadError
import com.peter1323.litrolow.data.model.PriceComparison
import com.peter1323.litrolow.data.model.Station
import com.peter1323.litrolow.data.model.comparePrices
import com.peter1323.litrolow.data.model.filterAndSort
import com.peter1323.litrolow.data.model.parseTankLitres
import com.peter1323.litrolow.data.model.popularBrands
import com.peter1323.litrolow.data.repository.AlertRepository
import com.peter1323.litrolow.data.repository.AlertSaveResult
import com.peter1323.litrolow.data.repository.StationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado completo de la pantalla de lista. [comparison] y [zoneName] solo existen si los resultados
 * son de una misma provincia: comparar con gasolineras de otra punta de España no serviría de nada.
 */
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
    val comparison: PriceComparison? = null,
    val zoneName: String? = null,
    val isFiltered: Boolean = false,
    val tankLitres: Int = DEFAULT_TANK_LITRES,
)

/** Carga las gasolineras, aplica los filtros, calcula el ahorro y permite marcar favoritas y crear alertas desde la lista. */
class StationsViewModel(
    private val repository: StationRepository,
    private val alerts: AlertRepository,
    filterDispatcher: CoroutineDispatcher = Dispatchers.Default,
    tankLitres: Flow<Int> = flowOf(DEFAULT_TANK_LITRES),
    private val saveTankLitres: (Int) -> Unit = {},
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
        combine(repository.stations, filters, status, marks, tankLitres) { stations, (brand, fuel, query), (refreshing, error), (favoriteIds, alertIds), litres ->
            val filtered = stations.filterAndSort(brand, fuel, query)
            val isSameProvince = filtered.distinctBy { it.province }.size == 1
            StationsUiState(
                favoriteIds = favoriteIds,
                stationIdsWithAlert = alertIds,
                isLoading = refreshing,
                error = error,
                hasSavedData = stations.isNotEmpty(),
                stations = filtered,
                brands = popularBrands(stations),
                selectedBrand = brand,
                fuel = fuel,
                comparison = if (isSameProvince) comparePrices(filtered.mapNotNull { it.prices.of(fuel) }) else null,
                zoneName = if (isSameProvince) filtered.zoneName() else null,
                isFiltered = brand != null || query.isNotBlank(),
                tankLitres = litres,
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

    /** Guarda los litros del depósito; devuelve false si lo escrito no es una cantidad válida. */
    fun saveTank(text: String): Boolean {
        val litres = parseTankLitres(text) ?: return false
        saveTankLitres(litres)
        return true
    }

    /** Marca o quita una favorita sin entrar en el detalle. */
    fun toggleFavorite(stationId: String) {
        val isFavorite = stationId in uiState.value.favoriteIds
        viewModelScope.launch { repository.setFavorite(stationId, !isFavorite) }
    }

    /** Crea una alerta desde la lista y devuelve el resultado para que el diálogo lo explique. */
    suspend fun createAlert(station: Station, fuel: FuelType, maxPriceText: String): AlertSaveResult =
        alerts.create(station, fuel, maxPriceText)

    /** Nombre de la zona de unos resultados: su localidad si es una sola y, si no, su provincia. */
    private fun List<Station>.zoneName(): String =
        if (distinctBy { it.locality }.size == 1) first().locality else first().province

    /** Ejecuta una descarga reflejando en el estado que está en curso y cómo termina. */
    private fun load(download: suspend () -> LoadError?) {
        viewModelScope.launch {
            isRefreshing.value = true
            error.value = download()
            isRefreshing.value = false
        }
    }
}
