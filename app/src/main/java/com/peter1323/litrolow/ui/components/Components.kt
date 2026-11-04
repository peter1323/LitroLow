package com.peter1323.litrolow.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.peter1323.litrolow.R
import com.peter1323.litrolow.data.model.FuelType
import com.peter1323.litrolow.data.model.LoadError
import com.peter1323.litrolow.data.model.Station
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val SpanishLocale: Locale = Locale.forLanguageTag("es-ES")

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
            )
        }
    }
}

/** Botón de corazón para marcar o quitar una favorita; se comparte entre la lista y el detalle. */
@Composable
fun FavoriteButton(isFavorite: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            stringResource(if (isFavorite) R.string.remove_favorite else R.string.add_favorite),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * Tarjeta de una gasolinera con su marca y sus cuatro precios; resalta el combustible indicado.
 * [actions] añade botones a la cabecera, para actuar sin entrar en el detalle.
 */
@Composable
fun StationCard(
    station: Station,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlightedFuel: FuelType? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandBadge(station.brand)
                Text(
                    station.brand,
                    Modifier.weight(1f).padding(start = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                actions()
            }
            Text(
                "${station.address} · ${station.locality}",
                Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                FuelType.entries.forEach { fuel ->
                    PriceColumn(fuel, station.prices.of(fuel), isHighlighted = fuel == highlightedFuel)
                }
            }
        }
    }
}

/** Nombre de un combustible con su precio debajo. */
@Composable
private fun PriceColumn(fuel: FuelType, price: Double?, isHighlighted: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 2.dp)) {
        Text(
            stringResource(fuel.label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        Text(
            formatPrice(price),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
