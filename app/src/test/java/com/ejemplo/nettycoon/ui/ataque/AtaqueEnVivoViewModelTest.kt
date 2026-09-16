package com.ejemplo.nettycoon.ui.ataque

import com.ejemplo.nettycoon.data.local.entity.EstadoPartida
import com.ejemplo.nettycoon.data.local.entity.ResultadoEvento
import com.ejemplo.nettycoon.data.repository.EventoAtaqueRepository
import com.ejemplo.nettycoon.data.repository.PartidaRepository
import com.ejemplo.nettycoon.domain.firewall.fakes.FakeEstadoPartidaDao
import com.ejemplo.nettycoon.domain.firewall.fakes.FakeEventoAtaqueDao
import com.ejemplo.nettycoon.domain.model.CategoriaResultado
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
 * Pruebas JVM del [AtaqueEnVivoViewModel]: comprueba el mapeo decisión → categoría → consecuencias
 * (reutilizando el dominio) para los cuatro cruces, el registro del evento, los contadores y el
 * paso al siguiente escenario.
 *
 * Sigue el patrón de `ConfigRedViewModelTest`: `Dispatchers.setMain` con un
 * [StandardTestDispatcher] y `advanceUntilIdle()` para dejar terminar las corrutinas. Usa los
 * repositorios reales sobre los fakes de DAO existentes, y un catálogo y selector controlados para
 * ser deterministas.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AtaqueEnVivoViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val uid = "usuario-prueba"

    private val malicioso = EscenarioAtaque(
        ipAtacante = "1.1.1.1", pais = "X", isp = "Y", puerto = 22,
        servicioNombre = "SSH", esMalicioso = true,
        textoSituacion = "s", textoPista = "p",
        leccionAcierto = "lección acierto malicioso", leccionError = "lección error malicioso",
    )
    private val legitimo = EscenarioAtaque(
        ipAtacante = "2.2.2.2", pais = "Z", isp = "W", puerto = 443,
        servicioNombre = "HTTPS", esMalicioso = false,
        textoSituacion = "s", textoPista = "p",
        leccionAcierto = "lección acierto legítimo", leccionError = "lección error legítimo",
    )

    @Before
    fun configurarDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun restaurarDispatcher() {
        Dispatchers.resetMain()
    }

    /** Partida con métricas holgadas para que ningún delta quede recortado por los límites. */
    private fun partidaBase() = EstadoPartida(
        owner = uid,
        puntaje = 100,
        dineroVirtual = 1000,
        saludRed = 100,
        nivel = 2,
    )

    private fun contexto(): Pair<FakeEstadoPartidaDao, FakeEventoAtaqueDao> {
        val partidaDao = FakeEstadoPartidaDao().apply { almacen[uid] = partidaBase() }
        return partidaDao to FakeEventoAtaqueDao()
    }

    private fun crearViewModel(
        partidaDao: FakeEstadoPartidaDao,
        eventoDao: FakeEventoAtaqueDao,
        escenarios: List<EscenarioAtaque>,
        seleccionar: (Int?) -> Int = { 0 },
    ) = AtaqueEnVivoViewModel(
        uid = uid,
        partidaRepo = PartidaRepository(partidaDao),
        eventoRepo = EventoAtaqueRepository(eventoDao),
        escenarios = escenarios,
        seleccionarSiguiente = seleccionar,
    )

    @Test
    fun `malicioso bloqueado es acierto, categoria bloqueo correcto y suma puntaje y dinero`() =
        runTest(dispatcher) {
            val (partidaDao, eventoDao) = contexto()
            val vm = crearViewModel(partidaDao, eventoDao, listOf(malicioso))
            advanceUntilIdle()

            vm.onBloquear()
            advanceUntilIdle()

            val resultado = vm.estado.value.ultimoResultado!!
            assertTrue(resultado.acierto)
            assertEquals(CategoriaResultado.BLOQUEO_CORRECTO, resultado.categoria)
            assertEquals(ResultadoEvento.BLOQUEADO, resultado.resultadoEvento)
            assertEquals(15, resultado.deltaPuntaje)
            assertEquals(50, resultado.deltaDinero)
            assertEquals(0, resultado.deltaSalud)
            assertEquals(malicioso.leccionAcierto, resultado.leccion)

            // Contadores.
            assertEquals(1, vm.estado.value.aciertos)
            assertEquals(1, vm.estado.value.rondas)

            // Persistencia de partida (delta aplicado sobre la base).
            assertEquals(115, partidaDao.almacen.getValue(uid).puntaje)
            assertEquals(1050, partidaDao.almacen.getValue(uid).dineroVirtual)

            // Evento registrado con los datos del escenario.
            assertEquals(1, eventoDao.insertados.size)
            val evento = eventoDao.insertados.first()
            assertEquals(uid, evento.owner)
            assertEquals("1.1.1.1", evento.ipAtacante)
            assertEquals(22, evento.puertoDestino)
            assertEquals("X", evento.pais)
            assertEquals(ResultadoEvento.BLOQUEADO, evento.resultado)
            assertTrue(evento.acierto)
        }

    @Test
    fun `legitimo permitido es acierto y categoria permiso correcto (mismo valor de acierto)`() =
        runTest(dispatcher) {
            val (partidaDao, eventoDao) = contexto()
            val vm = crearViewModel(partidaDao, eventoDao, listOf(legitimo))
            advanceUntilIdle()

            vm.onPermitir()
            advanceUntilIdle()

            val resultado = vm.estado.value.ultimoResultado!!
            assertTrue(resultado.acierto)
            assertEquals(CategoriaResultado.PERMISO_CORRECTO, resultado.categoria)
            assertEquals(ResultadoEvento.PERMITIDO, resultado.resultadoEvento)
            assertEquals(10, resultado.deltaPuntaje)
            assertEquals(50, resultado.deltaDinero)
            assertEquals(0, resultado.deltaSalud)
            assertEquals(legitimo.leccionAcierto, resultado.leccion)
            assertEquals(1, vm.estado.value.aciertos)

            val evento = eventoDao.insertados.first()
            assertEquals(ResultadoEvento.PERMITIDO, evento.resultado)
            assertTrue(evento.acierto)
        }

    @Test
    fun `malicioso permitido es brecha, resta salud y dinero, y no cuenta acierto`() =
        runTest(dispatcher) {
            val (partidaDao, eventoDao) = contexto()
            val vm = crearViewModel(partidaDao, eventoDao, listOf(malicioso))
            advanceUntilIdle()

            vm.onPermitir()
            advanceUntilIdle()

            val resultado = vm.estado.value.ultimoResultado!!
            assertFalse(resultado.acierto)
            assertEquals(CategoriaResultado.BRECHA, resultado.categoria)
            assertEquals(-20, resultado.deltaSalud)
            assertEquals(-100, resultado.deltaDinero)
            assertEquals(0, resultado.deltaPuntaje)
            assertEquals(malicioso.leccionError, resultado.leccion)
            assertEquals(0, vm.estado.value.aciertos)
            assertEquals(1, vm.estado.value.rondas)

            assertEquals(80, partidaDao.almacen.getValue(uid).saludRed)
        }

    @Test
    fun `legitimo bloqueado es falso positivo, resta puntaje y dinero, y no cuenta acierto`() =
        runTest(dispatcher) {
            val (partidaDao, eventoDao) = contexto()
            val vm = crearViewModel(partidaDao, eventoDao, listOf(legitimo))
            advanceUntilIdle()

            vm.onBloquear()
            advanceUntilIdle()

            val resultado = vm.estado.value.ultimoResultado!!
            assertFalse(resultado.acierto)
            assertEquals(CategoriaResultado.FALSO_POSITIVO, resultado.categoria)
            assertEquals(-5, resultado.deltaPuntaje)
            assertEquals(-25, resultado.deltaDinero)
            assertEquals(0, resultado.deltaSalud)
            assertEquals(legitimo.leccionError, resultado.leccion)
            assertEquals(0, vm.estado.value.aciertos)
        }

    @Test
    fun `una segunda decision sobre el mismo escenario se ignora`() = runTest(dispatcher) {
        val (partidaDao, eventoDao) = contexto()
        val vm = crearViewModel(partidaDao, eventoDao, listOf(malicioso))
        advanceUntilIdle()

        vm.onBloquear()
        advanceUntilIdle()
        // Intento de cambiar la decisión: no debe alterar nada.
        vm.onPermitir()
        advanceUntilIdle()

        assertEquals(CategoriaResultado.BLOQUEO_CORRECTO, vm.estado.value.ultimoResultado!!.categoria)
        assertEquals(1, vm.estado.value.rondas)
        assertEquals(1, eventoDao.insertados.size)
    }

    @Test
    fun `siguiente ataque cambia de escenario y limpia el veredicto`() = runTest(dispatcher) {
        val (partidaDao, eventoDao) = contexto()
        // Arranca en 0 (malicioso) y al pasar va a 1 (legítimo).
        val vm = crearViewModel(
            partidaDao, eventoDao,
            escenarios = listOf(malicioso, legitimo),
            seleccionar = { actual -> if (actual == 0) 1 else 0 },
        )
        advanceUntilIdle()
        assertEquals(malicioso, vm.estado.value.escenario)

        vm.onBloquear()
        advanceUntilIdle()
        assertTrue(vm.estado.value.decisionTomada)

        vm.onSiguienteAtaque()

        assertEquals(legitimo, vm.estado.value.escenario)
        assertNull(vm.estado.value.ultimoResultado)
        assertFalse(vm.estado.value.decisionTomada)
    }

    // --- Fase 2: puente hacia las reglas (sugerencia en memoria) ---

    @Test
    fun `tras 3 aciertos del mismo patron aparece la sugerencia (no antes)`() = runTest(dispatcher) {
        val (partidaDao, eventoDao) = contexto()
        // Escenario único: selector determinista que siempre devuelve 0 (mismo puerto/decisión).
        val vm = crearViewModel(partidaDao, eventoDao, listOf(malicioso), seleccionar = { 0 })
        advanceUntilIdle()

        // Ronda 1 y 2: sin sugerencia todavía.
        vm.onBloquear()
        advanceUntilIdle()
        assertNull(vm.estado.value.ultimoResultado!!.sugerencia)

        vm.onSiguienteAtaque()
        vm.onBloquear()
        advanceUntilIdle()
        assertNull(vm.estado.value.ultimoResultado!!.sugerencia)

        // Ronda 3: alcanza el umbral → aparece la sugerencia del patrón (puerto 22, Bloquear).
        vm.onSiguienteAtaque()
        vm.onBloquear()
        advanceUntilIdle()
        val sugerencia = vm.estado.value.ultimoResultado!!.sugerencia
        assertNotNull(sugerencia)
        assertEquals(22, sugerencia!!.puerto)
        assertEquals("Bloquear", sugerencia.accionTexto)
    }

    @Test
    fun `un patron con errores nunca sugiere automatizar`() = runTest(dispatcher) {
        val (partidaDao, eventoDao) = contexto()
        // Legítimo + Bloquear = falso positivo (error) repetido: no debe contar ni sugerir.
        val vm = crearViewModel(partidaDao, eventoDao, listOf(legitimo), seleccionar = { 0 })
        advanceUntilIdle()

        repeat(4) {
            vm.onBloquear()
            advanceUntilIdle()
            assertFalse(vm.estado.value.ultimoResultado!!.acierto)
            assertNull(vm.estado.value.ultimoResultado!!.sugerencia)
            vm.onSiguienteAtaque()
        }
    }

    @Test
    fun `la sugerencia no se repite para el mismo patron`() = runTest(dispatcher) {
        val (partidaDao, eventoDao) = contexto()
        val vm = crearViewModel(partidaDao, eventoDao, listOf(malicioso), seleccionar = { 0 })
        advanceUntilIdle()

        // Llega al umbral en la 3ª (sugerencia presente).
        repeat(3) {
            vm.onBloquear()
            advanceUntilIdle()
            vm.onSiguienteAtaque()
        }
        // 4º acierto del mismo patrón: ya se sugirió, no debe repetirse.
        vm.onBloquear()
        advanceUntilIdle()
        assertNull(vm.estado.value.ultimoResultado!!.sugerencia)
    }

    @Test
    fun `cada puerto lleva su propio conteo`() = runTest(dispatcher) {
        val (partidaDao, eventoDao) = contexto()
        // Alterna puerto 22 (bloquear) y puerto 443 (permitir); ambos aciertos, patrones distintos.
        val vm = crearViewModel(
            partidaDao, eventoDao,
            escenarios = listOf(malicioso, legitimo),
            seleccionar = { actual -> if (actual == 0) 1 else 0 },
        )
        advanceUntilIdle()

        // Secuencia: 22, 443, 22, 443, 22. El puerto 22 llega a 3; el 443 se queda en 2.
        val sugerencias = mutableListOf<SugerenciaRegla?>()
        repeat(5) {
            if (vm.estado.value.escenario == malicioso) vm.onBloquear() else vm.onPermitir()
            advanceUntilIdle()
            sugerencias.add(vm.estado.value.ultimoResultado!!.sugerencia)
            vm.onSiguienteAtaque()
        }

        // Solo la 5ª ronda (tercer acierto del puerto 22) dispara sugerencia.
        assertNull(sugerencias[0])
        assertNull(sugerencias[1])
        assertNull(sugerencias[2])
        assertNull(sugerencias[3])
        assertNotNull(sugerencias[4])
        assertEquals(22, sugerencias[4]!!.puerto)
    }
}
