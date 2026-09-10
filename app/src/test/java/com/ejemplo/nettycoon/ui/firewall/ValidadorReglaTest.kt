package com.ejemplo.nettycoon.ui.firewall

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de [ValidadorRegla]. Lógica pura: sin corrutinas, sin Android, sin Room.
 */
class ValidadorReglaTest {

    @Test
    fun `puerto valido sin ip devuelve comodin null`() {
        val resultado = ValidadorRegla.validar(puertoTexto = "443", ipTexto = "")

        assertEquals(ValidacionRegla.Valido(puerto = 443, ip = null), resultado)
    }

    @Test
    fun `ip con solo espacios se trata como vacia`() {
        val resultado = ValidadorRegla.validar(puertoTexto = "80", ipTexto = "   ")

        assertEquals(ValidacionRegla.Valido(puerto = 80, ip = null), resultado)
    }

    @Test
    fun `puerto e ip validos se devuelven ya convertidos y sin espacios`() {
        val resultado = ValidadorRegla.validar(puertoTexto = " 22 ", ipTexto = " 203.0.113.9 ")

        assertEquals(ValidacionRegla.Valido(puerto = 22, ip = "203.0.113.9"), resultado)
    }

    @Test
    fun `puerto vacio es invalido`() {
        assertTrue(ValidadorRegla.validar("", "") is ValidacionRegla.Invalido)
    }

    @Test
    fun `puerto no numerico es invalido`() {
        assertTrue(ValidadorRegla.validar("http", "") is ValidacionRegla.Invalido)
    }

    @Test
    fun `puerto fuera de rango es invalido en ambos extremos`() {
        assertTrue(ValidadorRegla.validar("0", "") is ValidacionRegla.Invalido)
        assertTrue(ValidadorRegla.validar("65536", "") is ValidacionRegla.Invalido)
    }

    @Test
    fun `puertos limite del rango son validos`() {
        assertEquals(
            ValidacionRegla.Valido(puerto = 1, ip = null),
            ValidadorRegla.validar("1", ""),
        )
        assertEquals(
            ValidacionRegla.Valido(puerto = 65535, ip = null),
            ValidadorRegla.validar("65535", ""),
        )
    }

    @Test
    fun `ip mal formada es invalida`() {
        val casos = listOf(
            "203.0.113",        // faltan octetos
            "203.0.113.9.1",    // sobran octetos
            "203.0.113.256",    // octeto fuera de rango
            "203.0.113.",       // octeto vacío
            "203.0.113.a",      // no numérico
            "localhost",
        )

        casos.forEach { ip ->
            assertTrue(
                "Se esperaba invalida: $ip",
                ValidadorRegla.validar("443", ip) is ValidacionRegla.Invalido,
            )
        }
    }

    @Test
    fun `ips validas en los limites de cada octeto`() {
        assertEquals(
            ValidacionRegla.Valido(puerto = 443, ip = "0.0.0.0"),
            ValidadorRegla.validar("443", "0.0.0.0"),
        )
        assertEquals(
            ValidacionRegla.Valido(puerto = 443, ip = "255.255.255.255"),
            ValidadorRegla.validar("443", "255.255.255.255"),
        )
    }
}
