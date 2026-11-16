package com.peter1323.litrolow.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

/** Comprueba la lectura de los horarios del Ministerio y la búsqueda de nombres con artículo. */
class ScheduleTest {
    private fun open(schedule: String, day: DayOfWeek, time: String) = isOpenAt(schedule, day, LocalTime.parse(time))

    /** Una gasolinera de 24 horas está abierta cualquier día y a cualquier hora. */
    @Test
    fun `abierta 24 horas`() {
        assertEquals(true, open("L-D: 24H", DayOfWeek.SUNDAY, "03:00"))
    }

    /** Cada tramo de días tiene su horario, y un día sin tramo está cerrado. */
    @Test
    fun `horario distinto segun el dia`() {
        val schedule = "L-V: 07:00-22:00; S: 08:00-14:00"
        assertEquals(true, open(schedule, DayOfWeek.MONDAY, "10:00"))
        assertEquals(false, open(schedule, DayOfWeek.MONDAY, "22:30"))
        assertEquals(false, open(schedule, DayOfWeek.SATURDAY, "15:00"))
        assertEquals(false, open(schedule, DayOfWeek.SUNDAY, "10:00"))
    }

    /** Un horario partido cierra a mediodía. */
    @Test
    fun `horario partido`() {
        val schedule = "L: 09:00-14:00 y 15:00-20:00"
        assertEquals(false, open(schedule, DayOfWeek.MONDAY, "14:30"))
        assertEquals(true, open(schedule, DayOfWeek.MONDAY, "16:00"))
    }

    /** Un tramo que acaba antes de empezar pasa de medianoche. */
    @Test
    fun `horario nocturno`() {
        assertEquals(true, open("L-D: 22:00-06:00", DayOfWeek.TUESDAY, "23:00"))
        assertEquals(false, open("L-D: 22:00-06:00", DayOfWeek.TUESDAY, "12:00"))
    }

    /** Ante un texto que no se entiende no se afirma nada. */
    @Test
    fun `horario desconocido`() {
        assertNull(open("Consultar en la estación", DayOfWeek.MONDAY, "10:00"))
        assertNull(open("L-V: mañanas", DayOfWeek.MONDAY, "10:00"))
        assertNull(open("", DayOfWeek.MONDAY, "10:00"))
    }

    /** Canarias va una hora por detrás. */
    @Test
    fun `zona horaria de canarias`() {
        assertEquals("Atlantic/Canary", stationZone("35").id)
        assertEquals("Europe/Madrid", stationZone("37").id)
    }

    /** El Ministerio pone el artículo al final entre paréntesis, pero la gente lo escribe delante. */
    @Test
    fun `busca nombres con articulo`() {
        val station = Station(
            "1", "REPSOL", "CALLE MAYOR 1", "LAGUNAS (LAS)", "CORUÑA (A)", "15", "L-D: 24H", 0.0, 0.0,
            FuelPrices(1.5, null, null, null), 0,
        )
        listOf("las lagunas", "lagunas", "a coruna", "coruna (a)").forEach {
            assertEquals(it, true, station.matches(it))
        }
        assertEquals(false, station.matches("las palmas"))
        assertEquals(listOf(station), listOf(station).comparableZone("A Coruña"))
    }
}
