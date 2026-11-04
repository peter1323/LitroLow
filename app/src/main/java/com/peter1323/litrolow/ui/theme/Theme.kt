package com.peter1323.litrolow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta propia: verdes que evocan ahorro y un ámbar para resaltar lo importante.
private val DarkColors = darkColorScheme(
    primary = Color(0xFF6EE7B7),
    onPrimary = Color(0xFF00382A),
    primaryContainer = Color(0xFF0B5D47),
    onPrimaryContainer = Color(0xFFB7F5DC),
    secondary = Color(0xFFFFC56E),
    onSecondary = Color(0xFF442B00),
    secondaryContainer = Color(0xFF5F4100),
    onSecondaryContainer = Color(0xFFFFDDB0),
    background = Color(0xFF0E1614),
    onBackground = Color(0xFFDEE5E1),
    surface = Color(0xFF0E1614),
    onSurface = Color(0xFFDEE5E1),
    surfaceVariant = Color(0xFF3F4945),
    onSurfaceVariant = Color(0xFFBFC9C3),
    surfaceContainer = Color(0xFF1A2320),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B6B52),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7F5DC),
    onPrimaryContainer = Color(0xFF002118),
    secondary = Color(0xFF7D5700),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDB0),
    onSecondaryContainer = Color(0xFF281800),
    background = Color(0xFFF7FBF8),
    onBackground = Color(0xFF171D1A),
    surface = Color(0xFFF7FBF8),
    onSurface = Color(0xFF171D1A),
    surfaceVariant = Color(0xFFDBE5DF),
    onSurfaceVariant = Color(0xFF3F4945),
    surfaceContainer = Color(0xFFEBF0EC),
)

/** Aplica la paleta clara u oscura de LitroLow a todo el contenido que envuelve. */
@Composable
fun LitroLowTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
