package com.ejemplo.nettycoon.ui.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas JVM del [ValidadorConfigRed]: lógica pura, sin Android ni corrutinas.
 *
 * Sigue el estilo de `ValidadorReglaTest`: cubre el caso válido (que además devuelve los datos
 * ya convertidos) y los inválidos de IP y de cada puerto.
 */
class ValidadorConfigRedTest {

    @Test
    fun `configuracion valida devuelve los datos convertidos`() {
        val resultado = ValidadorConfigRed.validar("192.168.0.1", "80", "443")

        assertTrue(resultado is ValidacionConfigRed.Valido)
        resultado as ValidacionConfigRed.Valido
        assertEquals("192.168.0.1", resultado.ipRouter)
        assertEquals(80, resultado.puertoLan)
        assertEquals(443, resultado.puertoWan)
    }

    @Test
    fun `ip recorta espacios alrededor`() {
        val resultado = ValidadorConfigRed.validar("  10.0.0.1  ", "1", "65535")

        assertTrue(resultado is ValidacionConfigRed.Valido)
        assertEquals("10.0.0.1", (resultado as ValidacionConfigRed.Valido).ipRouter)
    }

    @Test
    fun `ip vacia es invalida`() {
        val resultado = ValidadorConfigRed.validar("   ", "80", "443")

        assertTrue(resultado is ValidacionConfigRed.Invalido)
        assertEquals("Escribe la IP del router.", (resultado as ValidacionConfigRed.Invalido).mensaje)
    }

    @Test
    fun `ip mal formada es invalida`() {
        // Octeto fuera de rango, número incorrecto de octetos y texto: todos deben fallar.
        listOf("192.168.0.256", "192.168.0", "10.0.0.1.5", "casa").forEach { ip ->
            val resultado = ValidadorConfigRed.validar(ip, "80", "443")
            assertTrue("Debería ser inválida: $ip", resultado is ValidacionConfigRed.Invalido)
        }
    }

    @Test
    fun `puerto lan vacio es invalido`() {
        val resultado = ValidadorConfigRed.validar("192.168.0.1", "", "443")

        assertTrue(resultado is ValidacionConfigRed.Invalido)
        assertEquals("Escribe el puerto LAN.", (resultado as ValidacionConfigRed.Invalido).mensaje)
    }

    @Test
    fun `puerto lan no numerico es invalido`() {
        val resultado = ValidadorConfigRed.validar("192.168.0.1", "abc", "443")

        assertTrue(resultado is ValidacionConfigRed.Invalido)
        assertEquals(
            "El puerto LAN debe ser un número.",
            (resultado as ValidacionConfigRed.Invalido).mensaje,
        )
    }

    @Test
    fun `puertos fuera de rango son invalidos`() {
        // 0 por debajo del mínimo, 65536 por encima del máximo.
        val bajo = ValidadorConfigRed.validar("192.168.0.1", "0", "443")
        assertTrue(bajo is ValidacionConfigRed.Invalido)

        val alto = ValidadorConfigRed.validar("192.168.0.1", "80", "65536")
        assertTrue(alto is ValidacionConfigRed.Invalido)
        assertEquals(
            "El puerto WAN debe estar entre 1 y 65535.",
            (alto as ValidacionConfigRed.Invalido).mensaje,
        )
    }

    @Test
    fun `los limites del rango de puertos son validos`() {
        val resultado = ValidadorConfigRed.validar("192.168.0.1", "1", "65535")

        assertTrue(resultado is ValidacionConfigRed.Valido)
        resultado as ValidacionConfigRed.Valido
        assertEquals(1, resultado.puertoLan)
        assertEquals(65535, resultado.puertoWan)
    }
}
