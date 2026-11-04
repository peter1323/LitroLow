package com.peter1323.litrolow.ui.stations

import com.peter1323.litrolow.data.api.StationsResponse
import com.peter1323.litrolow.data.model.FuelType
import com.peter1323.litrolow.data.model.LoadError
import com.peter1323.litrolow.data.model.Station
import com.peter1323.litrolow.data.repository.AlertRepository
import com.peter1323.litrolow.data.repository.AlertSaveResult
import com.peter1323.litrolow.data.repository.StationRepository
import com.peter1323.litrolow.testutil.FakeAlertDao
import com.peter1323.litrolow.testutil.FakeApi
import com.peter1323.litrolow.testutil.FakeStationDao
import com.peter1323.litrolow.testutil.MainDispatcherRule
import com.peter1323.litrolow.testutil.station
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/** Comprueba la carga, los errores y los filtros de la lista de gasolineras. */
class StationsViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    /** Crea el ViewModel con datos ya guardados y recientes, de modo que no descarga nada. */
    private fun viewModelWith(vararg saved: Station): StationsViewModel {
        return viewModelOf(StationRepository(FakeApi(), FakeStationDao(saved.toList()), now = { 0 }))
    }

    /** Crea el ViewModel con un repositorio concreto y alertas en memoria. */
    private fun viewModelOf(repository: StationRepository) =
        StationsViewModel(repository, AlertRepository(FakeAlertDao(), repository), mainRule.dispatcher)

    /** El corazón de la lista debe marcar y desmarcar la favorita al momento. */
    @Test
    fun `alterna favorita desde la lista`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModelWith(station("1"))
        viewModel.uiState.launchIn(backgroundScope)
        viewModel.toggleFavorite("1")
        assertEquals(setOf("1"), viewModel.uiState.value.favoriteIds)
        viewModel.toggleFavorite("1")
        assertTrue(viewModel.uiState.value.favoriteIds.isEmpty())
    }

    /** La alerta creada desde la lista debe reflejarse en su tarjeta, y un precio inválido no debe crearla. */
    @Test
    fun `crea alerta desde la lista`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModelWith(station("1"))
        viewModel.uiState.launchIn(backgroundScope)
        assertEquals(AlertSaveResult.INVALID_PRICE, viewModel.createAlert(station("1"), FuelType.GASOLINE_95, "abc"))
        assertTrue(viewModel.uiState.value.stationIdsWithAlert.isEmpty())

        assertEquals(AlertSaveResult.SAVED, viewModel.createAlert(station("1"), FuelType.GASOLINE_95, "1,45"))
        assertEquals(setOf("1"), viewModel.uiState.value.stationIdsWithAlert)
    }

    /** La lista debe salir de más barata a más cara por el combustible elegido. */
    @Test
    fun `ordena de menor a mayor precio`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModelWith(station("caro", price95 = 1.9), station("barato", price95 = 1.2))
        viewModel.uiState.launchIn(backgroundScope)
        assertEquals(listOf("barato", "caro"), viewModel.uiState.value.stations.map { it.id })
    }

    /** Una gasolinera sin el combustible elegido no se puede comparar, así que no aparece. */
    @Test
    fun `excluye las que no venden el combustible`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModelWith(station("sin95", price95 = null), station("con95"))
        viewModel.uiState.launchIn(backgroundScope)
        assertEquals(listOf("con95"), viewModel.uiState.value.stations.map { it.id })
        viewModel.selectFuel(FuelType.DIESEL)
        assertEquals(2, viewModel.uiState.value.stations.size)
    }

    /** El filtro de marca deja solo esa marca, y null vuelve a mostrar todas. */
    @Test
    fun `filtra por marca`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModelWith(station("1", brand = "REPSOL"), station("2", brand = "BALLENOIL"))
        viewModel.uiState.launchIn(backgroundScope)
        viewModel.selectBrand("BALLENOIL")
        assertEquals(listOf("2"), viewModel.uiState.value.stations.map { it.id })
        viewModel.selectBrand(null)
        assertEquals(2, viewModel.uiState.value.stations.size)
    }

    /** Buscar sin tildes ni mayúsculas debe encontrar igualmente la localidad. */
    @Test
    fun `busca ignorando tildes y mayusculas`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModelWith(station("1", locality = "MÁLAGA"), station("2", locality = "SEVILLA"))
        viewModel.uiState.launchIn(backgroundScope)
        viewModel.updateQuery("malaga")
        assertEquals(listOf("1"), viewModel.uiState.value.stations.map { it.id })
    }

    /** Sin datos guardados y sin red, la pantalla debe poder mostrar el error de conexión. */
    @Test
    fun `sin internet expone el error`() = runTest(mainRule.dispatcher) {
        val repository = StationRepository(FakeApi(failure = IOException()), FakeStationDao())
        val viewModel = viewModelOf(repository)
        viewModel.uiState.launchIn(backgroundScope)
        val state = viewModel.uiState.value
        assertEquals(LoadError.NO_INTERNET, state.error)
        assertFalse(state.isLoading)
        assertFalse(state.hasSavedData)
    }

    /** Un fallo de la API con respuesta vacía no debe borrar los datos ya guardados. */
    @Test
    fun `respuesta vacia conserva los datos guardados`() = runTest(mainRule.dispatcher) {
        val api = FakeApi(response = StationsResponse("OK", emptyList()))
        val viewModel = viewModelOf(StationRepository(api, FakeStationDao(listOf(station()))))
        viewModel.uiState.launchIn(backgroundScope)
        viewModel.refresh()
        val state = viewModel.uiState.value
        assertEquals(LoadError.API_FAILURE, state.error)
        assertTrue(state.hasSavedData)
    }

    /** Con datos recientes no se debe repetir la descarga de 12 MB al abrir la app. */
    @Test
    fun `con datos recientes no descarga`() = runTest(mainRule.dispatcher) {
        val api = FakeApi()
        val dao = FakeStationDao(listOf(station(updatedAt = 1_000)))
        viewModelOf(StationRepository(api, dao, now = { 2_000 }))
        assertEquals(0, api.fullDownloads)
            }
}
