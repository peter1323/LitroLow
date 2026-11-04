package com.peter1323.litrolow.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peter1323.litrolow.R
import com.peter1323.litrolow.data.model.FuelType
import com.peter1323.litrolow.data.model.LoadError
import com.peter1323.litrolow.data.model.PriceComparison
import com.peter1323.litrolow.data.model.PriceLevel
import com.peter1323.litrolow.data.model.Station
import com.peter1323.litrolow.ui.theme.BrandGreen
import com.peter1323.litrolow.ui.theme.OnBrandGreen
import com.peter1323.litrolow.ui.theme.colors
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

private val SpanishLocale: Locale = Locale.forLanguageTag("es-ES")

// Evita que un precio casi igual al más barato deje una barra invisible.
private const val MIN_BAR_FRACTION = 0.05f

// Por debajo se mostraría "0,0 céntimos": a efectos prácticos, ese precio está en la media.
private const val MIN_VISIBLE_CENTS = 0.05

/** Texto de cada error de carga, para que todas las pantallas expliquen lo mismo. */
@get:StringRes
val LoadError.message: Int
    get() = when (this) {
        LoadError.NO_INTERNET -> R.string.error_no_internet
        LoadError.API_FAILURE -> R.string.error_api
    }

/** Da formato a un precio con tres decimales y coma ("1,529 €"), o un guion si no hay precio. */
@Composable
fun formatPrice(price: Double?): String =
    if (price == null) {
        stringResource(R.string.no_price)
    } else {
        String.format(SpanishLocale, stringResource(R.string.price_format), price)
    }

/** Precio máximo de una alerta con dos decimales, o tres si los tiene: "1,000" se leería como mil. */
fun formatAlertPrice(price: Double): String =
    DecimalFormat("0.00#", DecimalFormatSymbols(SpanishLocale)).format(price)

/** Escribe una cantidad con coma y los decimales indicados, como "3,60". */
fun formatDecimal(value: Double, decimals: Int): String =
    String.format(SpanishLocale, "%.${decimals}f", value)

/** Explica un precio frente a la media de [zone]: el total del repostaje en euros y la diferencia en céntimos por litro. */
@Composable
fun savingsText(price: Double, comparison: PriceComparison, litres: Int, zone: String): String {
    val cents = comparison.centsBelowAverage(price)
    val total = formatDecimal(abs(comparison.savings(price, litres)), 2)
    val perLitre = formatDecimal(abs(cents), 1)
    return when {
        abs(cents) < MIN_VISIBLE_CENTS -> stringResource(R.string.price_at_average, zone)
        cents > 0 -> stringResource(R.string.savings_below_average, total, litres, perLitre, zone)
        else -> stringResource(R.string.savings_above_average, total, litres, perLitre, zone)
    }
}

/** Indicador de carga a pantalla completa. */
@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(stringResource(R.string.loading), Modifier.padding(16.dp), textAlign = TextAlign.Center)
    }
}

/** Mensaje a pantalla completa para estados vacíos o de error, con un botón opcional. */
@Composable
fun MessageView(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
            if (actionLabel != null) {
                Button(onClick = onAction, Modifier.padding(top = 16.dp)) { Text(actionLabel) }
            }
        }
    }
}

/** Colores de los chips elegidos, para que resalten con el color de la marca. */
@Composable
fun selectedChipColors(): SelectableChipColors = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
)

/** Fila desplazable de chips para elegir combustible; se comparte entre la lista y el mapa. */
@Composable
fun FuelChips(
    selected: FuelType,
    onSelect: (FuelType) -> Unit,
    modifier: Modifier = Modifier,
    fuels: List<FuelType> = FuelType.entries,
) {
    LazyRow(
        modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(fuels) { fuel ->
            FilterChip(
                selected = fuel == selected,
                onClick = { onSelect(fuel) },
                label = { Text(stringResource(fuel.label)) },
                colors = selectedChipColors(),
            )
        }
    }
}

/** Botón de corazón para marcar o quitar una favorita; se comparte entre la lista y el detalle. */
@Composable
fun FavoriteButton(isFavorite: Boolean, tint: Color = MaterialTheme.colorScheme.primary, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            stringResource(if (isFavorite) R.string.remove_favorite else R.string.add_favorite),
            tint = tint,
        )
    }
}

/** Cabecera verde que abre las pantallas principales. */
@Composable
fun BrandHeader(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), color = BrandGreen, contentColor = OnBrandGreen) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

/** Etiqueta con un precio o un estado; toma el color del semáforo, o uno neutro si no hay con qué comparar. */
@Composable
fun PricePill(
    text: String,
    level: PriceLevel?,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
) {
    val colors = level?.colors ?: (MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurface)
    Surface(modifier, shape = CircleShape, color = colors.first, contentColor = colors.second) {
        Text(text, Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = style, fontWeight = FontWeight.Bold)
    }
}

/**
 * Tarjeta de una gasolinera con su marca y sus cuatro precios. Destaca el combustible indicado y, si hay
 * [comparison], lo colorea según lo barato que es en su zona. [actions] añade botones para actuar sin entrar en el detalle.
 */
@Composable
fun StationCard(
    station: Station,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlightedFuel: FuelType? = null,
    comparison: PriceComparison? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandBadge(station.brand)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        station.brand,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${station.address} · ${station.locality}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                actions()
            }
            StationPrices(station, highlightedFuel, comparison)
        }
    }
}

/** Precio destacado con su barra, si lo hay, y los demás combustibles en pequeño. */
@Composable
private fun StationPrices(station: Station, highlightedFuel: FuelType?, comparison: PriceComparison?) {
    val highlightedPrice = highlightedFuel?.let { station.prices.of(it) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        highlightedPrice?.let { PricePill(formatPrice(it), comparison?.levelOf(it)) }
        FuelType.entries.filter { it != highlightedFuel }.forEach { fuel ->
            PriceColumn(fuel, station.prices.of(fuel), Modifier.weight(1f))
        }
    }
    if (highlightedPrice != null && comparison != null) PriceBar(highlightedPrice, comparison)
}

/** Barra que sitúa un precio entre el más barato y el más caro de su zona. */
@Composable
private fun PriceBar(price: Double, comparison: PriceComparison) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(5.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
    ) {
        Box(
            Modifier
                .fillMaxWidth(comparison.position(price).coerceAtLeast(MIN_BAR_FRACTION))
                .fillMaxHeight()
                .background(comparison.levelOf(price).colors.first, CircleShape),
        )
    }
}

/** Nombre de un combustible con su precio debajo. */
@Composable
private fun PriceColumn(fuel: FuelType, price: Double?, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(fuel.label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        Text(formatPrice(price), style = MaterialTheme.typography.bodySmall)
    }
}
