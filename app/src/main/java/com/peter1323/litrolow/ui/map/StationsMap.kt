package com.peter1323.litrolow.ui.map

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.peter1323.litrolow.R
import com.peter1323.litrolow.data.model.GeoPoint
import com.peter1323.litrolow.data.model.Station
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import java.io.File
import org.osmdroid.util.GeoPoint as OsmPoint

private const val DEFAULT_ZOOM = 13.0

/** Mapa de OpenStreetMap con el punto buscado y un marcador por gasolinera; la más barata va destacada. */
@Composable
fun StationsMap(state: MapUiState, isDark: Boolean, onStationClick: (String) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val mapView = remember { createMapView(context) }
    BindToLifecycle(mapView)

    LaunchedEffect(state.center) {
        state.center?.let { mapView.controller.setCenter(it.toOsm()) }
    }
    LaunchedEffect(isDark) {
        // Invierte los colores del mapa para que no deslumbre en el tema oscuro.
        mapView.overlayManager.tilesOverlay.setColorFilter(if (isDark) TilesOverlay.INVERT_COLORS else null)
        mapView.invalidate()
    }
    LaunchedEffect(state.nearby, state.center) {
        showMarkers(mapView, state, onStationClick)
    }
    AndroidView({ mapView }, modifier)
}

/** Crea el mapa. La política de OpenStreetMap exige identificar la app y mostrar la atribución. */
private fun createMapView(context: Context): MapView {
    Configuration.getInstance().apply {
        userAgentValue = context.packageName
        // Usa la caché privada para no necesitar permisos de almacenamiento.
        osmdroidBasePath = context.cacheDir
        osmdroidTileCache = File(context.cacheDir, "tiles")
    }
    return MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        controller.setZoom(DEFAULT_ZOOM)
        overlays.add(CopyrightOverlay(context))
    }
}

/** Pausa y reanuda el mapa con la pantalla para no gastar batería ni datos en segundo plano. */
@Composable
private fun BindToLifecycle(mapView: MapView) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }
}

/** Sustituye los marcadores. La más barata se añade la última para que quede por encima de las demás. */
private fun showMarkers(mapView: MapView, state: MapUiState, onStationClick: (String) -> Unit) {
    mapView.overlays.removeAll { it is Marker }
    state.center?.let { mapView.overlays.add(centerMarker(mapView, it)) }
    state.nearby.drop(1).forEach { mapView.overlays.add(stationMarker(mapView, it, R.drawable.ic_map_pin, onStationClick)) }
    state.cheapest?.let { mapView.overlays.add(stationMarker(mapView, it, R.drawable.ic_map_pin_cheapest, onStationClick)) }
    mapView.invalidate()
}

/** Marcador del punto buscado, centrado y sin acción al pulsarlo. */
private fun centerMarker(mapView: MapView, point: GeoPoint) = Marker(mapView).apply {
    position = point.toOsm()
    icon = ContextCompat.getDrawable(mapView.context, R.drawable.ic_map_center)
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
    setOnMarkerClickListener { _, _ -> true }
}

/** Marcador de una gasolinera, que abre su detalle al pulsarlo. */
private fun stationMarker(mapView: MapView, station: Station, iconRes: Int, onClick: (String) -> Unit) =
    Marker(mapView).apply {
        position = OsmPoint(station.latitude, station.longitude)
        icon = ContextCompat.getDrawable(mapView.context, iconRes)
        title = station.brand
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        setOnMarkerClickListener { _, _ ->
            onClick(station.id)
            true
        }
    }

/** Convierte un punto de la app al tipo que usa osmdroid. */
private fun GeoPoint.toOsm() = OsmPoint(latitude, longitude)
