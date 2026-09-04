package com.ejemplo.nettycoon.domain.firewall

import com.ejemplo.nettycoon.data.local.entity.AccionFirewall
import com.ejemplo.nettycoon.data.local.entity.ResultadoEvento
import com.ejemplo.nettycoon.domain.model.Ataque
import com.ejemplo.nettycoon.domain.model.CategoriaResultado
import com.ejemplo.nettycoon.domain.model.ReglaEvaluable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests JVM del [MotorFirewall] (lógica pura, sin emulador).
 */
class MotorFirewallTest {

    private val ipAtacante = "203.0.113.5"

    // --- Semántica de coincidencia (match) ---

    @Test
    fun `coincide por puerto e ip exactos`() {
        val ataque = Ataque(ipAtacante, puertoDestino = 443, esMalicioso = true)
        val regla = ReglaEvaluable(puerto = 443, ip = ipAtacante, accion = AccionFirewall.DENY)

        val r = MotorFirewall.evaluar(ataque, listOf(regla))

        assertEquals(regla, r.reglaCoincidente)
        assertEquals(AccionFirewall.DENY, r.accionAplicada)
    }

    @Test
    fun `no coincide si el puerto difiere - cae en defecto`() {
        val ataque = Ataque(ipAtacante, puertoDestino = 22, esMalicioso = true)
        val regla = ReglaEvaluable(puerto = 443, ip = ipAtacante, accion = AccionFirewall.ALLOW)

        val r = MotorFirewall.evaluar(ataque, listOf(regla))

        assertNull("puerto distinto no debe coincidir", r.reglaCoincidente)
        assertEquals(AccionFirewall.DENY, r.accionAplicada) // default-DENY
    }

    @Test
    fun `no coincide si la ip difiere - cae en defecto`() {
        val ataque = Ataque(ipAtacante, puertoDestino = 443, esMalicioso = true)
        val regla = ReglaEvaluable(puerto = 443, ip = "198.51.100.9", accion = AccionFirewall.ALLOW)

        val r = MotorFirewall.evaluar(ataque, listOf(regla))

        assertNull(r.reglaCoincidente)
        assertEquals(AccionFirewall.DENY, r.accionAplicada)
    }

    @Test
    fun `ip null actua como comodin y coincide con cualquier ip`() {
        val ataque = Ataque(ipAtacante, puertoDestino = 443, esMalicioso = true)
        val regla = ReglaEvaluable(puerto = 443, ip = null, accion = AccionFirewall.DENY)

        val r = MotorFirewall.evaluar(ataque, listOf(regla))

        assertEquals(regla, r.reglaCoincidente)
        assertEquals(AccionFirewall.DENY, r.accionAplicada)
    }

    // --- Precedencia ---

    @Test
    fun `con reglas en conflicto gana DENY`() {
        val ataque = Ataque(ipAtacante, puertoDestino = 443, esMalicioso = true)
        val allow = ReglaEvaluable(puerto = 443, ip = null, accion = AccionFirewall.ALLOW)
        val deny = ReglaEvaluable(puerto = 443, ip = ipAtacante, accion = AccionFirewall.DENY)

        // Orden ALLOW antes que DENY: debe ganar DENY igualmente (determinista).
        val r = MotorFirewall.evaluar(ataque, listOf(allow, deny))

        assertEquals(AccionFirewall.DENY, r.accionAplicada)
        assertEquals(deny, r.reglaCoincidente)
    }

    @Test
    fun `varias reglas ALLOW coincidentes y ningun DENY aplica ALLOW`() {
        val ataque = Ataque(ipAtacante, puertoDestino = 80, esMalicioso = false)
        val allow1 = ReglaEvaluable(puerto = 80, ip = null, accion = AccionFirewall.ALLOW)
        val allow2 = ReglaEvaluable(puerto = 80, ip = ipAtacante, accion = AccionFirewall.ALLOW)

        val r = MotorFirewall.evaluar(ataque, listOf(allow1, allow2))

        assertEquals(AccionFirewall.ALLOW, r.accionAplicada)
        assertEquals(AccionFirewall.ALLOW, r.reglaCoincidente?.accion)
    }

