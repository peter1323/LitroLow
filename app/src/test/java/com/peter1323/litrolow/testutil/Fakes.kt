package com.peter1323.litrolow.testutil

import com.peter1323.litrolow.data.api.CarburantesApi
import com.peter1323.litrolow.data.api.StationDto
import com.peter1323.litrolow.data.api.StationsResponse
import com.peter1323.litrolow.data.db.AlertDao
import com.peter1323.litrolow.data.db.StationDao
import com.peter1323.litrolow.data.model.Favorite
import com.peter1323.litrolow.data.model.FuelPrices
import com.peter1323.litrolow.data.model.PriceAlert
import com.peter1323.litrolow.data.model.PricePoint
import com.peter1323.litrolow.data.model.Station
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Crea una gasolinera de prueba con valores por defecto para escribir tests cortos. */
fun station(
    id: String = "1",
    brand: String = "REPSOL",
    locality: String = "MADRID",
    latitude: Double = 40.0,
    longitude: Double = -3.0,
    price95: Double? = 1.5,
    provinceId: String = "28",
    updatedAt: Long = 0,
) = Station(
    id, brand, "CALLE MAYOR 1", locality, "MADRID", provinceId, "L-D: 24H", latitude, longitude,
    FuelPrices(price95, null, 1.4, null), updatedAt,
)

/** Crea el formato de la API para una gasolinera de prueba. */
fun stationDto(id: String = "1", price95: String = "1,500", provinceId: String = "28") = StationDto(
    id = id, brand = "Repsol", provinceId = provinceId, latitude = "40,0", longitude = "-3,0", price95 = price95,
)

/** API falsa: devuelve lo indicado en [response] o lanza [failure]. */
class FakeApi(
    var response: StationsResponse = StationsResponse("OK", listOf(stationDto())),
    var failure: Exception? = null,
) : CarburantesApi {
    var fullDownloads = 0

    override suspend fun getAllStations(): StationsResponse {
        fullDownloads++
        failure?.let { throw it }
        return response
    }

    override suspend fun getProvinceStations(provinceId: String): StationsResponse {
        failure?.let { throw it }
        return response
    }
}

/** DAO en memoria que imita lo que haría Room. */
class FakeStationDao(initial: List<Station> = emptyList()) : StationDao {
    val stations = MutableStateFlow(initial)
    val history = MutableStateFlow<List<PricePoint>>(emptyList())
    val favoriteIds = MutableStateFlow<Set<String>>(emptySet())

    override fun observeAll(): Flow<List<Station>> = stations
    override fun observeById(id: String) = stations.map { list -> list.find { it.id == id } }
    override suspend fun getAll() = stations.value
    override suspend fun getByProvince(provinceId: String) = stations.value.filter { it.provinceId == provinceId }
    override suspend fun getById(id: String) = observeById(id).first()
    override suspend fun oldestUpdate() = stations.value.minOfOrNull { it.updatedAt }
    override suspend fun upsertStations(stations: List<Station>) {
        val ids = stations.map { it.id }.toSet()
        this.stations.update { current -> current.filterNot { it.id in ids } + stations }
    }
    override suspend fun deleteOlderThan(timestamp: Long) = stations.update { list -> list.filter { it.updatedAt >= timestamp } }
    override suspend fun insertHistory(points: List<PricePoint>) = history.update { it + points }
    override suspend fun deleteHistoryOlderThan(timestamp: Long) = history.update { list -> list.filter { it.recordedAt >= timestamp } }
    override fun observeHistory(stationId: String) = history.map { list -> list.filter { it.stationId == stationId } }
    override fun observeFavorites() = stations.mapFavorites()
    override fun observeIsFavorite(stationId: String) = favoriteIds.map { stationId in it }
    override suspend fun insertFavorite(favorite: Favorite) = favoriteIds.update { it + favorite.stationId }
    override suspend fun deleteFavorite(stationId: String) = favoriteIds.update { it - stationId }

    /** Cruza las gasolineras con los ids favoritos, como hace la consulta real. */
    private fun Flow<List<Station>>.mapFavorites(): Flow<List<Station>> =
        kotlinx.coroutines.flow.combine(this, favoriteIds) { list, ids -> list.filter { it.id in ids } }
}

/** DAO de alertas en memoria. */
class FakeAlertDao : AlertDao {
    val alerts = MutableStateFlow<List<PriceAlert>>(emptyList())

    override fun observeAll(): Flow<List<PriceAlert>> = alerts
    override suspend fun getAll() = alerts.value
    override suspend fun upsert(alert: PriceAlert) = alerts.update { list ->
        val id = if (alert.id == 0L) list.size + 1L else alert.id
        list.filterNot { it.id == id } + alert.copy(id = id)
    }
    override suspend fun delete(id: Long) = alerts.update { list -> list.filterNot { it.id == id } }
}
