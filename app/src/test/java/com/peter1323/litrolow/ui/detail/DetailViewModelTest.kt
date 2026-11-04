package com.peter1323.litrolow.ui.detail

import com.peter1323.litrolow.data.api.StationsResponse
import com.peter1323.litrolow.data.model.FuelType
import com.peter1323.litrolow.data.repository.StationRepository
import com.peter1323.litrolow.testutil.FakeApi
import com.peter1323.litrolow.testutil.FakeStationDao
import com.peter1323.litrolow.testutil.MainDispatcherRule
import com.peter1323.litrolow.testutil.stationDto
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Comprueba el detalle de una gasolinera y su historial de precios. */
class DetailViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val api = FakeApi()
    private var clock = 1_000L
    private val repository = StationRepository(api, FakeStationDao(), now = { clock })

    /** Simula una consulta a la API en la que la gasolinera tiene el precio indicado. */
    private suspend fun consult(price95: String) {
        clock += 1_000
        api.response = StationsResponse("OK", listOf(stationDto(price95 = price95)))
        repository.refreshAll()
    }

    /** Cada cambio de precio debe añadir un punto a la gráfica. */
    @Test
    fun `cada cambio de precio anade un punto`() = runTest(mainRule.dispatcher) {
        val viewModel = DetailViewModel("1", repository)
        viewModel.uiState.launchIn(backgroundScope)
        consult("1,500")
        consult("1,450")
        assertEquals(listOf(1.5, 1.45), viewModel.uiState.value.chartPoints.map { it.price })
    }

    /** Consultar sin que cambie el precio no debe llenar la base de datos de puntos repetidos. */
    @Test
    fun `precio sin cambios no repite punto`() = runTest(mainRule.dispatcher) {
        val viewModel = DetailViewModel("1", repository)
        viewModel.uiState.launchIn(backgroundScope)
        consult("1,500")
        consult("1,500")
        assertEquals(1, viewModel.uiState.value.chartPoints.size)
    }

    /** La gráfica omite los registros sin precio para el combustible elegido. */
    @Test
    fun `la grafica usa el combustible elegido`() = runTest(mainRule.dispatcher) {
        val viewModel = DetailViewModel("1", repository)
        viewModel.uiState.launchIn(backgroundScope)
        consult("1,500")
        viewModel.selectChartFuel(FuelType.GASOLINE_98)
        assertEquals(FuelType.GASOLINE_98, viewModel.uiState.value.chartFuel)
        assertTrue(viewModel.uiState.value.chartPoints.isEmpty())
    }

    /** Una gasolinera que ya no existe no debe romper la pantalla. */
    @Test
    fun `gasolinera inexistente deja de cargar`() = runTest(mainRule.dispatcher) {
        val viewModel = DetailViewModel("no-existe", repository)
        viewModel.uiState.launchIn(backgroundScope)
        assertNull(viewModel.uiState.value.station)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }
}
