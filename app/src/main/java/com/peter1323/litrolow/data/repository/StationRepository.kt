package com.peter1323.litrolow.data.repository

import com.peter1323.litrolow.data.api.CarburantesApi
import com.peter1323.litrolow.data.api.StationsResponse
import com.peter1323.litrolow.data.db.StationDao
import com.peter1323.litrolow.data.model.Favorite
import com.peter1323.litrolow.data.model.LoadError
import com.peter1323.litrolow.data.model.PricePoint
import com.peter1323.litrolow.data.model.Station
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

// La API actualiza los precios cada media hora: descargar antes solo gastaría datos.
private const val STALE_AFTER_MS = 30 * 60 * 1000L
private const val HISTORY_RETENTION_MS = 90 * 24 * 60 * 60 * 1000L

/**
 * Única puerta de entrada a gasolineras, historial y favoritas. Las pantallas leen siempre de
 * Room y la red solo actualiza Room, así la app sigue mostrando datos aunque no haya internet.
 */
class StationRepository(
    private val api: CarburantesApi,
    private val dao: StationDao,
    private val now: () -> Long = System::currentTimeMillis,
) {
    val stations: Flow<List<Station>> = dao.observeAll()
    val favorites: Flow<List<Station>> = dao.observeFavorites()

    /** Observa una gasolinera concreta. */
    fun station(id: String): Flow<Station?> = dao.observeById(id)

    /** Observa el historial de precios de una gasolinera. */
    fun history(stationId: String): Flow<List<PricePoint>> = dao.observeHistory(stationId)

    /** Observa si una gasolinera es favorita. */
    fun isFavorite(stationId: String): Flow<Boolean> = dao.observeIsFavorite(stationId)

    /** Marca o desmarca una gasolinera como favorita. */
    suspend fun setFavorite(stationId: String, favorite: Boolean) {
        if (favorite) dao.insertFavorite(Favorite(stationId)) else dao.deleteFavorite(stationId)
    }

    /** Actualiza todo el país solo si los datos guardados ya están caducados. */
    suspend fun refreshIfStale(): LoadError? {
        val oldest = dao.oldestUpdate()
        return if (oldest != null && now() - oldest < STALE_AFTER_MS) null else refreshAll()
    }

    /** Descarga todo el país, guarda los cambios y elimina lo que ya no existe. Devuelve el error, si lo hay. */
    suspend fun refreshAll(): LoadError? {
        val timestamp = now()
        return download(timestamp, { api.getAllStations() }, { dao.getAll() }) {
            dao.deleteOlderThan(timestamp)
            dao.deleteHistoryOlderThan(timestamp - HISTORY_RETENTION_MS)
        }
    }

    /** Descarga solo una provincia; la usan las alertas para consumir pocos datos. */
    suspend fun refreshProvince(provinceId: String): LoadError? =
        download(now(), { api.getProvinceStations(provinceId) }, { dao.getByProvince(provinceId) })

    /** Lee una gasolinera una vez. */
    suspend fun getStation(id: String): Station? = dao.getById(id)

    /**
     * Pasos comunes a toda descarga. Si la respuesta falla o viene vacía no toca nada, para no
     * perder los datos guardados por un fallo puntual de la API.
     */
    private suspend fun download(
        timestamp: Long,
        fetch: suspend () -> StationsResponse,
        previous: suspend () -> List<Station>,
        afterSave: suspend () -> Unit = {},
    ): LoadError? {
        return try {
            val fresh = fetch().stations.mapNotNull { it.toStation(timestamp) }
            if (fresh.isEmpty()) return LoadError.API_FAILURE
            dao.saveSnapshot(fresh, changedPrices(fresh, previous()))
            afterSave()
            null
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            LoadError.NO_INTERNET
        } catch (_: HttpException) {
            LoadError.API_FAILURE
        } catch (_: SerializationException) {
            LoadError.API_FAILURE
        }
    }

    /** Crea puntos de historial solo para los precios que han cambiado: repetirlos no aporta a la gráfica. */
    private fun changedPrices(fresh: List<Station>, previous: List<Station>): List<PricePoint> {
        val previousById = previous.associateBy { it.id }
        return fresh
            .filter { station -> previousById[station.id]?.prices != station.prices }
            .map { PricePoint(it.id, it.updatedAt, it.prices) }
    }
}
