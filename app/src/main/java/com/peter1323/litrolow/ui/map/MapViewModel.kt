package com.peter1323.litrolow.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peter1323.litrolow.data.model.FuelType
import com.peter1323.litrolow.data.model.GeoPoint
import com.peter1323.litrolow.data.model.Station
import com.peter1323.litrolow.data.model.distanceKm
import com.peter1323.litrolow.data.model.matches
import com.peter1323.litrolow.data.model.normalizeText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val NEARBY_RADIUS_KM = 10.0

// Demasiados marcadores harían el mapa ilegible y lento; interesan los más baratos.
private const val MAX_MARKERS = 60

/** Problemas que la pantalla del mapa explica al usuario. */
enum class MapProblem { LOCATION_UNAVAILABLE, PERMISSION_DENIED, ADDRESS_NOT_FOUND }

/** Estado de la pantalla del mapa. [nearby] va de más barata a más cara, así que la primera es la mejor opción. */
data class MapUiState(
    val isSearching: Boolean = false,
    val problem: MapProblem? = null,
    val center: GeoPoint? = null,
    val fuel: FuelType = FuelType.GASOLINE_95,
    val nearby: List<Station> = emptyList(),
) {
    val cheapest: Station? get() = nearby.firstOrNull()
}

/**
 * Busca las gasolineras cercanas a un punto, que puede ser la ubicación del usuario o una dirección
 * escrita, para que el mapa funcione también sin dar permiso de ubicación.
 */
class MapViewModel(
    private val stations: Flow<List<Station>>,
    private val currentLocation: suspend () -> GeoPoint?,
    private val geocode: suspend (String) -> GeoPoint?,
    filterDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val fuel = MutableStateFlow(FuelType.GASOLINE_95)
    private val center = MutableStateFlow<GeoPoint?>(null)
    private val isSearching = MutableStateFlow(false)
    private val problem = MutableStateFlow<MapProblem?>(null)

    private val status = combine(isSearching, problem) { searching, problem -> searching to problem }

    val uiState: StateFlow<MapUiState> =
        combine(stations, center, fuel, status) { stations, center, fuel, (searching, problem) ->
            MapUiState(searching, problem, center, fuel, nearbyStations(stations, center, fuel))
        }
            .flowOn(filterDispatcher)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState())

    /** Cambia el combustible con el que se compara el precio. */
    fun selectFuel(value: FuelType) {
        fuel.value = value
    }

    /** Centra el mapa en la ubicación del usuario; la pantalla la llama con el permiso ya concedido. */
    fun useCurrentLocation() = search(MapProblem.LOCATION_UNAVAILABLE) { currentLocation() }

    /** Registra que el usuario ha rechazado el permiso, para sugerirle escribir una dirección. */
    fun onPermissionDenied() {
        problem.value = MapProblem.PERMISSION_DENIED
    }

    /** Centra el mapa en la dirección escrita; si el móvil no la resuelve, prueba con los datos de gasolineras. */
    fun searchAddress(address: String) {
        if (address.isBlank()) return
        search(MapProblem.ADDRESS_NOT_FOUND) { geocode(address.trim()) ?: centerOfMatchingStations(address) }
    }

    /** Ejecuta una búsqueda de punto y refleja su progreso y su resultado en el estado. */
    private fun search(problemIfMissing: MapProblem, find: suspend () -> GeoPoint?) {
        viewModelScope.launch {
            isSearching.value = true
            problem.value = null
            val found = find()
            if (found != null) center.value = found else problem.value = problemIfMissing
            isSearching.value = false
        }
    }

    /** Punto medio de las gasolineras que coinciden con el texto, por ejemplo las de una localidad. */
    private suspend fun centerOfMatchingStations(address: String): GeoPoint? {
        val query = normalizeText(address.trim())
        val matching = stations.first().filter { it.matches(query) }
        if (matching.isEmpty()) return null
        return GeoPoint(matching.map { it.latitude }.average(), matching.map { it.longitude }.average())
    }

    /** Gasolineras dentro del radio que venden el combustible, de más barata a más cara. */
    private fun nearbyStations(stations: List<Station>, center: GeoPoint?, fuel: FuelType): List<Station> {
        if (center == null) return emptyList()
        return stations
            .filter { it.prices.of(fuel) != null }
            .filter { distanceKm(center, GeoPoint(it.latitude, it.longitude)) <= NEARBY_RADIUS_KM }
            .sortedBy { it.prices.of(fuel) }
            .take(MAX_MARKERS)
    }
}
