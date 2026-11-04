package com.peter1323.litrolow.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.peter1323.litrolow.data.model.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume

private const val LOCATION_TIMEOUT_MS = 15_000L

/** Obtiene puntos geográficos con los servicios del propio Android, sin claves ni servicios de pago. */
class LocationRepository(private val context: Context) {
    private val locationManager = context.getSystemService(LocationManager::class.java)

    /** Devuelve la ubicación actual o, si tarda demasiado, la última conocida. Null si no hay ninguna. */
    // La interfaz solo llama aquí después de que el usuario haya concedido el permiso.
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): GeoPoint? {
        val provider = enabledProvider() ?: return null
        val location = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }
                LocationManagerCompat.getCurrentLocation(
                    locationManager,
                    provider,
                    signal,
                    ContextCompat.getMainExecutor(context),
                ) { continuation.resume(it) }
            }
        } ?: locationManager.getLastKnownLocation(provider)
        return location?.let { GeoPoint(it.latitude, it.longitude) }
    }

    /** Elige la red si está disponible porque responde mucho antes que el GPS y basta para buscar gasolineras cerca. */
    private fun enabledProvider(): String? =
        listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .firstOrNull { locationManager.isProviderEnabled(it) }

    /**
     * Convierte una dirección escrita en coordenadas. Devuelve null si no la encuentra o si el móvil
     * no tiene servicio de geocodificación, que no está garantizado en todos los dispositivos.
     */
    suspend fun geocode(address: String): GeoPoint? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.forLanguageTag("es-ES"))
        val query = "$address, España"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocodeAsync(geocoder, query)
            } else {
                geocodeBlocking(geocoder, query)
            }
        } catch (_: IOException) {
            null
        }
    }

    /** Usa la API asíncrona de Android 13 o superior. */
    @SuppressLint("NewApi")
    private suspend fun geocodeAsync(geocoder: Geocoder, query: String): GeoPoint? =
        suspendCancellableCoroutine { continuation ->
            geocoder.getFromLocationName(
                query,
                1,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<android.location.Address>) {
                        continuation.resume(addresses.firstOrNull()?.let { GeoPoint(it.latitude, it.longitude) })
                    }

                    override fun onError(errorMessage: String?) = continuation.resume(null)
                },
            )
        }

    /** En versiones anteriores la única API bloquea el hilo, así que se ejecuta en segundo plano. */
    @Suppress("DEPRECATION")
    private suspend fun geocodeBlocking(geocoder: Geocoder, query: String): GeoPoint? =
        withContext(Dispatchers.IO) {
            geocoder.getFromLocationName(query, 1)?.firstOrNull()?.let { GeoPoint(it.latitude, it.longitude) }
        }
}
