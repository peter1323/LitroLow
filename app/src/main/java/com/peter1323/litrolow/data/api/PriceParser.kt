package com.peter1323.litrolow.data.api

/**
 * Convierte un número de la API, escrito con coma decimal ("1,529"), a Double.
 * Devuelve null si el texto está vacío o no es un número, porque la API deja campos en blanco.
 */
fun parseDecimal(text: String?): Double? = text?.trim()?.replace(',', '.')?.toDoubleOrNull()

/** Como [parseDecimal], pero descarta ceros y negativos: un precio así significa que no hay dato válido. */
fun parsePrice(text: String?): Double? = parseDecimal(text)?.takeIf { it > 0 }
