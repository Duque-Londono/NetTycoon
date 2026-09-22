package com.ejemplo.nettycoon.domain.firewall

import com.ejemplo.nettycoon.data.local.entity.AccionFirewall
import com.ejemplo.nettycoon.data.local.entity.EstadoPartida
import com.ejemplo.nettycoon.data.local.entity.ResultadoEvento
import com.ejemplo.nettycoon.domain.model.CategoriaResultado
import com.ejemplo.nettycoon.domain.model.ResultadoEvaluacion
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests JVM de [ConsecuenciasPartida] (cálculo puro sobre el estado de partida).
 */
class ConsecuenciasPartidaTest {

    private fun estado(
        puntaje: Int = 0,
        saludRed: Int = 100,
        dineroVirtual: Int = 1000,
        nivel: Int = 1,
    ) = EstadoPartida(
        owner = "u",
        puntaje = puntaje,
        saludRed = saludRed,
        dineroVirtual = dineroVirtual,
        nivel = nivel,
    )

    private fun resultado(categoria: CategoriaResultado): ResultadoEvaluacion {
        val (accion, res, acierto) = when (categoria) {
            CategoriaResultado.BLOQUEO_CORRECTO -> Triple(AccionFirewall.DENY, ResultadoEvento.BLOQUEADO, true)
            CategoriaResultado.PERMISO_CORRECTO -> Triple(AccionFirewall.ALLOW, ResultadoEvento.PERMITIDO, true)
            CategoriaResultado.BRECHA -> Triple(AccionFirewall.ALLOW, ResultadoEvento.PERMITIDO, false)
            CategoriaResultado.FALSO_POSITIVO -> Triple(AccionFirewall.DENY, ResultadoEvento.BLOQUEADO, false)
        }
        return ResultadoEvaluacion(accion, null, res, acierto, categoria)
    }

    @Test
    fun `bloqueo correcto suma mas puntaje que permiso correcto`() {
        val bloqueo = ConsecuenciasPartida.aplicar(estado(), resultado(CategoriaResultado.BLOQUEO_CORRECTO))
        val permiso = ConsecuenciasPartida.aplicar(estado(), resultado(CategoriaResultado.PERMISO_CORRECTO))
        assertEquals(BalancePartida.PUNTAJE_BLOQUEO_CORRECTO, bloqueo.puntaje) // 15
        assertEquals(BalancePartida.PUNTAJE_PERMISO_CORRECTO, permiso.puntaje) // 10
        assert(bloqueo.puntaje > permiso.puntaje)
    }

    @Test
    fun `brecha resta salud y dinero`() {
        val r = ConsecuenciasPartida.aplicar(estado(saludRed = 100, dineroVirtual = 1000),
            resultado(CategoriaResultado.BRECHA))
        assertEquals(100 + BalancePartida.SALUD_BRECHA, r.saludRed) // 80
        assertEquals(1000 + BalancePartida.DINERO_BRECHA, r.dineroVirtual) // 900
    }

    @Test
    fun `falso positivo resta puntaje leve, salud y dinero`() {
        val r = ConsecuenciasPartida.aplicar(estado(saludRed = 100, puntaje = 100, dineroVirtual = 1000),
            resultado(CategoriaResultado.FALSO_POSITIVO))
        assertEquals(100 + BalancePartida.PUNTAJE_FALSO_POSITIVO, r.puntaje) // 95
        assertEquals(100 + BalancePartida.SALUD_FALSO_POSITIVO, r.saludRed) // 90
        assertEquals(1000 + BalancePartida.DINERO_FALSO_POSITIVO, r.dineroVirtual) // 975
    }

    @Test
    fun `balance de salud - falso positivo resta 10 y brecha resta 20`() {
        // Blindaje del balance E1: la indisponibilidad (falso positivo) cuesta la mitad que la
        // inseguridad (brecha), y ambas restan salud desde la misma base.
        assertEquals(-10, BalancePartida.SALUD_FALSO_POSITIVO)
        assertEquals(-20, BalancePartida.SALUD_BRECHA)

        val base = estado(saludRed = 100)
        val falsoPositivo = ConsecuenciasPartida.aplicar(base, resultado(CategoriaResultado.FALSO_POSITIVO))
        val brecha = ConsecuenciasPartida.aplicar(base, resultado(CategoriaResultado.BRECHA))
        assertEquals(90, falsoPositivo.saludRed) // 100 - 10
        assertEquals(80, brecha.saludRed) // 100 - 20
    }

    @Test
    fun `salud no baja de 0`() {
        val r = ConsecuenciasPartida.aplicar(estado(saludRed = 10), resultado(CategoriaResultado.BRECHA))
        assertEquals(0, r.saludRed) // 10 - 20 -> clamp 0
    }

    @Test
    fun `dinero no baja de 0`() {
        val r = ConsecuenciasPartida.aplicar(estado(dineroVirtual = 50), resultado(CategoriaResultado.BRECHA))
        assertEquals(0, r.dineroVirtual) // 50 - 100 -> clamp 0
    }

    @Test
    fun `puntaje no baja de 0`() {
        val r = ConsecuenciasPartida.aplicar(estado(puntaje = 0), resultado(CategoriaResultado.FALSO_POSITIVO))
        assertEquals(0, r.puntaje) // 0 - 5 -> clamp 0
    }

    @Test
    fun `el nivel sube segun el puntaje`() {
        // Partiendo de 90 y sumando un bloqueo correcto (+15) -> 105 -> nivel 2.
        val r = ConsecuenciasPartida.aplicar(estado(puntaje = 90), resultado(CategoriaResultado.BLOQUEO_CORRECTO))
        assertEquals(105, r.puntaje)
        assertEquals(2, r.nivel)
    }
}
