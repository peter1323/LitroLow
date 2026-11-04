package com.peter1323.litrolow.data.repository

import android.content.Context
import androidx.core.content.edit
import com.peter1323.litrolow.data.model.DEFAULT_TANK_LITRES
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Recuerda los litros que suele echar el usuario, con los que se calcula el ahorro en euros. */
class TankPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _litres = MutableStateFlow(prefs.getInt(KEY_LITRES, DEFAULT_TANK_LITRES))
    val litres: StateFlow<Int> = _litres.asStateFlow()

    /** Guarda los litros para los próximos arranques. */
    fun save(litres: Int) {
        _litres.value = litres
        prefs.edit { putInt(KEY_LITRES, litres) }
    }

    private companion object {
        const val KEY_LITRES = "tank_litres"
    }
}
