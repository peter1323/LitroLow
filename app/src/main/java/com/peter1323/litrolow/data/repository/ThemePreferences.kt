package com.peter1323.litrolow.data.repository

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Recuerda el tema elegido. Si el usuario no ha elegido ninguno, la app arranca en modo oscuro. */
class ThemePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _isDark = MutableStateFlow(prefs.getBoolean(KEY_DARK, true))
    val isDark: StateFlow<Boolean> = _isDark.asStateFlow()

    /** Alterna entre claro y oscuro y guarda la elección para el próximo arranque. */
    fun toggle() {
        _isDark.value = !_isDark.value
        prefs.edit { putBoolean(KEY_DARK, _isDark.value) }
    }

    private companion object {
        const val KEY_DARK = "dark_theme"
    }
}
