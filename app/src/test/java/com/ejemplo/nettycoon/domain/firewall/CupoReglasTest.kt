package com.ejemplo.nettycoon.domain.firewall

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests JVM de [cupoDeReglas] (cálculo puro sobre el rango).
 */
class CupoReglasTest {

    @Test
    fun `cada rango tiene su cupo`() {
        assertEquals(CuposReglas.CUPO_APRENDIZ, cupoDeReglas(Rango.APRENDIZ))
        assertEquals(CuposReglas.CUPO_TECNICO, cupoDeReglas(Rango.TECNICO))
        assertEquals(CuposReglas.CUPO_ANALISTA, cupoDeReglas(Rango.ANALISTA))
        assertEquals(CuposReglas.CUPO_EXPERTO, cupoDeReglas(Rango.EXPERTO))
    }

    @Test
    fun `el cupo crece con el rango`() {
        val cupos = Rango.entries.map { cupoDeReglas(it) }
        assertEquals(
            "Subir de rango nunca debe reducir el cupo",
            cupos.sorted(),
            cupos,
        )
        // Y crece de verdad: no son todos iguales.
        assertTrue(cupos.first() < cupos.last())
    }

    @Test
    fun `todo cupo es positivo (siempre se puede automatizar algo)`() {
        Rango.entries.forEach {
            assertTrue("Cupo no positivo en $it", cupoDeReglas(it) > 0)
        }
    }

    @Test
    fun `ni el cupo maximo cubre la mitad de los puertos jugables`() {
        // Invariante de diseno de E5: automatizar NUNCA puede cubrirlo todo, para que siempre
        // quede juego manual donde la economia (salud y tienda) pesa.
        val puertosJugables = 25
        // Se compara duplicando el cupo (y no dividiendo los puertos) para no perder el resto en
        // la división entera: 12 * 2 = 24 < 25, es decir el 48 %.
        assertTrue(
            "El cupo maximo (${CuposReglas.CUPO_EXPERTO}) cubriria demasiado",
            CuposReglas.CUPO_EXPERTO * 2 < puertosJugables,
        )
    }

    @Test
    fun `una familia grande no cabe en los rangos bajos`() {
        // Correo y Archivos tienen 7 puertos: automatizar la familia entera desde el puente no
        // puede ser gratis en los primeros rangos.
        val familiaGrande = MapeoFamilias.puertosDe(MapeoFamilias.CORREO).size
        assertEquals(7, familiaGrande)
        assertTrue(familiaGrande > cupoDeReglas(Rango.APRENDIZ))
        assertTrue(familiaGrande > cupoDeReglas(Rango.TECNICO))
    }

    @Test
    fun `cupoPorPuntaje pasa por el rango`() {
        assertEquals(CuposReglas.CUPO_APRENDIZ, cupoPorPuntaje(0))
        assertEquals(CuposReglas.CUPO_TECNICO, cupoPorPuntaje(UmbralesRango.PUNTAJE_TECNICO))
        assertEquals(CuposReglas.CUPO_ANALISTA, cupoPorPuntaje(UmbralesRango.PUNTAJE_ANALISTA))
        assertEquals(CuposReglas.CUPO_EXPERTO, cupoPorPuntaje(UmbralesRango.PUNTAJE_EXPERTO))
        // Y baja con el puntaje, igual que el rango.
        assertEquals(
            CuposReglas.CUPO_TECNICO,
            cupoPorPuntaje(UmbralesRango.PUNTAJE_ANALISTA - 5),
        )
    }
}
