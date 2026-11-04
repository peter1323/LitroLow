package com.peter1323.litrolow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.peter1323.litrolow.data.model.PriceLevel

/** Verde de las cabeceras; es igual en los dos temas porque lleva siempre texto oscuro encima. */
val BrandGreen = Color(0xFF12D98A)
val OnBrandGreen = Color(0xFF04342C)

/** Fondo y texto del semáforo de precios; se leen bien en los dos temas. */
val PriceLevel.colors: Pair<Color, Color>
    get() = when (this) {
        PriceLevel.CHEAP -> BrandGreen to OnBrandGreen
        PriceLevel.MEDIUM -> Color(0xFFFFC145) to Color(0xFF412402)
        PriceLevel.HIGH -> Color(0xFFFF6B5E) to Color(0xFF501313)
    }

// Paleta propia: un verde intenso que evoca ahorro y un ámbar para resaltar lo importante.
private val DarkColors = darkColorScheme(
    primary = BrandGreen,
    onPrimary = OnBrandGreen,
    primaryContainer = Color(0xFF0B5D47),
    onPrimaryContainer = Color(0xFFB7F5DC),
    secondary = Color(0xFFFFC145),
    onSecondary = Color(0xFF412402),
    secondaryContainer = Color(0xFF0F4A36),
    onSecondaryContainer = Color(0xFFB7F5DC),
    background = Color(0xFF0C1512),
    onBackground = Color(0xFFEAF5F0),
    surface = Color(0xFF0C1512),
    onSurface = Color(0xFFEAF5F0),
    surfaceVariant = Color(0xFF2B3A35),
    onSurfaceVariant = Color(0xFFA8BAB2),
    surfaceContainer = Color(0xFF17231F),
    surfaceContainerHigh = Color(0xFF1E2C27),
    surfaceContainerHighest = Color(0xFF17231F),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B6B52),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7F5DC),
    onPrimaryContainer = Color(0xFF002118),
    secondary = Color(0xFF7D5700),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB7F5DC),
    onSecondaryContainer = Color(0xFF002118),
    background = Color(0xFFF7FBF8),
    onBackground = Color(0xFF171D1A),
    surface = Color(0xFFF7FBF8),
    onSurface = Color(0xFF171D1A),
    surfaceVariant = Color(0xFFDBE5DF),
    onSurfaceVariant = Color(0xFF3F4945),
    surfaceContainer = Color(0xFFEBF0EC),
    surfaceContainerHigh = Color(0xFFE5ECE8),
    surfaceContainerHighest = Color(0xFFEEF4F0),
)

/** Aplica la paleta clara u oscura de LitroLow a todo el contenido que envuelve. */
@Composable
fun LitroLowTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