    // --- Política por defecto ---

    @Test
    fun `sin coincidencias aplica default-DENY`() {
        val ataque = Ataque(ipAtacante, puertoDestino = 8080, esMalicioso = true)
        val regla = ReglaEvaluable(puerto = 443, ip = null, accion = AccionFirewall.ALLOW)

        val r = MotorFirewall.evaluar(ataque, listOf(regla))

        assertNull(r.reglaCoincidente)
        assertEquals(AccionFirewall.DENY, r.accionAplicada)
    }

    @Test
    fun `politica por defecto es configurable a ALLOW`() {
        val ataque = Ataque(ipAtacante, puertoDestino = 8080, esMalicioso = false)

        val r = MotorFirewall.evaluar(ataque, emptyList(), politicaPorDefecto = AccionFirewall.ALLOW)

        assertNull(r.reglaCoincidente)
        assertEquals(AccionFirewall.ALLOW, r.accionAplicada)
    }

    @Test
    fun `lista de reglas vacia cae en default-DENY sin error`() {
        val ataque = Ataque(ipAtacante, puertoDestino = 443, esMalicioso = true)

        val r = MotorFirewall.evaluar(ataque, emptyList())

        assertNull(r.reglaCoincidente)
        assertEquals(AccionFirewall.DENY, r.accionAplicada)
        assertEquals(ResultadoEvento.BLOQUEADO, r.resultado)
    }

    // --- Los 4 resultados del modelo de acierto ---

    @Test
    fun `malicioso mas DENY es bloqueo correcto`() {
        val ataque = Ataque(ipAtacante, puertoDestino = 443, esMalicioso = true)
        val deny = ReglaEvaluable(puerto = 443, ip = null, accion = AccionFirewall.DENY)

        val r = MotorFirewall.evaluar(ataque, listOf(deny))

        assertTrue(r.acierto)
        assertEquals(ResultadoEvento.BLOQUEADO, r.resultado)
        assertEquals(CategoriaResultado.BLOQUEO_CORRECTO, r.categoria)
    }

    @Test
    fun `legitimo mas ALLOW es permiso correcto`() {
        val ataque = Ataque(ipAtacante, puertoDestino = 80, esMalicioso = false)
        val allow = ReglaEvaluable(puerto = 80, ip = null, accion = AccionFirewall.ALLOW)

        val r = MotorFirewall.evaluar(ataque, listOf(allow))

        assertTrue(r.acierto)
        assertEquals(ResultadoEvento.PERMITIDO, r.resultado)
        assertEquals(CategoriaResultado.PERMISO_CORRECTO, r.categoria)
    }

    @Test
    fun `malicioso mas ALLOW es brecha`() {
        val ataque = Ataque(ipAtacante, puertoDestino = 80, esMalicioso = true)
        val allow = ReglaEvaluable(puerto = 80, ip = null, accion = AccionFirewall.ALLOW)

        val r = MotorFirewall.evaluar(ataque, listOf(allow))

        assertFalse(r.acierto)
        assertEquals(ResultadoEvento.PERMITIDO, r.resultado)
        assertEquals(CategoriaResultado.BRECHA, r.categoria)
    }

    @Test
    fun `legitimo mas DENY es falso positivo`() {
        val ataque = Ataque(ipAtacante, puertoDestino = 80, esMalicioso = false)
        val deny = ReglaEvaluable(puerto = 80, ip = null, accion = AccionFirewall.DENY)

        val r = MotorFirewall.evaluar(ataque, listOf(deny))

        assertFalse(r.acierto)
        assertEquals(ResultadoEvento.BLOQUEADO, r.resultado)
        assertEquals(CategoriaResultado.FALSO_POSITIVO, r.categoria)
    }
}
