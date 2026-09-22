package com.ejemplo.nettycoon.ui.componentes

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Cubre las fronteras de los umbrales presentacionales de [estadoSalud].
 * Umbrales: >=70 SEGURA, 40..69 EN_RIESGO, <40 COMPROMETIDA.
 */
class EstadoSaludTest {

    @Test
    fun `0 por ciento es comprometida`() {
        assertEquals(EstadoSalud.COMPROMETIDA, estadoSalud(0))
    }

    @Test
    fun `39 por ciento sigue comprometida (justo bajo el umbral)`() {
        assertEquals(EstadoSalud.COMPROMETIDA, estadoSalud(39))
    }

    @Test
    fun `40 por ciento entra en riesgo (frontera inferior)`() {
        assertEquals(EstadoSalud.EN_RIESGO, estadoSalud(40))
    }

    @Test
    fun `69 por ciento sigue en riesgo (justo bajo segura)`() {
        assertEquals(EstadoSalud.EN_RIESGO, estadoSalud(69))
    }

    @Test
    fun `70 por ciento es segura (frontera inferior)`() {
        assertEquals(EstadoSalud.SEGURA, estadoSalud(70))
    }

    @Test
    fun `100 por ciento es segura`() {
        assertEquals(EstadoSalud.SEGURA, estadoSalud(100))
    }
}
