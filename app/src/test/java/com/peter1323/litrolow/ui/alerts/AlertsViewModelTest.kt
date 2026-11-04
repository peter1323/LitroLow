package com.peter1323.litrolow.ui.alerts

import com.peter1323.litrolow.data.api.StationsResponse
import com.peter1323.litrolow.data.model.FuelType
import com.peter1323.litrolow.data.model.LoadError
import com.peter1323.litrolow.data.model.PriceAlert
import com.peter1323.litrolow.data.repository.AlertRepository
import com.peter1323.litrolow.data.repository.AlertSaveResult
import com.peter1323.litrolow.data.repository.StationRepository
import com.peter1323.litrolow.testutil.FakeAlertDao
import com.peter1323.litrolow.testutil.FakeApi
import com.peter1323.litrolow.testutil.FakeStationDao
import com.peter1323.litrolow.testutil.MainDispatcherRule
import com.peter1323.litrolow.testutil.station
import com.peter1323.litrolow.testutil.stationDto
import com.peter1323.litrolow.ui.components.formatAlertPrice
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
        alertDao.upsert(PriceAlert(stationId = "1", stationName = "REPSOL", provinceId = "28", fuel = FuelType.GASOLINE_95, maxPrice = 1.5))

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
        val viewModel = AlertsViewModel(alerts, stations)
        detail.uiState.launchIn(backgroundScope)
        viewModel.uiState.launchIn(backgroundScope)

        assertEquals(AlertSaveResult.SAVED, detail.createAlert(station("1"), FuelType.GASOLINE_95, "1,45"))
        val alert = viewModel.uiState.value.alerts.single()
        assertEquals(1.45, alert.maxPrice, 0.0)
        assertEquals("REPSOL (MADRID)", alert.stationName)

        viewModel.delete(alert.id)
        assertTrue(viewModel.uiState.value.alerts.isEmpty())
    }

    /** La pantalla debe conocer el precio actual para decir si la alerta está cumplida. */
    @Test
    fun `muestra el precio actual y las cumplidas`() = runTest(mainRule.dispatcher) {
        addAlert()
        checkWithPrice("1,400")
        val viewModel = AlertsViewModel(alerts, stations)
        viewModel.uiState.launchIn(backgroundScope)
        val state = viewModel.uiState.value
        assertEquals(1.4, state.currentPrices.getValue(state.alerts.single().id), 0.0001)
        assertEquals(1, state.metCount)
    }

    /** Editar debe cambiar los datos, conservar la alerta y rearmarla; un precio inválido no debe tocarla. */
    @Test
    fun `editar una alerta`() = runTest(mainRule.dispatcher) {
        addAlert()
        checkWithPrice("1,400")
        val viewModel = AlertsViewModel(alerts, stations)
        viewModel.uiState.launchIn(backgroundScope)
        val original = viewModel.uiState.value.alerts.single()
        assertTrue(original.notified)

        assertEquals(AlertSaveResult.INVALID_PRICE, viewModel.update(original, FuelType.DIESEL, "65"))
        assertEquals(original, viewModel.uiState.value.alerts.single())

        assertEquals(AlertSaveResult.SAVED, viewModel.update(original, FuelType.DIESEL, "1,3"))
        val edited = viewModel.uiState.value.alerts.single()
        assertEquals(original.id, edited.id)
        assertEquals(FuelType.DIESEL, edited.fuel)
        assertEquals(1.3, edited.maxPrice, 0.0)
        assertFalse(edited.notified)
    }

    /** Misma gasolinera, combustible y precio no debe repetirse, escrito como se escriba; con otro precio sí vale. */
    @Test
    fun `no permite alertas duplicadas`() = runTest(mainRule.dispatcher) {
        val station = station("1")
        assertEquals(AlertSaveResult.SAVED, alerts.create(station, FuelType.GASOLINE_95, "1,50"))
        assertEquals(AlertSaveResult.DUPLICATE, alerts.create(station, FuelType.GASOLINE_95, "1,5"))
        assertEquals(AlertSaveResult.SAVED, alerts.create(station, FuelType.GASOLINE_95, "1,40"))
        assertEquals(AlertSaveResult.SAVED, alerts.create(station, FuelType.DIESEL, "1,50"))
        assertEquals(AlertSaveResult.SAVED, alerts.create(station("2"), FuelType.GASOLINE_95, "1,50"))
        assertEquals(4, alertDao.alerts.value.size)
    }

    /** Al editar, una alerta no choca consigo misma, pero no puede convertirse en copia de otra. */
    @Test
    fun `editar no permite crear un duplicado`() = runTest(mainRule.dispatcher) {
        alerts.create(station("1"), FuelType.GASOLINE_95, "1,50")
        alerts.create(station("1"), FuelType.GASOLINE_95, "1,40")
        val second = alertDao.alerts.value.last()
        assertEquals(AlertSaveResult.SAVED, alerts.update(second, second.fuel, "1,40"))
        assertEquals(AlertSaveResult.DUPLICATE, alerts.update(second, second.fuel, "1,50"))
        assertEquals(1.4, alertDao.alerts.value.last().maxPrice, 0.0)
    }

    /** Un euro no debe leerse como mil: "1,000" parece un millar en español. */
    @Test
    fun `el precio de la alerta no parece un millar`() {
        assertEquals("1,00", formatAlertPrice(1.0))
        assertEquals("1,45", formatAlertPrice(1.45))
        assertEquals("1,459", formatAlertPrice(1.459))
    }

    /** Un precio vacío o sin sentido no debe crear una alerta que nunca avisaría. */
    @Test
    fun `rechaza un precio no valido`() = runTest(mainRule.dispatcher) {
        stations.refreshAll()
        val detail = DetailViewModel("1", stations, alerts)
        detail.uiState.launchIn(backgroundScope)
        assertEquals(AlertSaveResult.INVALID_PRICE, detail.createAlert(station("1"), FuelType.GASOLINE_95, ""))
        assertEquals(AlertSaveResult.INVALID_PRICE, detail.createAlert(station("1"), FuelType.GASOLINE_95, "abc"))
        assertEquals(AlertSaveResult.INVALID_PRICE, detail.createAlert(station("1"), FuelType.GASOLINE_95, "0"))
        assertEquals(AlertSaveResult.INVALID_PRICE, detail.createAlert(station("1"), FuelType.GASOLINE_95, "65"))
        assertEquals(AlertSaveResult.INVALID_PRICE, detail.createAlert(station("1"), FuelType.GASOLINE_95, "10,01"))
        assertTrue(alertDao.alerts.value.isEmpty())
    }
}
