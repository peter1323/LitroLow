package com.peter1323.litrolow.data.model

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

// Iniciales con las que el Ministerio escribe los días, de lunes a domingo.
private const val DAY_LETTERS = "LMXJVSD"
private const val ALWAYS_OPEN = "24H"
private val CANARY_PROVINCE_IDS = setOf("35", "38")

/** Zona horaria de una gasolinera: Canarias va una hora por detrás del resto de España. */
fun stationZone(provinceId: String): ZoneId =
    ZoneId.of(if (provinceId in CANARY_PROVINCE_IDS) "Atlantic/Canary" else "Europe/Madrid")

/**
 * Indica si una gasolinera está abierta según su horario, escrito como «L-V: 07:00-22:00; S: 08:00-14:00»,
 * «L-D: 24H» o «L: 09:00-14:00 y 15:00-20:00». Devuelve null si el texto no sigue ese formato: es mejor no decir nada
 * que dar por cerrada una gasolinera abierta.
 */
// shortcut: un tramo que pasa de medianoche cuenta para el mismo día en sus dos mitades; afinarlo si aparecen horarios nocturnos distintos según el día.
fun isOpenAt(schedule: String, day: DayOfWeek, time: LocalTime): Boolean? {
    var isOpen = false
    for (segment in schedule.split(';')) {
        val colon = segment.indexOf(':')
        if (colon < 0) return null
        val days = parseDays(segment.substring(0, colon)) ?: return null
        val hours = segment.substring(colon + 1).trim()
        val isOpenInHours = hours.equals(ALWAYS_OPEN, ignoreCase = true) ||
            hours.split(" y ").map { isInRange(it, time) ?: return null }.any { it }
        if (day.value in days && isOpenInHours) isOpen = true
    }
    return isOpen
}

/** Convierte «L-V» o «S» en los números de sus días, de 1 (lunes) a 7 (domingo). */
private fun parseDays(text: String): Set<Int>? {
    val days = text.trim().split('-').map { part ->
        val index = if (part.length == 1) DAY_LETTERS.indexOf(part.uppercase()) else -1
        if (index < 0) return null
        index + 1
    }
    return when (days.size) {
        1 -> setOf(days[0])
        2 -> if (days[0] <= days[1]) (days[0]..days[1]).toSet() else ((days[0]..7) + (1..days[1])).toSet()
        else -> null
    }
}

/** Indica si una hora cae dentro de un tramo como «07:00-22:00»; un tramo que acaba antes de empezar pasa de medianoche. */
private fun isInRange(range: String, time: LocalTime): Boolean? {
    val ends = range.trim().split('-')
    if (ends.size != 2) return null
    return try {
        val start = LocalTime.parse(ends[0].trim())
        val end = LocalTime.parse(ends[1].trim())
        if (start < end) time >= start && time < end else time >= start || time < end
    } catch (_: DateTimeParseException) {
        null
    }
}
