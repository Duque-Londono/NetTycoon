package com.ejemplo.nettycoon.ui.network

import com.ejemplo.nettycoon.data.local.entity.EstadoPartida
import com.ejemplo.nettycoon.data.repository.PartidaRepository
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
 * Pruebas JVM del [ConfigRedViewModel]: carga inicial del formulario, guardado válido que
 * preserva las métricas del juego, y guardado inválido que no toca la base de datos.
 *
 * `viewModelScope` corre en `Dispatchers.Main`, que no existe en la JVM, así que se sustituye
 * por un [StandardTestDispatcher] controlado por el test (`setMain`/`resetMain`). Con
 * `advanceUntilIdle()` se dejan terminar las corrutinas antes de comprobar el estado.
 *
 * Se usa el [PartidaRepository] real sobre el [FakeEstadoPartidaDao] existente, para probar el
 * mismo camino que recorre producción.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConfigRedViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val uid = "usuario-prueba"

    @Before
    fun configurarDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun restaurarDispatcher() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel(dao: FakeEstadoPartidaDao): ConfigRedViewModel =
        ConfigRedViewModel(uid, PartidaRepository(dao))

    /** Partida con métricas NO por defecto, para poder verificar que el guardado no las pisa. */
    private fun partidaConMetricas() = EstadoPartida(
        owner = uid,
        puntaje = 750,
        dineroVirtual = 3200,
        saludRed = 42,
        nivel = 5,
        ipRouter = "192.168.0.1",
        puertoLan = 80,
        puertoWan = 443,
    )

    @Test
    fun `carga inicial rellena el formulario desde la partida`() = runTest(dispatcher) {
        val dao = FakeEstadoPartidaDao().apply { almacen[uid] = partidaConMetricas() }
        val vm = crearViewModel(dao)

        advanceUntilIdle()

        val estado = vm.estado.value
        assertFalse(estado.cargando)
        assertEquals("192.168.0.1", estado.ipRouterTexto)
        assertEquals("80", estado.puertoLanTexto)
        assertEquals("443", estado.puertoWanTexto)
        assertNotNull(estado.partida)
    }

    @Test
    fun `guardar valido actualiza los tres campos y preserva las metricas`() = runTest(dispatcher) {
        val dao = FakeEstadoPartidaDao().apply { almacen[uid] = partidaConMetricas() }
        val vm = crearViewModel(dao)
        advanceUntilIdle()

        vm.onIpRouterCambiado("10.0.0.1")
        vm.onPuertoLanCambiado("8080")
        vm.onPuertoWanCambiado("22")
        vm.guardar()
        advanceUntilIdle()

        val guardada = dao.almacen.getValue(uid)
        // Los tres campos de red cambiaron...
        assertEquals("10.0.0.1", guardada.ipRouter)
        assertEquals(8080, guardada.puertoLan)
        assertEquals(22, guardada.puertoWan)
        // ...y las métricas del juego quedaron intactas.
        assertEquals(750, guardada.puntaje)
        assertEquals(3200, guardada.dineroVirtual)
        assertEquals(42, guardada.saludRed)
        assertEquals(5, guardada.nivel)

        assertTrue(vm.estado.value.guardadoConExito)
        assertNull(vm.estado.value.errorFormulario)
    }

    @Test
    fun `editar un campo limpia la senal de guardado con exito`() = runTest(dispatcher) {
        val dao = FakeEstadoPartidaDao().apply { almacen[uid] = partidaConMetricas() }
        val vm = crearViewModel(dao)
        advanceUntilIdle()

        vm.guardar()
        advanceUntilIdle()
        assertTrue(vm.estado.value.guardadoConExito)

        vm.onIpRouterCambiado("10.0.0.2")

        assertFalse(vm.estado.value.guardadoConExito)
    }

    @Test
    fun `guardar invalido no toca la base de datos y publica error de formulario`() = runTest(dispatcher) {
        val original = partidaConMetricas()
        val dao = FakeEstadoPartidaDao().apply { almacen[uid] = original }
        val vm = crearViewModel(dao)
        advanceUntilIdle()

        // IP inválida: la validación debe frenar antes de escribir en Room.
        vm.onIpRouterCambiado("999.999.999.999")
        vm.guardar()
        advanceUntilIdle()

        assertNotNull(vm.estado.value.errorFormulario)
        assertFalse(vm.estado.value.guardadoConExito)
        // La fila en el DAO sigue siendo la original, sin cambios.
        assertEquals(original, dao.almacen.getValue(uid))
    }
}
