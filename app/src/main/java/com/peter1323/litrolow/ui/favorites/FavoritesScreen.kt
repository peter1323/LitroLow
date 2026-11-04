package com.peter1323.litrolow.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peter1323.litrolow.R
import com.peter1323.litrolow.ui.components.LoadingView
import com.peter1323.litrolow.ui.components.MessageView
import com.peter1323.litrolow.ui.components.StationCard

/** Lista de gasolineras favoritas, o una explicación de cómo añadirlas si aún no hay ninguna. */
@Composable
fun FavoritesScreen(viewModel: FavoritesViewModel, onStationClick: (String) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when {
        state.isLoading -> LoadingView()
        state.stations.isEmpty() -> MessageView(stringResource(R.string.empty_favorites))
        else -> LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.stations, key = { it.id }) { station ->
                StationCard(station, { onStationClick(station.id) })
            }
        }
    }
}
