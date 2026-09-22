package com.ejemplo.nettycoon.data.repository

import com.ejemplo.nettycoon.data.local.entity.EstadoPartida
import com.ejemplo.nettycoon.domain.firewall.RegenSalud
import com.ejemplo.nettycoon.domain.firewall.fakes.FakeEstadoPartidaDao
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas JVM de [RegeneradorSalud] sobre el [PartidaRepository] real con un fake de DAO y un fake
 * de prefs (mapa en memoria). Blinda la aplicación de la regen y su convivencia con E1: el ancla
 * vive en prefs, independiente de lo que E1 escriba en Room.
 */
class RegeneradorSaludTest {

    private val uid = "usuario-prueba"
    private val paso = RegenSalud.PASO_MS
    private val t0 = 1_000_000_000_000L

    /** Fake de prefs del ancla: un mapa en memoria con la misma firma que `PreferenciasRegen`. */
    private class FakeAnclas {
        private val map = mutableMapOf<String, Long>()
        fun leer(uid: String, porDefecto: Long): Long = map[uid] ?: porDefecto
        fun guardar(uid: String, millis: Long) { map[uid] = millis }
        fun valor(uid: String): Long? = map[uid]
    }

    private fun partidaBase(saludRed: Int) = EstadoPartida(owner = uid, saludRed = saludRed)

    private fun regenerador(
        dao: FakeEstadoPartidaDao,
        anclas: FakeAnclas,
        ahora: Long,
    ) = RegeneradorSalud(
        partidaRepo = PartidaRepository(dao),
        leerAncla = anclas::leer,
        guardarAncla = anclas::guardar,
        reloj = { ahora },
    )

    @Test
    fun `sin tiempo transcurrido no cambia la salud pero deja el ancla`() = runBlocking {
        val dao = FakeEstadoPartidaDao().apply { almacen[uid] = partidaBase(saludRed = 50) }
        val anclas = FakeAnclas()

        regenerador(dao, anclas, ahora = t0).aplicar(uid)

        assertEquals(50, dao.almacen.getValue(uid).saludRed)
        // El ancla por defecto es "ahora"; se persiste para arrancar el conteo desde aquí.
        assertEquals(t0, anclas.valor(uid))
    }

    @Test
    fun `con 10 minutos regenera 10 y persiste salud y ancla`() = runBlocking {
        // Salud 80 como si E1 hubiera aplicado una brecha; el ancla se fijó 10 min antes.
        val dao = FakeEstadoPartidaDao().apply { almacen[uid] = partidaBase(saludRed = 80) }
        val anclas = FakeAnclas().apply { guardar(uid, t0) }

        regenerador(dao, anclas, ahora = t0 + 2 * paso).aplicar(uid)

        assertEquals(90, dao.almacen.getValue(uid).saludRed) // 80 + 2*5
        assertEquals(t0 + 2 * paso, anclas.valor(uid))
    }

    @Test
    fun `regen desde 0 permite salir de comprometida`() = runBlocking {
        val dao = FakeEstadoPartidaDao().apply { almacen[uid] = partidaBase(saludRed = 0) }
        val anclas = FakeAnclas().apply { guardar(uid, t0) }

        regenerador(dao, anclas, ahora = t0 + paso).aplicar(uid)

        assertEquals(5, dao.almacen.getValue(uid).saludRed) // > 0: ya no está comprometida
    }

    @Test
    fun `convivencia con E1 - la regen cuenta desde el ancla en prefs, no desde Room`() = runBlocking {
        // La partida ya fue tocada por E1 (salud 80); el ancla de prefs es lo único que fija el
        // reloj de regen. Aunque Room haya cambiado, la regen usa el ancla guardada.
        val dao = FakeEstadoPartidaDao().apply { almacen[uid] = partidaBase(saludRed = 80) }
        val anclas = FakeAnclas().apply { guardar(uid, t0) }

        regenerador(dao, anclas, ahora = t0 + paso).aplicar(uid)

        assertEquals(85, dao.almacen.getValue(uid).saludRed)
        assertEquals(t0 + paso, anclas.valor(uid))
    }
}
