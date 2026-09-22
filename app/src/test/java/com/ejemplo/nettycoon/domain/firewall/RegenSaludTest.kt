package com.ejemplo.nettycoon.domain.firewall

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas JVM de la función pura [RegenSalud.calcular]: fronteras del paso de tiempo, tope a 100,
 * conservación del resto de minutos en el ancla y defensa ante reloj hacia atrás (anti-farmeo).
 */
class RegenSaludTest {

    private val paso = RegenSalud.PASO_MS // 5 min en millis
    private val t0 = 1_000_000_000_000L // instante base arbitrario (epoch millis)

    @Test
    fun `0 minutos no regenera y conserva el ancla`() {
        val r = RegenSalud.calcular(saludActual = 50, ancla = t0, ahora = t0)
        assertEquals(50, r.saludNueva)
        assertEquals(t0, r.anclaNueva)
    }

    @Test
    fun `4 minutos son 0 pasos - sin cambio`() {
        val ahora = t0 + 4 * 60_000L
        val r = RegenSalud.calcular(saludActual = 50, ancla = t0, ahora = ahora)
        assertEquals(50, r.saludNueva)
        assertEquals(t0, r.anclaNueva) // se conserva el ancla, no se pierde el tiempo acumulado
    }

    @Test
    fun `5 minutos suman 5 y avanzan el ancla un paso`() {
        val ahora = t0 + paso
        val r = RegenSalud.calcular(saludActual = 50, ancla = t0, ahora = ahora)
        assertEquals(55, r.saludNueva)
        assertEquals(t0 + paso, r.anclaNueva)
    }

    @Test
    fun `12 minutos suman 10 y conservan el resto de 2 minutos`() {
        val ahora = t0 + 12 * 60_000L // 2 pasos completos + 2 min de resto
        val r = RegenSalud.calcular(saludActual = 50, ancla = t0, ahora = ahora)
        assertEquals(60, r.saludNueva) // 50 + 2*5
        // El ancla avanza SOLO 2 pasos (10 min), no a "ahora": conserva los 2 min sobrantes.
        assertEquals(t0 + 2 * paso, r.anclaNueva)
        // Verificación del resto conservado: 2 min después ya no daría otro paso todavía.
        assertEquals(2 * 60_000L, ahora - r.anclaNueva)
    }

    @Test
    fun `salud ya en 100 no cambia y pone el ancla al dia`() {
        val ahora = t0 + 100 * 60_000L
        val r = RegenSalud.calcular(saludActual = 100, ancla = t0, ahora = ahora)
        assertEquals(100, r.saludNueva)
        assertEquals(ahora, r.anclaNueva)
    }

    @Test
    fun `salud que cruza 100 se recorta al tope`() {
        val ahora = t0 + 100 * 60_000L // 20 pasos -> +100
        val r = RegenSalud.calcular(saludActual = 95, ancla = t0, ahora = ahora)
        assertEquals(100, r.saludNueva) // 95 + 100 -> clamp 100
        assertEquals(ahora, r.anclaNueva) // al tope, no se banquea el resto
    }

    @Test
    fun `reloj hacia atras no regenera nada`() {
        val ahora = t0 - 30 * 60_000L // "ahora" anterior al ancla
        val r = RegenSalud.calcular(saludActual = 40, ancla = t0, ahora = ahora)
        assertEquals(40, r.saludNueva)
        assertEquals(t0, r.anclaNueva)
    }
}
