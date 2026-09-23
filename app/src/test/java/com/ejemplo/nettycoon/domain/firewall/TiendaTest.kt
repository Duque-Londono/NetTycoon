package com.ejemplo.nettycoon.domain.firewall

import com.ejemplo.nettycoon.data.local.entity.AccionFirewall
import com.ejemplo.nettycoon.data.local.entity.EstadoPartida
import com.ejemplo.nettycoon.data.local.entity.ResultadoEvento
import com.ejemplo.nettycoon.domain.model.CategoriaResultado
import com.ejemplo.nettycoon.domain.model.ResultadoEvaluacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests JVM de [Tienda] (compra pura de cura y escudo) y de cómo el escudo comprado intercepta
 * el daño en [ConsecuenciasPartida].
 */
class TiendaTest {

    private fun estado(
        saludRed: Int = 100,
        dineroVirtual: Int = 1000,
        escudoActivo: Boolean = false,
        puntaje: Int = 0,
    ) = EstadoPartida(
        owner = "u",
        puntaje = puntaje,
        saludRed = saludRed,
        dineroVirtual = dineroVirtual,
        escudoActivo = escudoActivo,
    )

    private fun resultado(categoria: CategoriaResultado): ResultadoEvaluacion {
        val (accion, res, acierto) = when (categoria) {
            CategoriaResultado.BLOQUEO_CORRECTO ->
                Triple(AccionFirewall.DENY, ResultadoEvento.BLOQUEADO, true)
            CategoriaResultado.PERMISO_CORRECTO ->
                Triple(AccionFirewall.ALLOW, ResultadoEvento.PERMITIDO, true)
            CategoriaResultado.BRECHA ->
                Triple(AccionFirewall.ALLOW, ResultadoEvento.PERMITIDO, false)
            CategoriaResultado.FALSO_POSITIVO ->
                Triple(AccionFirewall.DENY, ResultadoEvento.BLOQUEADO, false)
        }
        return ResultadoEvaluacion(accion, null, res, acierto, categoria)
    }

    // --- Cura ---

    @Test
    fun `cura con dinero suficiente sube la salud y cobra el precio exacto`() {
        val inicial = estado(saludRed = 50, dineroVirtual = 1000)
        val r = Tienda.comprarCura(inicial, BalancePartida.CURA_PAQUETE_CHICO)

        assertTrue(r.exito)
        assertNull(r.motivo)
        assertEquals(75, r.estado.saludRed) // 50 + 25
        assertEquals(25, r.saludCurada)
        // 25 puntos * 6 $/punto = 150.
        assertEquals(25 * BalancePartida.PRECIO_CURA_POR_PUNTO, r.costo)
        assertEquals(1000 - 150, r.estado.dineroVirtual)
    }

    @Test
    fun `cura con dinero insuficiente se rechaza y NO modifica el estado`() {
        // Curar 25 cuesta 150; el jugador solo tiene 100.
        val inicial = estado(saludRed = 50, dineroVirtual = 100)
        val r = Tienda.comprarCura(inicial, BalancePartida.CURA_PAQUETE_CHICO)

        assertFalse(r.exito)
        assertEquals(Tienda.MotivoRechazo.SALDO_INSUFICIENTE, r.motivo)
        assertEquals(inicial, r.estado) // ni un punto de salud ni un peso se movieron
        assertEquals(0, r.costo)
    }

    @Test
    fun `la cura respeta el tope de 100 y cobra solo los puntos realmente curados`() {
        // Con 90 de salud, el paquete de +25 solo puede curar 10 → cuesta 60, no 150.
        val inicial = estado(saludRed = 90, dineroVirtual = 1000)
        val r = Tienda.comprarCura(inicial, BalancePartida.CURA_PAQUETE_CHICO)

        assertTrue(r.exito)
        assertEquals(BalancePartida.SALUD_MAX, r.estado.saludRed)
        assertEquals(10, r.saludCurada)
        assertEquals(10 * BalancePartida.PRECIO_CURA_POR_PUNTO, r.costo)
        assertEquals(1000 - 60, r.estado.dineroVirtual)
    }

    @Test
    fun `curar con la salud ya llena se rechaza sin cobrar`() {
        val inicial = estado(saludRed = BalancePartida.SALUD_MAX, dineroVirtual = 1000)
        val r = Tienda.comprarCura(inicial, BalancePartida.CURA_PAQUETE_CHICO)

        assertFalse(r.exito)
        assertEquals(Tienda.MotivoRechazo.SALUD_COMPLETA, r.motivo)
        assertEquals(inicial, r.estado)
    }

    @Test
    fun `curar SI se permite con la red comprometida (es el atajo de pago al candado)`() {
        val inicial = estado(saludRed = 0, dineroVirtual = 1000)
        val r = Tienda.comprarCura(inicial, BalancePartida.CURA_PAQUETE_CHICO)

        assertTrue(r.exito)
        assertEquals(25, r.estado.saludRed)
    }

    @Test
    fun `reparar al maximo cobra solo lo que falta`() {
        val inicial = estado(saludRed = 40, dineroVirtual = 1000)
        val r = Tienda.comprarCura(inicial, BalancePartida.SALUD_MAX)

        assertTrue(r.exito)
        assertEquals(BalancePartida.SALUD_MAX, r.estado.saludRed)
        assertEquals(60, r.saludCurada)
        assertEquals(60 * BalancePartida.PRECIO_CURA_POR_PUNTO, r.costo)
    }

    // --- Escudo ---

