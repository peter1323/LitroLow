package com.peter1323.litrolow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val White = Color.White
private val Dark = Color(0xFF1A1A1A)

// Colores aproximados de las marcas más frecuentes en los datos: ayudan a reconocerlas de un vistazo.
private val BRAND_COLORS = mapOf(
    "REPSOL" to (Color(0xFFF26B21) to White),
    "MOEVE" to (Color(0xFF00A58E) to White),
    "CEPSA" to (Color(0xFFD9262E) to White),
    "GALP" to (Color(0xFFF57C00) to Dark),
    "BALLENOIL" to (Color(0xFF0B5FA5) to White),
    "PLENERGY" to (Color(0xFF7AB800) to Dark),
    "SHELL" to (Color(0xFFFFD500) to Color(0xFFD9262E)),
    "PETROPRIX" to (Color(0xFF1F2A44) to White),
    "PETRONOR" to (Color(0xFF00529B) to White),
    "CARREFOUR" to (Color(0xFF004E9E) to White),
    "BP" to (Color(0xFF007F3E) to Color(0xFFFFE600)),
    "AVIA" to (Color(0xFFC8102E) to White),
    "Q8" to (Color(0xFF004B87) to Color(0xFFFFC72C)),
)

/**
 * Texto del distintivo: el nombre entero si es tan corto como "BP" o "Q8" y, si no, su primera letra
 * o cifra. Muchos rótulos empiezan por símbolos, como "(SIN RÓTULO)".
 */
fun brandInitials(brand: String): String =
    if (brand.length <= 3 && brand.isNotEmpty()) brand else brand.firstOrNull { it.isLetterOrDigit() }?.toString() ?: "?"

/**
 * Círculo que identifica la marca con su inicial y su color. Busca por el principio del nombre
 * para que variantes como "PETROPRIX MIJAS" compartan color.
 */
@Composable
fun BrandBadge(brand: String, modifier: Modifier = Modifier) {
    val colors = BRAND_COLORS.entries.firstOrNull { brand.startsWith(it.key) }?.value
    Box(
        modifier
            .size(40.dp)
            .background(colors?.first ?: MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            brandInitials(brand),
            color = colors?.second ?: MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
