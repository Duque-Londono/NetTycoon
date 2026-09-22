package com.ejemplo.nettycoon.ui.tienda

import com.ejemplo.nettycoon.data.local.entity.EstadoPartida
import com.ejemplo.nettycoon.data.repository.PartidaRepository
import com.ejemplo.nettycoon.data.repository.RegeneradorSalud
import com.ejemplo.nettycoon.domain.firewall.BalancePartida
import com.ejemplo.nettycoon.domain.firewall.fakes.FakeEstadoPartidaDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas JVM del [TiendaViewModel]: que una compra válida se PERSISTA y que una rechazada no
 * escriba nada. Sigue el patrón de los demás tests de ViewModel (`Dispatchers.setMain` con
 * [StandardTestDispatcher] + `advanceUntilIdle()`).
 *
 * Las reglas de precio y validación tienen su propio test puro (`TiendaTest`); aquí solo se
 * comprueba el cableado: estado publicado, persistencia y avisos.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TiendaViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val uid = "usuario-prueba"

    private fun crearViewModel(
        partida: EstadoPartida,
    ): Triple<TiendaViewModel, PartidaRepository, FakeEstadoPartidaDao> {
        val dao = FakeEstadoPartidaDao().apply { almacen[partida.owner] = partida }
        val partidaRepo = PartidaRepository(dao)
        // Regenerador neutro: el ancla por defecto es "ahora", así que no regenera nada y no
        // interfiere con las aserciones (la regen tiene su propio test).
        val regenerador = RegeneradorSalud(
            partidaRepo = partidaRepo,
            leerAncla = { _, porDefecto -> porDefecto },
            guardarAncla = { _, _ -> },
        )
        return Triple(TiendaViewModel(uid, partidaRepo, regenerador), partidaRepo, dao)
    }

    private fun partida(
        saludRed: Int = 100,
        dineroVirtual: Int = 1000,
        escudoActivo: Boolean = false,
    ) = EstadoPartida(
        owner = uid,
        saludRed = saludRed,
        dineroVirtual = dineroVirtual,
        escudoActivo = escudoActivo,
    )

    @Before
    fun configurarDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun restaurarDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `la carga inicial publica la partida y el escaparate`() = runTest(dispatcher) {
        val (vm, _, _) = crearViewModel(partida(saludRed = 50))
        advanceUntilIdle()

        val estado = vm.estado.value
        assertFalse(estado.cargando)
        assertNotNull(estado.partida)
        // Tres opciones de cura (chica, grande, al máximo) + el escudo.
        assertEquals(4, estado.opciones.size)
    }

    @Test
    fun `una cura valida se publica y se PERSISTE`() = runTest(dispatcher) {
        val (vm, _, dao) = crearViewModel(partida(saludRed = 50, dineroVirtual = 1000))
        advanceUntilIdle()

        vm.comprar(ArticuloTienda.Cura(BalancePartida.CURA_PAQUETE_CHICO))
        advanceUntilIdle()

        // A la vista.
        assertEquals(75, vm.estado.value.partida!!.saludRed)
        assertEquals(850, vm.estado.value.partida!!.dineroVirtual)
        assertNotNull("Debe avisar de la compra", vm.estado.value.aviso)
        // Y en la "base de datos".
        assertEquals(75, dao.almacen[uid]!!.saludRed)
        assertEquals(850, dao.almacen[uid]!!.dineroVirtual)
    }

    @Test
    fun `una compra sin saldo NO persiste nada y avisa`() = runTest(dispatcher) {
        val original = partida(saludRed = 50, dineroVirtual = 10)
        val (vm, _, dao) = crearViewModel(original)
        advanceUntilIdle()

        vm.comprar(ArticuloTienda.Cura(BalancePartida.CURA_PAQUETE_CHICO))
        advanceUntilIdle()

        assertEquals("La partida no debe moverse", original, dao.almacen[uid])
        assertEquals(50, vm.estado.value.partida!!.saludRed)
        assertEquals(10, vm.estado.value.partida!!.dineroVirtual)
        assertNotNull("Debe explicar por qué no se pudo", vm.estado.value.aviso)
    }

    @Test
    fun `comprar el escudo lo activa y lo persiste`() = runTest(dispatcher) {
        val (vm, _, dao) = crearViewModel(partida(saludRed = 60, dineroVirtual = 1000))
        advanceUntilIdle()

        vm.comprar(ArticuloTienda.Escudo)
        advanceUntilIdle()

        assertTrue(vm.estado.value.partida!!.escudoActivo)
        assertTrue(dao.almacen[uid]!!.escudoActivo)
        assertEquals(1000 - BalancePartida.PRECIO_ESCUDO, dao.almacen[uid]!!.dineroVirtual)
    }

    @Test
    fun `con escudo activo la opcion del escudo queda deshabilitada`() = runTest(dispatcher) {
        val (vm, _, _) = crearViewModel(partida(saludRed = 60, escudoActivo = true))
        advanceUntilIdle()

        val escudo = vm.estado.value.opciones.first { it.articulo == ArticuloTienda.Escudo }
        assertFalse(escudo.habilitada)
        assertNotNull(escudo.razonDeshabilitada)
    }

    @Test
    fun `con la red comprometida se puede curar pero no comprar escudo`() = runTest(dispatcher) {
        val (vm, _, _) = crearViewModel(partida(saludRed = 0, dineroVirtual = 1000))
        advanceUntilIdle()

        val opciones = vm.estado.value.opciones
        val escudo = opciones.first { it.articulo == ArticuloTienda.Escudo }
        val curaChica = opciones.first {
            it.articulo == ArticuloTienda.Cura(BalancePartida.CURA_PAQUETE_CHICO)
        }

        assertFalse("El escudo no se vende con la red caída", escudo.habilitada)
        assertTrue("Curar es el atajo de pago para salir del candado", curaChica.habilitada)
    }

    @Test
    fun `limpiarAviso descarta el mensaje`() = runTest(dispatcher) {
        val (vm, _, _) = crearViewModel(partida(saludRed = 50, dineroVirtual = 1000))
        advanceUntilIdle()

        vm.comprar(ArticuloTienda.Cura(BalancePartida.CURA_PAQUETE_CHICO))
        advanceUntilIdle()
        assertNotNull(vm.estado.value.aviso)

        vm.limpiarAviso()
        assertNull(vm.estado.value.aviso)
    }
}
