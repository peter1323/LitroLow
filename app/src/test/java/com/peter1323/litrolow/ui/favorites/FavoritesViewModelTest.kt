package com.peter1323.litrolow.ui.favorites

import com.peter1323.litrolow.data.repository.AlertRepository
import com.peter1323.litrolow.data.repository.StationRepository
import com.peter1323.litrolow.testutil.FakeAlertDao
import com.peter1323.litrolow.testutil.FakeApi
import com.peter1323.litrolow.testutil.FakeStationDao
import com.peter1323.litrolow.testutil.MainDispatcherRule
import com.peter1323.litrolow.testutil.station
import com.peter1323.litrolow.ui.detail.DetailViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Comprueba que marcar una favorita en el detalle se refleja en la pantalla de favoritas. */
class FavoritesViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = StationRepository(FakeApi(), FakeStationDao(listOf(station("1"), station("2"))))
    private val alerts = AlertRepository(FakeAlertDao(), repository)

    /** La papelera debe quitar la favorita sin pasar por el detalle. */
    @Test
    fun `quitar desde la lista de favoritas`() = runTest(mainRule.dispatcher) {
        repository.setFavorite("1", true)
        val favorites = FavoritesViewModel(repository)
        favorites.uiState.launchIn(backgroundScope)
        assertEquals(listOf("1"), favorites.uiState.value.stations.map { it.id })

        favorites.remove("1")
        assertTrue(favorites.uiState.value.stations.isEmpty())
    }

    /** Marcar y desmarcar debe añadir y quitar la gasolinera de la lista. */
    @Test
    fun `alternar favorita actualiza la lista`() = runTest(mainRule.dispatcher) {
        val detail = DetailViewModel("1", repository, alerts)
        val favorites = FavoritesViewModel(repository)
        detail.uiState.launchIn(backgroundScope)
        favorites.uiState.launchIn(backgroundScope)
        assertTrue(favorites.uiState.value.stations.isEmpty())

        detail.toggleFavorite()
        assertTrue(detail.uiState.value.isFavorite)
        assertEquals(listOf("1"), favorites.uiState.value.stations.map { it.id })

        detail.toggleFavorite()
        assertFalse(detail.uiState.value.isFavorite)
        assertTrue(favorites.uiState.value.stations.isEmpty())
    }
}
