package com.peter1323.litrolow

import android.app.Application
import com.peter1323.litrolow.data.api.createCarburantesApi
import com.peter1323.litrolow.data.db.LitroLowDatabase
import com.peter1323.litrolow.data.repository.AlertRepository
import com.peter1323.litrolow.data.repository.StationRepository
import com.peter1323.litrolow.data.repository.ThemePreferences

/**
 * Crea una sola vez las dependencias de la app y las comparte. Con tan pocas piezas, hacerlo a mano
 * es más simple que añadir una librería de inyección de dependencias.
 */
class LitroLowApplication : Application() {
    private val database by lazy { LitroLowDatabase.create(this) }
    val stationRepository by lazy { StationRepository(createCarburantesApi(), database.stationDao()) }
    val alertRepository by lazy { AlertRepository(database.alertDao(), stationRepository) }
    val themePreferences by lazy { ThemePreferences(this) }
}
