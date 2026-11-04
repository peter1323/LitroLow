package com.peter1323.litrolow.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peter1323.litrolow.data.model.Station
import com.peter1323.litrolow.data.repository.StationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado de la pantalla de favoritas. */
data class FavoritesUiState(
    val isLoading: Boolean = true,
    val stations: List<Station> = emptyList(),
)

/** Expone las gasolineras que el usuario ha guardado como favoritas y permite quitarlas. */
class FavoritesViewModel(private val repository: StationRepository) : ViewModel() {
    val uiState: StateFlow<FavoritesUiState> = repository.favorites
        .map { FavoritesUiState(isLoading = false, stations = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavoritesUiState())

    /** Quita una gasolinera de favoritas. */
    fun remove(stationId: String) {
        viewModelScope.launch { repository.setFavorite(stationId, false) }
    }
}
