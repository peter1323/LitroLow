package com.peter1323.litrolow.data.repository

import com.peter1323.litrolow.data.api.parsePrice
import com.peter1323.litrolow.data.db.AlertDao
import com.peter1323.litrolow.data.model.FuelType
import com.peter1323.litrolow.data.model.LoadError
import com.peter1323.litrolow.data.model.PriceAlert
import com.peter1323.litrolow.data.model.Station
import kotlinx.coroutines.flow.Flow

// Ningún carburante se acerca a este precio: algo mayor es una errata, como escribir 65 en vez de 1,65.
private const val MAX_ALERT_PRICE = 10.0

/** Convierte el precio escrito por el usuario, o devuelve null si no es válido. */
fun parseAlertPrice(text: String): Double? = parsePrice(text)?.takeIf { it <= MAX_ALERT_PRICE }

/** Crea una alerta a partir del precio escrito por el usuario, o devuelve null si no es válido. */
fun priceAlertFor(station: Station, fuel: FuelType, maxPriceText: String): PriceAlert? {
    val maxPrice = parseAlertPrice(maxPriceText) ?: return null
    return PriceAlert(
        stationId = station.id,
        stationName = "${station.brand} (${station.locality})",
        provinceId = station.provinceId,
        fuel = fuel,
        maxPrice = maxPrice,
    )
}

/** Alerta que acaba de cumplirse, junto con el precio que la ha disparado. */
data class TriggeredAlert(val alert: PriceAlert, val price: Double)

/** Guarda las alertas de precio y decide cuáles hay que avisar. */
class AlertRepository(
    private val dao: AlertDao,
    private val stations: StationRepository,
) {
    val alerts: Flow<List<PriceAlert>> = dao.observeAll()

    /** Guarda una alerta, nueva o editada. */
    suspend fun save(alert: PriceAlert) = dao.upsert(alert)

    /** Borra una alerta. */
    suspend fun delete(id: Long) = dao.delete(id)

    /**
     * Actualiza las provincias con alertas y devuelve las recién cumplidas. Cada alerta avisa una
     * sola vez y se rearma cuando el precio vuelve a superar el máximo, para no repetir la
     * notificación en cada comprobación. Si falla la descarga devuelve el error.
     */
    suspend fun check(): CheckResult {
        val alerts = dao.getAll()
        val error = alerts.map { it.provinceId }.distinct().firstNotNullOfOrNull { stations.refreshProvince(it) }
        if (error != null) return CheckResult(emptyList(), error)
        return CheckResult(alerts.mapNotNull { evaluate(it) }, null)
    }

    /** Compara el precio actual con el máximo, guarda el nuevo estado y devuelve el aviso si toca. */
    private suspend fun evaluate(alert: PriceAlert): TriggeredAlert? {
        val price = stations.getStation(alert.stationId)?.prices?.of(alert.fuel) ?: return null
        val isBelow = price <= alert.maxPrice
        if (isBelow != alert.notified) dao.upsert(alert.copy(notified = isBelow))
        return if (isBelow && !alert.notified) TriggeredAlert(alert, price) else null
    }
}

/** Resultado de una comprobación de alertas: las cumplidas y, si lo hubo, el error de descarga. */
data class CheckResult(val triggered: List<TriggeredAlert>, val error: LoadError?)
