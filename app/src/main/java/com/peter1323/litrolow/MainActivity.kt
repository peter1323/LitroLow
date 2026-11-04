package com.peter1323.litrolow

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peter1323.litrolow.ui.navigation.LitroLowRoot
import com.peter1323.litrolow.ui.theme.LitroLowTheme

/** Única Activity de la app: muestra el splash y aloja toda la interfaz de Compose. */
class MainActivity : ComponentActivity() {

    /** Instala el splash antes de crear la vista, como exige la API SplashScreen, y aplica el tema guardado. */
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val themePreferences = (application as LitroLowApplication).themePreferences
        setContent {
            val isDark by themePreferences.isDark.collectAsStateWithLifecycle()
            LaunchedEffect(isDark) { applySystemBars(isDark) }
            LitroLowTheme(isDark) {
                LitroLowRoot(isDark, themePreferences::toggle)
            }
        }
    }

    /** Ajusta los iconos del sistema al tema de la app, que puede no coincidir con el del móvil. */
    private fun applySystemBars(isDark: Boolean) {
        val style = if (isDark) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}
