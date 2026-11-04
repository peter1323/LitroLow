package com.peter1323.litrolow.ui.stations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peter1323.litrolow.R
import com.peter1323.litrolow.data.model.Station
import com.peter1323.litrolow.ui.components.BrandHeader
import com.peter1323.litrolow.ui.components.CreateAlertAction
import com.peter1323.litrolow.ui.components.FavoriteButton
import com.peter1323.litrolow.ui.components.FuelChips
import com.peter1323.litrolow.ui.components.LoadingView
import com.peter1323.litrolow.ui.components.MessageView
import com.peter1323.litrolow.ui.components.StationCard
import com.peter1323.litrolow.ui.components.TankDialog
import com.peter1323.litrolow.ui.components.formatDecimal
import com.peter1323.litrolow.ui.components.formatPrice
import com.peter1323.litrolow.ui.components.message
import com.peter1323.litrolow.ui.components.selectedChipColors
import com.peter1323.litrolow.ui.theme.BrandGreen
import com.peter1323.litrolow.ui.theme.OnBrandGreen
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull

/** Pantalla principal: la más barata con su ahorro, buscador, filtros y lista de más barata a más cara. */
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
            StationList(state, query, viewModel, onStationClick)
        }
    }
}

/** Cabecera con la gasolinera más barata de los resultados y lo que se ahorra en ella. */
@Composable
private fun CheapestHeader(station: Station, state: StationsUiState, onSave: (String) -> Boolean, onClick: () -> Unit) {
    val price = station.prices.of(state.fuel)
    val title = when {
        state.zoneName != null -> stringResource(R.string.hero_cheapest_in, state.zoneName)
        state.isFiltered -> stringResource(R.string.hero_cheapest_search)
        else -> stringResource(R.string.hero_cheapest_spain)
    }
    BrandHeader(Modifier.clickable(onClick = onClick)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Text(formatPrice(price), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text("${station.brand} · ${stringResource(state.fuel.label)}", style = MaterialTheme.typography.bodyMedium)
        val savings = if (price != null) state.comparison?.savings(price, state.tankLitres) else null
        if (savings == null) {
            val hint = when {
                state.zoneName != null -> R.string.hero_single_station
                state.isFiltered -> R.string.hero_mixed_zones
                else -> R.string.hero_search_hint
            }
            HeaderNote(stringResource(hint))
        } else {
            EditableSavings(formatDecimal(savings, 2), state.tankLitres, onSave)
        }
    }
}

/** Nota del ahorro; al tocarla se pueden cambiar los litros del depósito. */
@Composable
private fun EditableSavings(savings: String, litres: Int, onSave: (String) -> Boolean) {
    var isEditing by rememberSaveable { mutableStateOf(false) }
    HeaderNote(stringResource(R.string.hero_savings, savings, litres), Modifier.clickable { isEditing = true }, isEditable = true)
    if (isEditing) TankDialog(litres, onSave) { isEditing = false }
}

/** Texto destacado dentro de la cabecera verde. */
@Composable
private fun HeaderNote(text: String, modifier: Modifier = Modifier, isEditable: Boolean = false) {
    Surface(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = OnBrandGreen,
        contentColor = BrandGreen,
    ) {
        Row(modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            if (isEditable) Icon(Icons.Default.Edit, stringResource(R.string.tank_title), Modifier.padding(start = 8.dp))
        }
    }
}

/** Buscador, chips de combustible y chips de marca. */
@Composable
private fun Filters(state: StationsUiState, query: String, viewModel: StationsViewModel) {
    Column {
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
}

/** Fila desplazable con las marcas sacadas de los datos reales. */
@Composable
private fun BrandChips(brands: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(selected == null, { onSelect(null) }, { Text(stringResource(R.string.all_brands)) }, colors = selectedChipColors())
        }
        items(brands) { brand ->
            FilterChip(brand == selected, { onSelect(brand) }, { Text(brand) }, colors = selectedChipColors())
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

/** Toda la pantalla se desplaza a la vez para que la cabecera y los filtros no le quiten sitio a la lista. */
@Composable
private fun StationList(state: StationsUiState, query: String, viewModel: StationsViewModel, onStationClick: (String) -> Unit) {
    val listState = rememberLazyListState()
    val firstStationId by rememberUpdatedState(state.stations.firstOrNull()?.id)
    // Vuelve arriba solo si cambian los resultados, para ver su ahorro; al volver del detalle se conserva la posición.
    LaunchedEffect(Unit) {
        snapshotFlow { firstStationId }.filterNotNull().drop(1).collect { listState.scrollToItem(0) }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.stations.firstOrNull()?.let { cheapest ->
            item(key = "header") {
                CheapestHeader(cheapest, state, viewModel::saveTank) { onStationClick(cheapest.id) }
            }
        }
        item(key = "filters") { Filters(state, query, viewModel) }
        state.error?.let { error -> item(key = "notice") { SavedDataNotice(stringResource(error.message)) } }
        if (state.stations.isEmpty()) {
            item(key = "empty") { MessageView(stringResource(R.string.empty_stations)) }
        }
        items(state.stations, key = { it.id }) { station ->
            StationCard(
                station,
                { onStationClick(station.id) },
                Modifier.padding(horizontal = 16.dp),
                state.fuel,
                state.comparison.takeIf { station.id in state.zoneIds },
            ) {
                FavoriteButton(station.id in state.favoriteIds) { viewModel.toggleFavorite(station.id) }
                AlertIconButton(station, station.id in state.stationIdsWithAlert, viewModel)
            }
        }
    }
}
