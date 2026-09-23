package com.ejemplo.nettycoon.domain.firewall

import com.ejemplo.nettycoon.data.local.entity.AccionFirewall
import com.ejemplo.nettycoon.data.local.entity.EstadoPartida
import com.ejemplo.nettycoon.data.local.entity.ResultadoEvento
import com.ejemplo.nettycoon.domain.model.CategoriaResultado
import com.ejemplo.nettycoon.domain.model.ResultadoEvaluacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests JVM de [rangoPorPuntaje] y sus derivadas (cálculo puro sobre el puntaje).
 *
 * El foco está en las FRONTERAS de cada umbral (justo debajo / justo encima) y en que bajar de
 * rango funcione, ya que el puntaje puede decrecer (el falso positivo resta 5).
 */
class RangoTest {

    // --- Fronteras de cada umbral ---

    @Test
    fun `puntaje 0 es APRENDIZ`() {
        assertEquals(Rango.APRENDIZ, rangoPorPuntaje(0))
    }

    @Test
    fun `frontera de TECNICO`() {
        assertEquals(Rango.APRENDIZ, rangoPorPuntaje(UmbralesRango.PUNTAJE_TECNICO - 1)) // 149
        assertEquals(Rango.TECNICO, rangoPorPuntaje(UmbralesRango.PUNTAJE_TECNICO)) // 150
        assertEquals(Rango.TECNICO, rangoPorPuntaje(UmbralesRango.PUNTAJE_TECNICO + 1)) // 151
    }

    @Test
    fun `frontera de ANALISTA`() {
        assertEquals(Rango.TECNICO, rangoPorPuntaje(UmbralesRango.PUNTAJE_ANALISTA - 1)) // 399
        assertEquals(Rango.ANALISTA, rangoPorPuntaje(UmbralesRango.PUNTAJE_ANALISTA)) // 400
        assertEquals(Rango.ANALISTA, rangoPorPuntaje(UmbralesRango.PUNTAJE_ANALISTA + 1)) // 401
    }

    @Test
    fun `frontera de EXPERTO`() {
        assertEquals(Rango.ANALISTA, rangoPorPuntaje(UmbralesRango.PUNTAJE_EXPERTO - 1)) // 799
        assertEquals(Rango.EXPERTO, rangoPorPuntaje(UmbralesRango.PUNTAJE_EXPERTO)) // 800
        assertEquals(Rango.EXPERTO, rangoPorPuntaje(UmbralesRango.PUNTAJE_EXPERTO + 1)) // 801
    }

    // --- Totalidad: la función acepta cualquier entero ---

    @Test
    fun `un puntaje negativo cae en APRENDIZ y no revienta`() {
        assertEquals(Rango.APRENDIZ, rangoPorPuntaje(-1))
        assertEquals(Rango.APRENDIZ, rangoPorPuntaje(Int.MIN_VALUE))
    }

    @Test
    fun `un puntaje muy alto satura en EXPERTO`() {
        assertEquals(Rango.EXPERTO, rangoPorPuntaje(100_000))
        assertEquals(Rango.EXPERTO, rangoPorPuntaje(Int.MAX_VALUE))
    }

    // --- Bajar de rango (el puntaje puede decrecer: falso positivo = -5) ---

    @Test
    fun `bajar de rango funciona solo, sin estado guardado`() {
        // Justo en el umbral de ANALISTA...
        assertEquals(Rango.ANALISTA, rangoPorPuntaje(400))
        // ...un falso positivo (-5) lo devuelve a TECNICO.
        assertEquals(Rango.TECNICO, rangoPorPuntaje(400 - 5))
    }

    @Test
    fun `el rango baja de verdad al aplicar un falso positivo sobre el umbral`() {
        val enElUmbral = EstadoPartida(owner = "u", puntaje = UmbralesRango.PUNTAJE_ANALISTA)
        assertEquals(Rango.ANALISTA, rangoPorPuntaje(enElUmbral.puntaje))

        val tras = ConsecuenciasPartida.aplicar(
            enElUmbral,
            ResultadoEvaluacion(
                accionAplicada = AccionFirewall.DENY,
                reglaCoincidente = null,
                resultado = ResultadoEvento.BLOQUEADO,
                acierto = false,
                categoria = CategoriaResultado.FALSO_POSITIVO,
            ),
        )

        assertEquals(395, tras.puntaje)
        assertEquals(Rango.TECNICO, rangoPorPuntaje(tras.puntaje))
    }

    // --- Invariantes ---

    @Test
    fun `la funcion es monotona, mas puntaje nunca da un rango menor`() {
        var anterior = rangoPorPuntaje(0)
        for (puntaje in 0..1000) {
            val actual = rangoPorPuntaje(puntaje)
            assertTrue(
                "En $puntaje el rango bajó de ${anterior.etiqueta} a ${actual.etiqueta}",
                actual.ordinal >= anterior.ordinal,
            )
            anterior = actual
        }
    }

    @Test
    fun `los rangos estan declarados en orden ascendente de umbral`() {
        val umbrales = Rango.entries.map { it.puntajeMinimo }
        assertEquals(umbrales.sorted(), umbrales)
    }

    @Test
    fun `todo rango tiene etiqueta y descripcion no vacias`() {
        Rango.entries.forEach {
            assertTrue("Etiqueta vacía en $it", it.etiqueta.isNotBlank())
            assertTrue("Descripción vacía en $it", it.descripcion.isNotBlank())
        }
    }

    // --- Progreso hacia el siguiente rango ---

    @Test
    fun `puntajeParaSiguienteRango cuenta lo que falta`() {
        assertEquals(150, puntajeParaSiguienteRango(0)) // faltan 150 para TECNICO
        assertEquals(1, puntajeParaSiguienteRango(149))
        assertEquals(250, puntajeParaSiguienteRango(150)) // de 150 a 400 (ANALISTA)
        assertEquals(400, puntajeParaSiguienteRango(400)) // de 400 a 800 (EXPERTO)
    }

    @Test
    fun `en EXPERTO ya no hay siguiente rango`() {
        assertNull(siguienteRango(UmbralesRango.PUNTAJE_EXPERTO))
        assertNull(puntajeParaSiguienteRango(UmbralesRango.PUNTAJE_EXPERTO))
        assertNull(puntajeParaSiguienteRango(99_999))
    }

    @Test
    fun `siguienteRango encadena los rangos en orden`() {
        assertEquals(Rango.TECNICO, siguienteRango(0))
        assertEquals(Rango.ANALISTA, siguienteRango(UmbralesRango.PUNTAJE_TECNICO))
        assertEquals(Rango.EXPERTO, siguienteRango(UmbralesRango.PUNTAJE_ANALISTA))
    }
}
