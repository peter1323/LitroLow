package com.peter1323.litrolow.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Comprueba el semáforo de precios, el cálculo del ahorro y los litros del depósito. */
class PriceComparisonTest {
    private val comparison = PriceComparison(min = 1.40, max = 1.70, average = 1.50)

    /** Cada tercio del rango de la zona corresponde a un color del semáforo. */
    @Test
    fun `clasifica cada precio segun su posicion en la zona`() {
        assertEquals(PriceLevel.CHEAP, comparison.levelOf(1.40))
        assertEquals(PriceLevel.MEDIUM, comparison.levelOf(1.55))
        assertEquals(PriceLevel.HIGH, comparison.levelOf(1.70))
    }

    /** Si todas cuestan lo mismo, ninguna es cara: tampoco se debe dividir entre cero. */
    @Test
    fun `una zona con precios iguales es barata`() {
        assertEquals(PriceLevel.CHEAP, PriceComparison(1.5, 1.5, 1.5).levelOf(1.5))
    }

    /** El ahorro es la diferencia con la media por los litros, y negativo si sale más caro. */
    @Test
    fun `calcula el ahorro por litros frente a la media`() {
        assertEquals(4.0, comparison.savings(1.40, 40), 0.0001)
        assertEquals(-2.0, comparison.savings(1.55, 40), 0.0001)
    }

    /** Con una sola gasolinera no hay nada con lo que comparar. */
    @Test
    fun `necesita al menos dos precios`() {
        assertNull(comparePrices(listOf(1.5)))
        assertEquals(PriceComparison(1.4, 1.6, 1.5), comparePrices(listOf(1.4, 1.6)))
    }

    /** Se acepta cualquier cantidad de 1 a 200 litros, y se rechazan las erratas. */
    @Test
    fun `valida los litros del deposito`() {
        assertEquals(47, parseTankLitres(" 47 "))
        assertEquals(1, parseTankLitres("1"))
        assertEquals(200, parseTankLitres("200"))
        listOf("0", "201", "4000", "abc", "", "47,5").forEach { assertNull(it, parseTankLitres(it)) }
    }
}
