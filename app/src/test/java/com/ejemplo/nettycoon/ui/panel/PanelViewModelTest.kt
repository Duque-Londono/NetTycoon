package com.ejemplo.nettycoon.ui.panel

import com.ejemplo.nettycoon.data.local.dao.EstadoPartidaDao
import com.ejemplo.nettycoon.data.local.entity.EstadoPartida
import com.ejemplo.nettycoon.data.repository.EventoAtaqueRepository
import com.ejemplo.nettycoon.data.repository.GeoIpRepository
import com.ejemplo.nettycoon.data.repository.PartidaRepository
import com.ejemplo.nettycoon.data.repository.RegeneradorSalud
import com.ejemplo.nettycoon.data.repository.ReglaFirewallRepository
import com.ejemplo.nettycoon.domain.firewall.ProcesarAtaqueUseCase
import com.ejemplo.nettycoon.domain.firewall.RegenSalud
import com.ejemplo.nettycoon.domain.firewall.fakes.FakeEventoAtaqueDao
import com.ejemplo.nettycoon.domain.firewall.fakes.FakeReglaFirewallDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * Pruebas JVM del [PanelViewModel]. El foco es el bug de QA: el Panel debe OBSERVAR la partida de
 * forma reactiva, de modo que refleje al instante los cambios que otras pantallas ("Ataque en
 * vivo") persisten en la misma fila de Room.
 *
 * Sigue el patrón de los demás tests de ViewModel (`Dispatchers.setMain` con
 * [StandardTestDispatcher] + `advanceUntilIdle()`). Usa un fake de DAO REACTIVO local a este test
 * (backed por [MutableStateFlow]) para no tocar el `FakeEstadoPartidaDao` compartido; así una
 * escritura vía el repositorio se propaga por el `Flow` igual que haría Room.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PanelViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val uid = "usuario-prueba"

    /**
     * DAO de partida reactivo: `observarPorOwner` devuelve un [Flow] respaldado por un
     * [MutableStateFlow] que emite en cada `insertarSiNoExiste`/`actualizar`, imitando a Room.
     */
    private class FakeEstadoPartidaDaoReactivo : EstadoPartidaDao {
        private val estado = MutableStateFlow<EstadoPartida?>(null)

        fun sembrar(partida: EstadoPartida) {
            estado.value = partida
        }

        override suspend fun insertarSiNoExiste(estado: EstadoPartida) {
            // Semántica IGNORE: solo crea si aún no había fila.
            if (this.estado.value == null) this.estado.value = estado
        }

        override suspend fun actualizar(estado: EstadoPartida) {
            this.estado.value = estado
        }

        override suspend fun obtenerPorOwner(owner: String): EstadoPartida? =
            estado.value?.takeIf { it.owner == owner }

        override fun observarPorOwner(owner: String): Flow<EstadoPartida?> = estado.asStateFlow()
    }

    private fun partidaBase() = EstadoPartida(
        owner = uid,
        puntaje = 100,
        dineroVirtual = 1000,
        saludRed = 100,
        nivel = 1,
    )

    private fun crearViewModel(partidaDao: EstadoPartidaDao): Pair<PanelViewModel, PartidaRepository> {
        val partidaRepo = PartidaRepository(partidaDao)
        val useCase = ProcesarAtaqueUseCase(
            reglaRepo = ReglaFirewallRepository(FakeReglaFirewallDao()),
            eventoRepo = EventoAtaqueRepository(FakeEventoAtaqueDao()),
            partidaRepo = partidaRepo,
            geoIpRepo = GeoIpRepository(),
        )
        // Regenerador neutro: el ancla por defecto es "ahora", así que no regenera nada y no
        // interfiere con las aserciones de este test (la regen tiene su propio test).
        val regenerador = RegeneradorSalud(
            partidaRepo = partidaRepo,
            leerAncla = { _, porDefecto -> porDefecto },
            guardarAncla = { _, _ -> },
        )
        return PanelViewModel(uid, useCase, partidaRepo, regenerador) to partidaRepo
    }

    @Before
    fun configurarDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun restaurarDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `carga inicial refleja la partida existente y baja cargando`() = runTest(dispatcher) {
        val dao = FakeEstadoPartidaDaoReactivo().apply { sembrar(partidaBase()) }
        val (vm, _) = crearViewModel(dao)

        advanceUntilIdle()

        val estado = vm.estado.value
        assertFalse("La primera emisión del Flow debe bajar cargando", estado.cargando)
        assertNotNull(estado.partida)
        assertEquals(100, estado.partida!!.saludRed)
        assertEquals(1, estado.partida!!.nivel)
    }

    @Test
    fun `refleja al instante los cambios persistidos por otra pantalla`() = runTest(dispatcher) {
        val dao = FakeEstadoPartidaDaoReactivo().apply { sembrar(partidaBase()) }
        val (vm, partidaRepo) = crearViewModel(dao)
        advanceUntilIdle()

        // Simula lo que "Ataque en vivo" persiste tras perder la partida (misma fila de Room).
        partidaRepo.actualizarPartida(partidaBase().copy(saludRed = 0, puntaje = 250, nivel = 3))
        advanceUntilIdle()

        val estado = vm.estado.value
        assertEquals("El Panel debe reflejar la salud actualizada", 0, estado.partida!!.saludRed)
        assertEquals(250, estado.partida!!.puntaje)
        assertEquals(3, estado.partida!!.nivel)
    }

    @Test
    fun `crea la fila si aun no existe y la observa`() = runTest(dispatcher) {
        val dao = FakeEstadoPartidaDaoReactivo() // sin sembrar: no hay partida todavía.
        val (vm, _) = crearViewModel(dao)

        advanceUntilIdle()

        val estado = vm.estado.value
        assertFalse(estado.cargando)
        assertNotNull("getOrCreate debe haber creado la fila y el Flow emitirla", estado.partida)
        assertEquals(uid, estado.partida!!.owner)
    }

    @Test
    fun `refrescarRegen sube la salud a la vista al cumplirse el tramo`() = runTest(dispatcher) {
        val paso = RegenSalud.PASO_MS
        val t0 = 1_000_000_000_000L
        var ahora = t0
        val anclas = mutableMapOf(uid to t0)

        val dao = FakeEstadoPartidaDaoReactivo().apply { sembrar(partidaBase().copy(saludRed = 50)) }
        val partidaRepo = PartidaRepository(dao)
        val regenerador = RegeneradorSalud(
            partidaRepo = partidaRepo,
            leerAncla = { u, def -> anclas[u] ?: def },
            guardarAncla = { u, m -> anclas[u] = m },
            reloj = { ahora },
        )
        val useCase = ProcesarAtaqueUseCase(
            reglaRepo = ReglaFirewallRepository(FakeReglaFirewallDao()),
            eventoRepo = EventoAtaqueRepository(FakeEventoAtaqueDao()),
            partidaRepo = partidaRepo,
            geoIpRepo = GeoIpRepository(),
        )
        val vm = PanelViewModel(uid, useCase, partidaRepo, regenerador)
        advanceUntilIdle()
        // En init aún no ha pasado tiempo: salud intacta y ancla publicada.
        assertEquals(50, vm.estado.value.partida!!.saludRed)
        assertEquals(t0, vm.estado.value.anclaRegen)

        // Pasa un tramo y el contador dispara la reaplicación.
        ahora = t0 + paso
        vm.refrescarRegen()
        advanceUntilIdle()

        assertEquals(55, vm.estado.value.partida!!.saludRed)
        assertEquals(t0 + paso, vm.estado.value.anclaRegen)
    }
}
