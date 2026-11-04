package com.peter1323.litrolow.ui.stations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peter1323.litrolow.R
import com.peter1323.litrolow.data.model.Station
import com.peter1323.litrolow.ui.components.CreateAlertAction
import com.peter1323.litrolow.ui.components.FavoriteButton
import com.peter1323.litrolow.ui.components.FuelChips
import com.peter1323.litrolow.ui.components.LoadingView
import com.peter1323.litrolow.ui.components.MessageView
import com.peter1323.litrolow.ui.components.StationCard
import com.peter1323.litrolow.ui.components.message

/** Pantalla principal: buscador, filtros y lista de gasolineras de más barata a más cara. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationsScreen(viewModel: StationsViewModel, onStationClick: (String) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    when {
        !state.hasSavedData && state.isLoading -> LoadingView()
        !state.hasSavedData && state.error != null -> MessageView(
            message = stringResource(state.error!!.message),
            actionLabel = stringResource(R.string.retry),
            onAction = viewModel::refresh,
        )
        else -> PullToRefreshBox(state.isLoading, viewModel::refresh) {
            Column(Modifier.fillMaxSize()) {
                Filters(state, query, viewModel)
                state.error?.let { SavedDataNotice(stringResource(it.message)) }
                StationList(state, viewModel, onStationClick)
            }
        }
    }
}

/** Buscador, chips de combustible y chips de marca. */
@Composable
private fun Filters(state: StationsUiState, query: String, viewModel: StationsViewModel) {
    OutlinedTextField(
        value = query,
        onValueChange = viewModel::updateQuery,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
    )
    FuelChips(state.fuel, viewModel::selectFuel)
    BrandChips(state.brands, state.selectedBrand, viewModel::selectBrand)
}

/** Fila desplazable con las marcas sacadas de los datos reales. */
@Composable
private fun BrandChips(brands: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(selected == null, { onSelect(null) }, { Text(stringResource(R.string.all_brands)) })
        }
        items(brands) { brand ->
            FilterChip(brand == selected, { onSelect(brand) }, { Text(brand) })
        }
    }
}

/** Avisa de que la actualización ha fallado pero se están viendo los datos guardados. */
@Composable
private fun SavedDataNotice(error: String) {
    Text(
        "$error ${stringResource(R.string.showing_saved_data)}",
        Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.secondary,
        style = MaterialTheme.typography.bodySmall,
    )
}

/** Botón de alerta de una tarjeta; se colorea si esa gasolinera ya tiene alguna alerta. */
@Composable
private fun AlertIconButton(station: Station, hasAlert: Boolean, viewModel: StationsViewModel) {
    CreateAlertAction(station, { fuel, price -> viewModel.createAlert(station, fuel, price) }) { onClick ->
        IconButton(onClick) {
            Icon(
                Icons.Default.Notifications,
                stringResource(if (hasAlert) R.string.create_another_alert else R.string.create_alert),
                tint = if (hasAlert) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Lista de gasolineras, o un mensaje si los filtros no dejan ninguna. */
@Composable
private fun StationList(state: StationsUiState, viewModel: StationsViewModel, onStationClick: (String) -> Unit) {
    if (state.stations.isEmpty()) {
        MessageView(stringResource(R.string.empty_stations))
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.stations, key = { it.id }) { station ->
            StationCard(station, { onStationClick(station.id) }, highlightedFuel = state.fuel) {
                FavoriteButton(station.id in state.favoriteIds) { viewModel.toggleFavorite(station.id) }
                AlertIconButton(station, station.id in state.stationIdsWithAlert, viewModel)
            }
        }
    }
}
