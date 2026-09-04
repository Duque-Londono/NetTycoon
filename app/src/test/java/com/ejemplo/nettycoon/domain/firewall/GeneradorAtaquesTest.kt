package com.ejemplo.nettycoon.domain.firewall

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Unit tests JVM del [GeneradorAtaques]. La inyección de `Random(semilla)` lo hace
 * determinista y reproducible.
 */
class GeneradorAtaquesTest {

    @Test
    fun `misma semilla produce el mismo ataque (determinista)`() {
        val a = GeneradorAtaques(random = Random(42)).generar()
        val b = GeneradorAtaques(random = Random(42)).generar()
        assertEquals(a, b)
    }

    @Test
    fun `el puerto pertenece a la lista configurada`() {
        val puertos = listOf(80, 443)
        val gen = GeneradorAtaques(random = Random(1), puertosComunes = puertos)
        repeat(50) {
            assertTrue(gen.generar().puertoDestino in puertos)
        }
    }

    @Test
    fun `la ip tiene formato ipv4 valido`() {
        val gen = GeneradorAtaques(random = Random(7))
        repeat(50) {
            val octetos = gen.generar().ipAtacante.split(".")
            assertEquals(4, octetos.size)
            octetos.forEach { assertTrue(it.toInt() in 0..255) }
        }
    }

    @Test
    fun `probabilidad 1_0 siempre malicioso y 0_0 nunca`() {
        val siempre = GeneradorAtaques(random = Random(3), probabilidadMalicioso = 1.0)
        val nunca = GeneradorAtaques(random = Random(3), probabilidadMalicioso = 0.0)
        repeat(30) {
            assertTrue(siempre.generar().esMalicioso)
            assertFalse(nunca.generar().esMalicioso)
        }
    }
}
