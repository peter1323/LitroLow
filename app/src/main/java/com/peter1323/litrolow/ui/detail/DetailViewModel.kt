package com.peter1323.litrolow.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peter1323.litrolow.data.model.FuelType
import com.peter1323.litrolow.data.model.Station
import com.peter1323.litrolow.data.repository.StationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado de la pantalla de detalle. */
data class DetailUiState(
    val isLoading: Boolean = true,
    val station: Station? = null,
    val isFavorite: Boolean = false,
)

/** Muestra una gasolinera y permite marcarla como favorita. */
class DetailViewModel(
    private val stationId: String,
    private val repository: StationRepository,
) : ViewModel() {

    val uiState: StateFlow<DetailUiState> =
        combine(repository.station(stationId), repository.isFavorite(stationId)) { station, isFavorite ->
            DetailUiState(isLoading = false, station = station, isFavorite = isFavorite)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    /** Invierte el estado de favorita de la gasolinera. */
    fun toggleFavorite() {
        viewModelScope.launch { repository.setFavorite(stationId, !uiState.value.isFavorite) }
    }
}
