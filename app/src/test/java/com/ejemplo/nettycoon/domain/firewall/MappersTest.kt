package com.ejemplo.nettycoon.domain.firewall

import com.ejemplo.nettycoon.data.local.entity.AccionFirewall
import com.ejemplo.nettycoon.data.local.entity.ReglaFirewall
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests JVM de la traducción Room → dominio ([aEvaluable] / [aReglasEvaluables]).
 */
class MappersTest {

    @Test
    fun `mapea puerto ip y accion correctamente`() {
        val regla = ReglaFirewall(
            id = 5, owner = "u", puerto = 443, ip = "1.2.3.4",
            accion = AccionFirewall.DENY, activa = true,
        )
        val ev = regla.aEvaluable()
        assertEquals(443, ev.puerto)
        assertEquals("1.2.3.4", ev.ip)
        assertEquals(AccionFirewall.DENY, ev.accion)
    }

    @Test
    fun `descarta las reglas inactivas`() {
        val reglas = listOf(
            ReglaFirewall(id = 1, owner = "u", puerto = 80, accion = AccionFirewall.ALLOW, activa = true),
            ReglaFirewall(id = 2, owner = "u", puerto = 22, accion = AccionFirewall.DENY, activa = false),
        )
        val ev = reglas.aReglasEvaluables()
        assertEquals(1, ev.size)
        assertEquals(80, ev.first().puerto)
    }

    @Test
    fun `lista vacia mapea a lista vacia`() {
        assertTrue(emptyList<ReglaFirewall>().aReglasEvaluables().isEmpty())
    }
}
