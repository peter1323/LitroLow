package com.peter1323.litrolow.ui.detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peter1323.litrolow.R
import com.peter1323.litrolow.data.model.FuelType
import com.peter1323.litrolow.data.model.Station
import com.peter1323.litrolow.ui.components.BrandHeader
import com.peter1323.litrolow.ui.components.CreateAlertAction
import com.peter1323.litrolow.ui.components.FavoriteButton
import com.peter1323.litrolow.ui.components.FuelChips
import com.peter1323.litrolow.ui.components.LoadingView
import com.peter1323.litrolow.ui.components.MessageView
import com.peter1323.litrolow.ui.components.PricePill
import com.peter1323.litrolow.ui.components.formatPrice
import com.peter1323.litrolow.ui.components.savingsText
import com.peter1323.litrolow.ui.theme.BrandGreen
import com.peter1323.litrolow.ui.theme.OnBrandGreen

/** Pantalla con toda la información de una gasolinera. */
@Composable
fun DetailScreen(viewModel: DetailViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val station = state.station
    when {
        state.isLoading -> LoadingView()
        station == null -> MessageView(stringResource(R.string.station_not_found))
        else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Header(station, state, viewModel::toggleFavorite)
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Prices(station, state)
                RouteButton(station)
                CreateAlertAction(station, { fuel, price -> viewModel.createAlert(station, fuel, price) }) { onClick ->
                    OutlinedButton(onClick, Modifier.fillMaxWidth()) { Text(stringResource(R.string.create_alert)) }
                }
                Text(stringResource(R.string.history_title), style = MaterialTheme.typography.titleMedium)
            }
            // Fuera del margen de la pantalla: los chips ya llevan el suyo y se desplazan hasta el borde.
            FuelChips(state.chartFuel, viewModel::selectChartFuel)
            Card(Modifier.fillMaxWidth().padding(16.dp)) {
                Column(Modifier.padding(12.dp)) { PriceHistoryChart(state.chartPoints) }
            }
        }
    }
}

/** Cabecera verde con marca, dirección, horario, botón de favorita y lo que se ahorra o se paga de más en su provincia. */
@Composable
private fun Header(station: Station, state: DetailUiState, onToggleFavorite: () -> Unit) {
    BrandHeader {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(station.brand, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(station.address)
                Text("${station.locality} (${station.province})")
                Text(stringResource(R.string.schedule_format, station.schedule), Modifier.padding(top = 8.dp))
            }
            FavoriteButton(state.isFavorite, OnBrandGreen, onToggleFavorite)
        }
        stationSavingsText(station, state)?.let { text ->
            Surface(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(8.dp),
                color = OnBrandGreen,
                contentColor = BrandGreen,
            ) {
                Text(text, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Texto con el combustible de la gráfica frente a su provincia, o null si esta gasolinera no lo vende o no hay con qué compararlo. */
@Composable
private fun stationSavingsText(station: Station, state: DetailUiState): String? {
    val price = station.prices.of(state.chartFuel) ?: return null
    val comparison = state.comparisons[state.chartFuel] ?: return null
    return "${stringResource(state.chartFuel.label)}\n${savingsText(price, comparison, state.tankLitres, station.province)}"
}

/** Precios de cada combustible, coloreados según lo baratos que son en su provincia. */
@Composable
private fun Prices(station: Station, state: DetailUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FuelType.entries.chunked(2).forEach { fuels ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fuels.forEach { fuel -> FuelPriceCard(fuel, station, state, Modifier.weight(1f)) }
            }
        }
    }
}

/** Tarjeta con el nombre de un combustible y su precio. */
@Composable
private fun FuelPriceCard(fuel: FuelType, station: Station, state: DetailUiState, modifier: Modifier) {
    val price = station.prices.of(fuel)
    Card(modifier) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(fuel.label),
                Modifier.padding(bottom = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PricePill(formatPrice(price), price?.let { state.comparisons[fuel]?.levelOf(it) })
        }
    }
}

/** Botón que abre la ruta hasta la gasolinera en la app de mapas del móvil. */
@Composable
private fun RouteButton(station: Station) {
    val context = LocalContext.current
    Button(onClick = { openRoute(context, station) }, Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.open_route))
    }
}

/** Lanza la app de mapas instalada; avisa en vez de cerrarse si el móvil no tiene ninguna. */
private fun openRoute(context: Context, station: Station) {
    val coordinates = "${station.latitude},${station.longitude}"
    val uri = "geo:$coordinates?q=$coordinates(${Uri.encode(station.brand)})".toUri()
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.no_maps_app, Toast.LENGTH_LONG).show()
    }
}
