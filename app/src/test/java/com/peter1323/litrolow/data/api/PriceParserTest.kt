package com.peter1323.litrolow.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Comprueba la conversión de los números de la API, que llegan como texto con coma decimal. */
class PriceParserTest {

    /** El formato normal de la API se convierte sin perder decimales. */
    @Test
    fun `convierte la coma decimal`() {
        assertEquals(1.529, parsePrice("1,529")!!, 0.0)
    }

    /** Los espacios sobrantes no deben impedir la conversión. */
    @Test
    fun `ignora espacios`() {
        assertEquals(1.699, parsePrice(" 1,699 ")!!, 0.0)
    }

    /** Un campo vacío significa que la gasolinera no vende ese combustible. */
    @Test
    fun `texto vacio o nulo devuelve null`() {
        assertNull(parsePrice(""))
        assertNull(parsePrice("   "))
        assertNull(parsePrice(null))
    }

    /** Un texto que no es un número no debe hacer fallar la carga de toda la lista. */
    @Test
    fun `texto no numerico devuelve null`() {
        assertNull(parsePrice("N/D"))
    }

    /** Un precio de cero o negativo no es un precio real. */
    @Test
    fun `precio cero o negativo devuelve null`() {
        assertNull(parsePrice("0,000"))
        assertNull(parsePrice("-1,2"))
    }

    /** Las coordenadas sí admiten negativos: media España tiene longitud negativa. */
    @Test
    fun `coordenada negativa se conserva`() {
        assertEquals(-1.539167, parseDecimal("-1,539167")!!, 0.0)
    }
}
