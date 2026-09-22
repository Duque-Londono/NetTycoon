package com.ejemplo.nettycoon.domain.firewall

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas JVM de [TiempoRegen]: tiempo restante hasta el próximo +5 y detección de paso pendiente,
 * derivados del mismo ancla que [RegenSalud].
 */
class TiempoRegenTest {

    private val paso = RegenSalud.PASO_MS // 5 min
    private val t0 = 1_000_000_000_000L

    @Test
    fun `recien anclado faltan 5 minutos`() {
        assertEquals(paso, TiempoRegen.restanteMs(50, ancla = t0, ahora = t0))
    }

    @Test
    fun `a 4 min 59 s falta 1 segundo`() {
        val ahora = t0 + (4 * 60 + 59) * 1000L
        assertEquals(1000L, TiempoRegen.restanteMs(50, ancla = t0, ahora = ahora))
    }

    @Test
    fun `a mitad de tramo faltan 2 min 30 s`() {
        val ahora = t0 + 150 * 1000L // 2:30
        assertEquals(150 * 1000L, TiempoRegen.restanteMs(50, ancla = t0, ahora = ahora))
    }

    @Test
    fun `con salud llena no hay contador`() {
        assertNull(TiempoRegen.restanteMs(100, ancla = t0, ahora = t0 + paso))
    }

    @Test
    fun `si el tramo ya se cumplio el restante es un tramo completo`() {
        // Justo al cumplirse un tramo (5:00): el restante vuelve a 5:00 y hay paso pendiente.
        assertEquals(paso, TiempoRegen.restanteMs(50, ancla = t0, ahora = t0 + paso))
        assertTrue(TiempoRegen.pasoPendiente(50, ancla = t0, ahora = t0 + paso))
    }

    @Test
    fun `restante conserva el resto cuando pasaron varios tramos sin aplicar`() {
        val ahora = t0 + 12 * 60 * 1000L // 12 min: 2 tramos + 2 min de resto
        // Falta completar el tramo en curso: 5:00 - 2:00 = 3:00.
        assertEquals(3 * 60 * 1000L, TiempoRegen.restanteMs(50, ancla = t0, ahora = ahora))
    }

    @Test
    fun `sin tramo cumplido no hay paso pendiente`() {
        val ahora = t0 + (4 * 60 + 59) * 1000L
        assertFalse(TiempoRegen.pasoPendiente(50, ancla = t0, ahora = ahora))
    }

    @Test
    fun `salud llena nunca tiene paso pendiente`() {
        assertFalse(TiempoRegen.pasoPendiente(100, ancla = t0, ahora = t0 + 10 * paso))
    }

    @Test
    fun `reloj hacia atras muestra el tramo completo`() {
        val ahora = t0 - 30 * 60 * 1000L
        assertEquals(paso, TiempoRegen.restanteMs(40, ancla = t0, ahora = ahora))
        assertFalse(TiempoRegen.pasoPendiente(40, ancla = t0, ahora = ahora))
    }
}
