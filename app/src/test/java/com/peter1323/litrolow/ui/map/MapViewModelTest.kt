package com.peter1323.litrolow.ui.map

import com.peter1323.litrolow.data.model.FuelType
import com.peter1323.litrolow.data.model.GeoPoint
import com.peter1323.litrolow.data.model.Station
import com.peter1323.litrolow.testutil.MainDispatcherRule
import com.peter1323.litrolow.testutil.station
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Comprueba la búsqueda de gasolineras cercanas, con ubicación y con dirección. */
class MapViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val here = GeoPoint(40.0, -3.0)

    /** Crea el ViewModel con la ubicación y la geocodificación simuladas. */
    private fun viewModel(
        stations: List<Station>,
        location: GeoPoint? = here,
        geocoded: GeoPoint? = here,
    ) = MapViewModel(flowOf(stations), { location }, { geocoded }, mainRule.dispatcher)

    /** Solo cuentan las gasolineras dentro del radio, y la primera debe ser la más barata. */
    @Test
    fun `destaca la mas barata dentro del radio`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel(
            listOf(
                station("cara", price95 = 1.8),
                station("barata", price95 = 1.3),
                station("lejana", latitude = 42.0, price95 = 1.0),
            ),
        )
        viewModel.uiState.launchIn(backgroundScope)
        viewModel.useCurrentLocation()
        assertEquals(listOf("barata", "cara"), viewModel.uiState.value.nearby.map { it.id })
        assertEquals("barata", viewModel.uiState.value.cheapest?.id)
    }

    /** Sin dar la ubicación, escribir una dirección debe mostrar igualmente las gasolineras cercanas. */
    @Test
    fun `funciona con una direccion sin ubicacion`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel(listOf(station("1")), location = null)
        viewModel.uiState.launchIn(backgroundScope)
        viewModel.onPermissionDenied()
        assertEquals(MapProblem.PERMISSION_DENIED, viewModel.uiState.value.problem)

        viewModel.searchAddress("Calle Mayor 1, Madrid")
        assertEquals(here, viewModel.uiState.value.center)
        assertEquals(listOf("1"), viewModel.uiState.value.nearby.map { it.id })
        assertNull(viewModel.uiState.value.problem)
    }

    /** Si el móvil no tiene geocodificador, la localidad se busca en los datos de gasolineras. */
    @Test
    fun `sin geocodificador usa las gasolineras de la localidad`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel(
            listOf(station("1", locality = "MÁLAGA", latitude = 36.7, longitude = -4.4)),
            geocoded = null,
        )
        viewModel.uiState.launchIn(backgroundScope)
        viewModel.searchAddress("malaga")
        assertEquals(GeoPoint(36.7, -4.4), viewModel.uiState.value.center)
        assertEquals(listOf("1"), viewModel.uiState.value.nearby.map { it.id })
    }

    /** Una dirección que no existe debe explicarse sin mover el mapa. */
    @Test
    fun `direccion desconocida muestra el problema`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel(listOf(station("1")), geocoded = null)
        viewModel.uiState.launchIn(backgroundScope)
        viewModel.searchAddress("zzzz")
        assertEquals(MapProblem.ADDRESS_NOT_FOUND, viewModel.uiState.value.problem)
        assertNull(viewModel.uiState.value.center)
    }

    /** Si no se puede obtener la ubicación, debe avisar en lugar de quedarse cargando. */
    @Test
    fun `ubicacion no disponible muestra el problema`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel(listOf(station("1")), location = null)
        viewModel.uiState.launchIn(backgroundScope)
        viewModel.useCurrentLocation()
        assertEquals(MapProblem.LOCATION_UNAVAILABLE, viewModel.uiState.value.problem)
        assertEquals(false, viewModel.uiState.value.isSearching)
    }

    /** Cambiar de combustible excluye las gasolineras que no lo venden. */
    @Test
    fun `cambiar de combustible recalcula las cercanas`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel(listOf(station("1")))
        viewModel.uiState.launchIn(backgroundScope)
        viewModel.useCurrentLocation()
        viewModel.selectFuel(FuelType.GASOLINE_98)
        assertTrue(viewModel.uiState.value.nearby.isEmpty())
    }
}
