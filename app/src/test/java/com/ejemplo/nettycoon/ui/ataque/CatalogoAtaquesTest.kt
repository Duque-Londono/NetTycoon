package com.ejemplo.nettycoon.ui.ataque

import com.ejemplo.nettycoon.domain.firewall.MapeoFamilias
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Invariantes del catálogo fijo de escenarios ([CatalogoAtaques]). No necesita corrutinas ni Room:
 * es una lista pura en código. Blinda el balance por nivel y las reglas de calidad del nivel
 * IMPOSIBLE (examen final "pelado"), donde el único "tell" vive en el triplete IP/país/ISP.
 */
class CatalogoAtaquesTest {

    private val escenarios = CatalogoAtaques.escenarios
    private val imposibles = escenarios.filter { it.dificultad == Dificultad.IMPOSIBLE }

    @Test
    fun `conteo por dificultad es 9-12-9-9`() {
        assertEquals(9, escenarios.count { it.dificultad == Dificultad.FACIL })
        assertEquals(12, escenarios.count { it.dificultad == Dificultad.MEDIO })
        assertEquals(9, escenarios.count { it.dificultad == Dificultad.DIFICIL })
        assertEquals(9, escenarios.count { it.dificultad == Dificultad.IMPOSIBLE })
    }

    @Test
    fun `los escenarios IMPOSIBLE tienen triplete IP pais ISP no vacio`() {
        // En Imposible se oculta todo salvo el contexto crudo: el triplete es la ÚNICA señal, así
        // que no puede venir vacío.
        imposibles.forEach { esc ->
            assertTrue("IP vacía en puerto ${esc.puerto}", esc.ipAtacante.isNotBlank())
            assertTrue("País vacío en puerto ${esc.puerto}", esc.pais.isNotBlank())
            assertTrue("ISP vacío en puerto ${esc.puerto}", esc.isp.isNotBlank())
        }
    }

    @Test
    fun `los escenarios IMPOSIBLE apagan el material educativo`() {
        // Coherencia con el render pelado: pista y explicación ampliada vacías (se ocultan).
        imposibles.forEach { esc ->
            assertTrue("Pista no vacía en puerto ${esc.puerto}", esc.textoPista.isBlank())
            assertTrue("Explicación no vacía en puerto ${esc.puerto}", esc.explicacionAmpliada.isBlank())
        }
    }

    @Test
    fun `todo malicioso IMPOSIBLE tiene puerto mapeado en una familia (para Estadisticas)`() {
        // Nota: los niveles 1-3 incluyen a propósito maliciosos en puertos "Otros" (escaneo,
        // exfiltración por puerto alto); por eso la invariante se acota al nivel IMPOSIBLE, cuyos
        // maliciosos sí deben agruparse por familia en la pantalla de progreso.
        imposibles.filter { it.esMalicioso }.forEach { esc ->
            assertTrue(
                "El malicioso del puerto ${esc.puerto} cae en Otros y no se agruparía",
                MapeoFamilias.familiaDe(esc.puerto) != MapeoFamilias.OTROS,
            )
        }
    }

    @Test
    fun `la mezcla IMPOSIBLE tiene tanto maliciosos como legitimos`() {
        // No es un nivel de solo-bloquear ni solo-permitir: debe enseñar a discriminar.
        assertTrue(imposibles.any { it.esMalicioso })
        assertTrue(imposibles.any { !it.esMalicioso })
    }
}