    @Test
    fun `comprar escudo con dinero suficiente lo activa y cobra el precio`() {
        val inicial = estado(saludRed = 50, dineroVirtual = 1000)
        val r = Tienda.comprarEscudo(inicial)

        assertTrue(r.exito)
        assertTrue(r.estado.escudoActivo)
        assertEquals(BalancePartida.PRECIO_ESCUDO, r.costo)
        assertEquals(1000 - BalancePartida.PRECIO_ESCUDO, r.estado.dineroVirtual)
    }

    @Test
    fun `comprar escudo sin dinero suficiente se rechaza y NO modifica el estado`() {
        val inicial = estado(saludRed = 50, dineroVirtual = BalancePartida.PRECIO_ESCUDO - 1)
        val r = Tienda.comprarEscudo(inicial)

        assertFalse(r.exito)
        assertEquals(Tienda.MotivoRechazo.SALDO_INSUFICIENTE, r.motivo)
        assertEquals(inicial, r.estado)
    }

    @Test
    fun `el escudo no es apilable`() {
        val inicial = estado(saludRed = 50, dineroVirtual = 1000, escudoActivo = true)
        val r = Tienda.comprarEscudo(inicial)

        assertFalse(r.exito)
        assertEquals(Tienda.MotivoRechazo.ESCUDO_YA_ACTIVO, r.motivo)
        assertEquals(inicial, r.estado) // no se cobra un segundo escudo
    }

    @Test
    fun `no se puede comprar escudo con la red comprometida`() {
        val inicial = estado(saludRed = 0, dineroVirtual = 1000)
        val r = Tienda.comprarEscudo(inicial)

        assertFalse(r.exito)
        assertEquals(Tienda.MotivoRechazo.RED_COMPROMETIDA, r.motivo)
        assertEquals(inicial, r.estado)
    }

    // --- Absorción: el escudo comprado interceptando el daño de E1 ---

    @Test
    fun `el escudo absorbe una brecha una SOLA vez y se consume`() {
        val conEscudo = estado(saludRed = 100, dineroVirtual = 1000, escudoActivo = true)

        // Primer golpe: la salud queda intacta, pero el dinero se pierde igual.
        val primero = ConsecuenciasPartida.aplicar(conEscudo, resultado(CategoriaResultado.BRECHA))
        assertEquals(100, primero.saludRed)
        assertFalse(primero.escudoActivo) // consumido
        assertEquals(1000 + BalancePartida.DINERO_BRECHA, primero.dineroVirtual) // -100 igualmente

        // Segundo golpe seguido: ya sin escudo, duele como en E1.
        val segundo = ConsecuenciasPartida.aplicar(primero, resultado(CategoriaResultado.BRECHA))
        assertEquals(100 + BalancePartida.SALUD_BRECHA, segundo.saludRed) // 80
        assertFalse(segundo.escudoActivo)
    }

    @Test
    fun `el escudo absorbe tambien un falso positivo, sin librar del castigo de puntaje`() {
        val conEscudo = estado(
            saludRed = 100,
            dineroVirtual = 1000,
            escudoActivo = true,
            puntaje = 100,
        )
        val r = ConsecuenciasPartida.aplicar(conEscudo, resultado(CategoriaResultado.FALSO_POSITIVO))

        assertEquals(100, r.saludRed)
        assertFalse(r.escudoActivo)
        assertEquals(100 + BalancePartida.PUNTAJE_FALSO_POSITIVO, r.puntaje) // 95
        assertEquals(1000 + BalancePartida.DINERO_FALSO_POSITIVO, r.dineroVirtual) // 975
    }

    @Test
    fun `sin escudo el golpe sigue doliendo exactamente como en E1 (regresion)`() {
        val sinEscudo = estado(saludRed = 100, dineroVirtual = 1000)

        val brecha = ConsecuenciasPartida.aplicar(sinEscudo, resultado(CategoriaResultado.BRECHA))
        assertEquals(100 + BalancePartida.SALUD_BRECHA, brecha.saludRed) // 80

        val falsoPositivo =
            ConsecuenciasPartida.aplicar(sinEscudo, resultado(CategoriaResultado.FALSO_POSITIVO))
        assertEquals(100 + BalancePartida.SALUD_FALSO_POSITIVO, falsoPositivo.saludRed) // 90
    }

    @Test
    fun `un acierto NO consume el escudo`() {
        val conEscudo = estado(saludRed = 80, dineroVirtual = 1000, escudoActivo = true)

        val bloqueo =
            ConsecuenciasPartida.aplicar(conEscudo, resultado(CategoriaResultado.BLOQUEO_CORRECTO))
        assertTrue(bloqueo.escudoActivo)

        val permiso =
            ConsecuenciasPartida.aplicar(conEscudo, resultado(CategoriaResultado.PERMISO_CORRECTO))
        assertTrue(permiso.escudoActivo)
    }

    @Test
    fun `el escudo protege de morir (la red no llega a 0 con el golpe absorbido)`() {
        // 20 de salud + brecha (-20) dejaría la red en 0 y con el candado de E2 puesto.
        val alBorde = estado(saludRed = 20, dineroVirtual = 1000, escudoActivo = true)
        val r = ConsecuenciasPartida.aplicar(alBorde, resultado(CategoriaResultado.BRECHA))

        assertEquals(20, r.saludRed)
        assertTrue(r.saludRed > BalancePartida.SALUD_MIN)
        assertFalse(r.escudoActivo)
    }
}
