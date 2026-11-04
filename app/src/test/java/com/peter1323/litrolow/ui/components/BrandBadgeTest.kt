package com.peter1323.litrolow.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/** Comprueba el texto del distintivo con rótulos reales de la API. */
class BrandBadgeTest {

    /** Las marcas cortas se reconocen mejor con su nombre completo. */
    @Test
    fun `marca corta se muestra entera`() {
        assertEquals("BP", brandInitials("BP"))
        assertEquals("Q8", brandInitials("Q8"))
    }

    /** Las marcas largas usan su inicial, saltando los símbolos del principio. */
    @Test
    fun `marca larga usa la primera letra`() {
        assertEquals("R", brandInitials("REPSOL"))
        assertEquals("S", brandInitials("(SIN RÓTULO)"))
    }

    /** Un rótulo vacío no debe romper la tarjeta. */
    @Test
    fun `rotulo vacio usa un signo`() {
        assertEquals("?", brandInitials(""))
    }
}
