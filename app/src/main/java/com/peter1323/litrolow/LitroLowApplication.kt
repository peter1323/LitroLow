package com.peter1323.litrolow

import android.app.Application
import com.peter1323.litrolow.data.api.createCarburantesApi
import com.peter1323.litrolow.data.db.LitroLowDatabase
import com.peter1323.litrolow.data.repository.AlertRepository
import com.peter1323.litrolow.data.repository.LocationRepository
import com.peter1323.litrolow.data.repository.StationRepository
import com.peter1323.litrolow.data.repository.TankPreferences
import com.peter1323.litrolow.data.repository.ThemePreferences
import com.peter1323.litrolow.worker.PriceAlertWorker

/**
 * Crea una sola vez las dependencias de la app y las comparte. Con tan pocas piezas, hacerlo a mano
 * es más simple que añadir una librería de inyección de dependencias.
 */
class LitroLowApplication : Application() {
    private val database by lazy { LitroLowDatabase.create(this) }
    val stationRepository by lazy { StationRepository(createCarburantesApi(), database.stationDao()) }
    val alertRepository by lazy { AlertRepository(database.alertDao(), stationRepository) }
    val locationRepository by lazy { LocationRepository(this) }
    val themePreferences by lazy { ThemePreferences(this) }
    val tankPreferences by lazy { TankPreferences(this) }

    /** Programa la comprobación de alertas al arrancar la app. */
    override fun onCreate() {
        super.onCreate()
        PriceAlertWorker.schedule(this)
    }
}
