package com.peter1323.litrolow.data.repository

import com.peter1323.litrolow.data.db.AlertDao
import com.peter1323.litrolow.data.model.LoadError
import com.peter1323.litrolow.data.model.PriceAlert
import kotlinx.coroutines.flow.Flow

/** Alerta que acaba de cumplirse, junto con el precio que la ha disparado. */
data class TriggeredAlert(val alert: PriceAlert, val price: Double)

/** Guarda las alertas de precio y decide cuáles hay que avisar. */
class AlertRepository(
    private val dao: AlertDao,
    private val stations: StationRepository,
) {
    val alerts: Flow<List<PriceAlert>> = dao.observeAll()

    /** Guarda una alerta nueva. */
    suspend fun add(alert: PriceAlert) = dao.upsert(alert)

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
