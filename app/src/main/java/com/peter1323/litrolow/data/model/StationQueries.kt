package com.peter1323.litrolow.data.model

import java.text.Normalizer
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// Hay más de 3.000 rótulos distintos, casi todos de una sola gasolinera: el filtro solo ofrece los relevantes.
private const val MIN_STATIONS_FOR_BRAND_FILTER = 20
private const val EARTH_RADIUS_KM = 6371.0

/** Devuelve las marcas con suficientes gasolineras para ofrecerlas en el filtro, de más a menos frecuentes. */
fun popularBrands(stations: List<Station>): List<String> =
    stations.groupingBy { it.brand }.eachCount()
        .filterValues { it >= MIN_STATIONS_FOR_BRAND_FILTER }
        .entries.sortedByDescending { it.value }
        .map { it.key }

/** Quita tildes y mayúsculas para que buscar "malaga" encuentre "MÁLAGA". */
fun normalizeText(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase()

/** Indica si la gasolinera coincide con un texto ya normalizado por localidad, dirección, provincia o marca. */
fun Station.matches(normalizedQuery: String): Boolean =
    listOf(locality, address, province, brand).any { normalizeText(it).contains(normalizedQuery) }

/**
 * Aplica los filtros y ordena de más barata a más cara. Las gasolineras sin ese combustible
 * se excluyen porque no se pueden comparar.
 */
fun List<Station>.filterAndSort(brand: String?, fuel: FuelType, query: String): List<Station> {
    val normalizedQuery = normalizeText(query.trim())
    return filter { station ->
        station.prices.of(fuel) != null &&
            (brand == null || station.brand == brand) &&
            (normalizedQuery.isEmpty() || station.matches(normalizedQuery))
    }.sortedBy { it.prices.of(fuel) }
}

/** Calcula la distancia en kilómetros entre dos puntos con la fórmula del semiverseno. */
fun distanceKm(a: GeoPoint, b: GeoPoint): Double {
    val deltaLat = Math.toRadians(b.latitude - a.latitude)
    val deltaLon = Math.toRadians(b.longitude - a.longitude)
    val h = sin(deltaLat / 2).pow(2) +
        cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sin(deltaLon / 2).pow(2)
    return 2 * EARTH_RADIUS_KM * asin(sqrt(h))
}
