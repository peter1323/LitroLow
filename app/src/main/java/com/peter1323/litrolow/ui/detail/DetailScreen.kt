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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.peter1323.litrolow.ui.components.LoadingView
import com.peter1323.litrolow.ui.components.MessageView
import com.peter1323.litrolow.ui.components.formatPrice

/** Pantalla con toda la información de una gasolinera. */
@Composable
fun DetailScreen(viewModel: DetailViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val station = state.station
    when {
        state.isLoading -> LoadingView()
        station == null -> MessageView(stringResource(R.string.station_not_found))
        else -> Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header(station, state.isFavorite, viewModel::toggleFavorite)
            Prices(station)
            RouteButton(station)
        }
    }
}

/** Marca, dirección, horario y botón de favorita. */
@Composable
private fun Header(station: Station, isFavorite: Boolean, onToggleFavorite: () -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(station.brand, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(station.address)
            Text("${station.locality} (${station.province})")
            Text(stringResource(R.string.schedule_format, station.schedule), Modifier.padding(top = 8.dp))
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                stringResource(if (isFavorite) R.string.remove_favorite else R.string.add_favorite),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** Lista de los precios de cada combustible. */
@Composable
private fun Prices(station: Station) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FuelType.entries.forEach { fuel ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(fuel.label))
                Text(formatPrice(station.prices.of(fuel)), fontWeight = FontWeight.Bold)
            }
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
