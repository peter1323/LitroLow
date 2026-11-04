package com.peter1323.litrolow.ui.alerts

import com.peter1323.litrolow.data.api.StationsResponse
import com.peter1323.litrolow.data.model.FuelType
import com.peter1323.litrolow.data.model.LoadError
import com.peter1323.litrolow.data.model.PriceAlert
import com.peter1323.litrolow.data.repository.AlertRepository
import com.peter1323.litrolow.data.repository.StationRepository
import com.peter1323.litrolow.testutil.FakeAlertDao
import com.peter1323.litrolow.testutil.FakeApi
import com.peter1323.litrolow.testutil.FakeStationDao
import com.peter1323.litrolow.testutil.MainDispatcherRule
import com.peter1323.litrolow.testutil.stationDto
import com.peter1323.litrolow.ui.detail.DetailViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/** Comprueba la creación y el borrado de alertas y cuándo deben avisar. */
class AlertsViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val api = FakeApi()
    private val stationDao = FakeStationDao()
    private val alertDao = FakeAlertDao()
    private val stations = StationRepository(api, stationDao)
    private val alerts = AlertRepository(alertDao, stations)

    /** Hace que la API devuelva el precio indicado y ejecuta una comprobación de alertas. */
    private suspend fun checkWithPrice(price: String): Int {
        api.response = StationsResponse("OK", listOf(stationDto(price95 = price)))
        return alerts.check().triggered.size
    }

    /** Crea una alerta de Gasolina 95 con máximo de 1,50 €. */
    private suspend fun addAlert() =
        alerts.add(PriceAlert(stationId = "1", stationName = "REPSOL", provinceId = "28", fuel = FuelType.GASOLINE_95, maxPrice = 1.5))

    /** Debe avisar solo al bajar del máximo y no repetir el aviso en cada comprobación. */
    @Test
    fun `avisa una vez y se rearma al volver a subir`() = runTest(mainRule.dispatcher) {
        addAlert()
        assertEquals(0, checkWithPrice("1,600"))
        assertEquals(1, checkWithPrice("1,450"))
        assertEquals(0, checkWithPrice("1,400"))
        assertEquals(0, checkWithPrice("1,700"))
        assertEquals(1, checkWithPrice("1,500"))
    }

    /** Un fallo de red no debe contar como aviso; el error llega al worker para que reintente. */
    @Test
    fun `un fallo de red devuelve el error`() = runTest(mainRule.dispatcher) {
        addAlert()
        api.failure = IOException()
        val result = alerts.check()
        assertEquals(LoadError.NO_INTERNET, result.error)
        assertTrue(result.triggered.isEmpty())
    }

    /** Desde el detalle se crea la alerta aceptando coma o punto, y aparece en la lista hasta que se borra. */
    @Test
    fun `crear y borrar una alerta`() = runTest(mainRule.dispatcher) {
        stations.refreshAll()
        val detail = DetailViewModel("1", stations, alerts)
        val viewModel = AlertsViewModel(alerts)
        detail.uiState.launchIn(backgroundScope)
        viewModel.uiState.launchIn(backgroundScope)

        assertTrue(detail.createAlert(FuelType.GASOLINE_95, "1,45"))
        val alert = viewModel.uiState.value.alerts.single()
        assertEquals(1.45, alert.maxPrice, 0.0)
        assertEquals("REPSOL ()", alert.stationName)

        viewModel.delete(alert.id)
        assertTrue(viewModel.uiState.value.alerts.isEmpty())
    }

    /** Un precio vacío o sin sentido no debe crear una alerta que nunca avisaría. */
    @Test
    fun `rechaza un precio no valido`() = runTest(mainRule.dispatcher) {
        stations.refreshAll()
        val detail = DetailViewModel("1", stations, alerts)
        detail.uiState.launchIn(backgroundScope)
        assertFalse(detail.createAlert(FuelType.GASOLINE_95, ""))
        assertFalse(detail.createAlert(FuelType.GASOLINE_95, "abc"))
        assertFalse(detail.createAlert(FuelType.GASOLINE_95, "0"))
        assertFalse(detail.createAlert(FuelType.GASOLINE_95, "65"))
        assertFalse(detail.createAlert(FuelType.GASOLINE_95, "10,01"))
        assertTrue(alertDao.alerts.value.isEmpty())
    }
}
