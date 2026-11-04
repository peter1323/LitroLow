package com.peter1323.litrolow.data.model

import kotlin.math.round

/** Litros que se suponen hasta que el usuario indica los que suele echar; el texto siempre muestra la cifra usada. */
const val DEFAULT_TANK_LITRES = 40

const val CENTS_PER_EURO = 100

private const val PRICE_PRECISION = 1000.0

private const val MIN_TANK_LITRES = 1

// Cubre desde motos hasta furgonetas grandes y rechaza erratas como 4000.
private const val MAX_TANK_LITRES = 200
private const val CHEAP_LIMIT = 1.0 / 3
private const val MEDIUM_LIMIT = 2.0 / 3

/** Lo bueno que es un precio frente a los demás de su zona. */
enum class PriceLevel { CHEAP, MEDIUM, HIGH }

/** Precios de referencia de una zona, para valorar cada gasolinera frente a las demás. */
data class PriceComparison(val min: Double, val max: Double, val average: Double) {
    /** Sitúa un precio en la zona: 0 es el más barato y 1 el más caro. */
    fun position(price: Double): Float =
        if (max == min) 0f else ((price - min) / (max - min)).coerceIn(0.0, 1.0).toFloat()

    /** Clasifica un precio dividiendo el rango de la zona en tres partes iguales. */
    fun levelOf(price: Double): PriceLevel {
        val position = position(price)
        return when {
            position < CHEAP_LIMIT -> PriceLevel.CHEAP
            position < MEDIUM_LIMIT -> PriceLevel.MEDIUM
            else -> PriceLevel.HIGH
        }
    }

    /** Euros que se ahorran frente a la media al echar esos litros; es negativo si sale más caro. */
    fun savings(price: Double, litres: Int): Double = (average - price) * litres

    /** Céntimos por litro que un precio está por debajo de la media; es negativo si está por encima. */
    fun centsBelowAverage(price: Double): Double = (average - price) * CENTS_PER_EURO
}

/** Calcula las referencias de una zona; con menos de dos precios no hay nada que comparar. */
fun comparePrices(prices: List<Double>): PriceComparison? =
    if (prices.size < 2) null else PriceComparison(prices.min(), prices.max(), roundToPriceDecimals(prices.average()))

/** Redondea a tres decimales, como se muestra el precio, para que el ahorro cuadre al calcularlo con la media que se ve. */
private fun roundToPriceDecimals(price: Double): Double = round(price * PRICE_PRECISION) / PRICE_PRECISION

/** Convierte lo escrito en los litros del depósito, o devuelve null si no es una cantidad válida. */
fun parseTankLitres(text: String): Int? =
    text.trim().toIntOrNull()?.takeIf { it in MIN_TANK_LITRES..MAX_TANK_LITRES }
