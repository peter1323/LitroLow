package com.peter1323.litrolow.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.peter1323.litrolow.R
import com.peter1323.litrolow.data.model.GeoPoint
import com.peter1323.litrolow.data.model.PriceLevel
import com.peter1323.litrolow.data.model.Station
import com.peter1323.litrolow.ui.components.formatDecimal
import com.peter1323.litrolow.ui.theme.colors
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import java.io.File
import org.osmdroid.util.GeoPoint as OsmPoint

private const val DEFAULT_ZOOM = 13.0
private const val TEXT_SP = 12f
private const val CHEAPEST_TEXT_SP = 15f
private const val PILL_PADDING_H = 8f
private const val PILL_PADDING_V = 5f
private const val PILL_BORDER = 2f

// Oscurece el mapa invirtiendo el brillo y girando el tono 180 grados: invertir sin más volvería el mar marrón y los bosques morados.
private val DARK_MAP_FILTER = ColorMatrixColorFilter(
    ColorMatrix(
        floatArrayOf(
            0.574f, -1.43f, -0.144f, 0f, 255f,
            -0.426f, -0.43f, -0.144f, 0f, 255f,
            -0.426f, -1.43f, 0.856f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
)

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
        mapView.overlayManager.tilesOverlay.setColorFilter(if (isDark) DARK_MAP_FILTER else null)
        // La atribución de OpenStreetMap es obligatoria y debe poder leerse sobre cualquier fondo.
        mapView.overlays.filterIsInstance<CopyrightOverlay>().forEach {
            it.setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        }
        mapView.invalidate()
    }
    LaunchedEffect(state.nearby, state.center) {
        showMarkers(mapView, state, onStationClick)
    }
    // Sin recortar, el mapa puede dibujarse encima de los controles vecinos.
    AndroidView({ mapView }, modifier.clipToBounds())
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

/** Sustituye los marcadores. Se añaden de más cara a más barata para que las baratas queden por encima. */
private fun showMarkers(mapView: MapView, state: MapUiState, onStationClick: (String) -> Unit) {
    mapView.overlays.removeAll { it is Marker }
    state.center?.let { mapView.overlays.add(centerMarker(mapView, it)) }
    state.nearby.asReversed().forEach { station ->
        val icon = pricePill(mapView.context, station, state)
        mapView.overlays.add(stationMarker(mapView, station, icon, onStationClick))
    }
    mapView.invalidate()
}

/** Dibuja la etiqueta de un marcador con su precio y el color del semáforo; la más barata va mayor y con borde. */
private fun pricePill(context: Context, station: Station, state: MapUiState): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val isCheapest = station == state.cheapest
    val price = requireNotNull(station.prices.of(state.fuel)) { "Las gasolineras del mapa siempre tienen este combustible" }
    val (background, content) = (state.comparison?.levelOf(price) ?: PriceLevel.CHEAP).colors
    val text = formatDecimal(price, 3)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = (if (isCheapest) CHEAPEST_TEXT_SP else TEXT_SP) * density
        typeface = Typeface.DEFAULT_BOLD
    }
    val width = paint.measureText(text) + 2 * PILL_PADDING_H * density
    val height = paint.textSize + 2 * PILL_PADDING_V * density
    val bitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val border = PILL_BORDER * density
    val pill = RectF(border, border, bitmap.width - border, bitmap.height - border)
    val radius = bitmap.height / 2f
    canvas.drawRoundRect(pill, radius, radius, paint.apply { color = background.toArgb() })
    if (isCheapest) {
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = border
            color = content.toArgb()
        }
        canvas.drawRoundRect(pill, radius, radius, borderPaint)
    }
    paint.color = content.toArgb()
    val baseline = bitmap.height / 2f - (paint.ascent() + paint.descent()) / 2
    canvas.drawText(text, (bitmap.width - paint.measureText(text)) / 2, baseline, paint)
    return BitmapDrawable(context.resources, bitmap)
}

/** Marcador del punto buscado, centrado y sin acción al pulsarlo. */
private fun centerMarker(mapView: MapView, point: GeoPoint) = Marker(mapView).apply {
    position = point.toOsm()
    icon = ContextCompat.getDrawable(mapView.context, R.drawable.ic_map_center)
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
    setOnMarkerClickListener { _, _ -> true }
}

/** Marcador de una gasolinera, que abre su detalle al pulsarlo. */
private fun stationMarker(mapView: MapView, station: Station, pill: BitmapDrawable, onClick: (String) -> Unit) =
    Marker(mapView).apply {
        position = OsmPoint(station.latitude, station.longitude)
        icon = pill
        title = station.brand
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        setOnMarkerClickListener { _, _ ->
            onClick(station.id)
            true
        }
    }

/** Convierte un punto de la app al tipo que usa osmdroid. */
private fun GeoPoint.toOsm() = OsmPoint(latitude, longitude)
