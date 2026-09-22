package com.ejemplo.nettycoon.ui.ataque

import com.ejemplo.nettycoon.data.local.entity.AccionFirewall
import com.ejemplo.nettycoon.data.local.entity.EstadoPartida
import com.ejemplo.nettycoon.data.local.entity.ReglaFirewall
import com.ejemplo.nettycoon.data.local.entity.ResultadoEvento
import com.ejemplo.nettycoon.data.repository.EventoAtaqueRepository
import com.ejemplo.nettycoon.data.repository.PartidaRepository
import com.ejemplo.nettycoon.data.repository.RegeneradorSalud
import com.ejemplo.nettycoon.data.repository.ReglaFirewallRepository
import com.ejemplo.nettycoon.domain.firewall.MapeoFamilias
import com.ejemplo.nettycoon.domain.firewall.fakes.FakeEstadoPartidaDao
import com.ejemplo.nettycoon.domain.firewall.fakes.FakeEventoAtaqueDao
import com.ejemplo.nettycoon.domain.firewall.fakes.FakeReglaFirewallDao
import com.ejemplo.nettycoon.domain.model.CategoriaResultado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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

    /** Copia del escenario malicioso base cambiando solo el puerto (misma verdad: DENY = acierto). */
    private fun maliciosoEnPuerto(puerto: Int) = malicioso.copy(puerto = puerto)

    /** Copia del escenario malicioso base como nivel IMPOSIBLE, con puerto opcional. */
    private fun imposibleEnPuerto(puerto: Int) =
        malicioso.copy(puerto = puerto, dificultad = Dificultad.IMPOSIBLE)

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
        nivel: Dificultad = Dificultad.MEDIO,
        seleccionar: (Int?, Int) -> Int = { _, _ -> 0 },
        reglaDao: FakeReglaFirewallDao = FakeReglaFirewallDao(),
    ): AtaqueEnVivoViewModel {
        val partidaRepo = PartidaRepository(partidaDao)
        return AtaqueEnVivoViewModel(
            uid = uid,
            partidaRepo = partidaRepo,
            eventoRepo = EventoAtaqueRepository(eventoDao),
            reglaRepo = ReglaFirewallRepository(reglaDao),
            regenerador = regeneradorNeutro(partidaRepo),
            escenarios = escenarios,
            nivelInicial = nivel,
            seleccionarSiguiente = seleccionar,
        )
    }

    /**
     * Regenerador neutro para los tests que no ejercitan la regen: el ancla por defecto es "ahora",
     * así que `RegenSalud.calcular` no suma nada y la salud queda como la deja E1.
     */
    private fun regeneradorNeutro(partidaRepo: PartidaRepository) = RegeneradorSalud(
        partidaRepo = partidaRepo,
        leerAncla = { _, porDefecto -> porDefecto },
        guardarAncla = { _, _ -> },
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
            // El falso positivo también resta salud (indisponibilidad): base 100 - 10 = 90.
            assertEquals(-10, resultado.deltaSalud)
            assertEquals(90, partidaDao.almacen.getValue(uid).saludRed)
            assertEquals(legitimo.leccionError, resultado.leccion)
            assertEquals(0, vm.estado.value.aciertos)
        }

    // --- Candado E2: red comprometida (salud <= 0) ---

    @Test
    fun `entrar con salud 0 marca la red comprometida y no carga escenario`() =
        runTest(dispatcher) {
            val partidaDao = FakeEstadoPartidaDao().apply {
                almacen[uid] = partidaBase().copy(saludRed = 0)
            }
            val vm = vmConNivel(partidaDao, FakeEventoAtaqueDao(), nivelInicial = null)
            advanceUntilIdle()

            assertTrue(vm.estado.value.comprometida)
            assertNull(vm.estado.value.escenario)
        }

    @Test
    fun `si el golpe final deja la salud en 0, la siguiente ronda queda bloqueada`() =
        runTest(dispatcher) {
            // Salud 20: permitir el malicioso (brecha -20) la lleva a 0.
            val partidaDao = FakeEstadoPartidaDao().apply {
                almacen[uid] = partidaBase().copy(saludRed = 20)
            }
            val vm = crearViewModel(partidaDao, FakeEventoAtaqueDao(), listOf(malicioso))
            advanceUntilIdle()

            vm.onPermitir()
            advanceUntilIdle()
            // El veredicto del golpe final SÍ se muestra (aún no está comprometida).
            assertEquals(CategoriaResultado.BRECHA, vm.estado.value.ultimoResultado!!.categoria)
            assertFalse(vm.estado.value.comprometida)
            assertEquals(0, partidaDao.almacen.getValue(uid).saludRed)

            // Al pedir la siguiente ronda, se activa el candado en vez de cargar otro escenario.
            vm.onSiguienteAtaque()
            advanceUntilIdle()
            assertTrue(vm.estado.value.comprometida)
            assertNull(vm.estado.value.escenario)
            assertNull(vm.estado.value.ultimoResultado)
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
            seleccionar = { actual, _ -> if (actual == 0) 1 else 0 },
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
        val vm = crearViewModel(partidaDao, eventoDao, listOf(malicioso), seleccionar = { _, _ -> 0 })
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
        val vm = crearViewModel(partidaDao, eventoDao, listOf(legitimo), seleccionar = { _, _ -> 0 })
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
        val vm = crearViewModel(partidaDao, eventoDao, listOf(malicioso), seleccionar = { _, _ -> 0 })
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
    fun `familias distintas llevan conteos separados`() = runTest(dispatcher) {
        val (partidaDao, eventoDao) = contexto()
        // Alterna puerto 22 (Acceso remoto, bloquear) y 443 (Web, permitir): familias y acciones
        // distintas, ambos aciertos.
        val vm = crearViewModel(
            partidaDao, eventoDao,
            escenarios = listOf(malicioso, legitimo),
            seleccionar = { actual, _ -> if (actual == 0) 1 else 0 },
        )
        advanceUntilIdle()

        // Secuencia: 22, 443, 22, 443, 22. "Acceso remoto" llega a 3; "Web" se queda en 2.
        val sugerencias = mutableListOf<SugerenciaRegla?>()
        repeat(5) {
            if (vm.estado.value.escenario == malicioso) vm.onBloquear() else vm.onPermitir()
            advanceUntilIdle()
            sugerencias.add(vm.estado.value.ultimoResultado!!.sugerencia)
            vm.onSiguienteAtaque()
        }

        // Solo la 5ª ronda (tercer acierto de "Acceso remoto") dispara; "Web" nunca llega al umbral.
        assertNull(sugerencias[0])
        assertNull(sugerencias[1])
        assertNull(sugerencias[2])
        assertNull(sugerencias[3])
        assertNotNull(sugerencias[4])
        assertEquals(22, sugerencias[4]!!.puerto)
        assertEquals(MapeoFamilias.ACCESO_REMOTO, sugerencias[4]!!.familia)
    }

    @Test
    fun `tres aciertos de la misma familia con puertos distintos disparan la sugerencia`() =
        runTest(dispatcher) {
            val (partidaDao, eventoDao) = contexto()
            // Bases de datos con puertos DISTINTOS: MySQL 3306, PostgreSQL 5432, MongoDB 27017.
            // Con el conteo por puerto exacto de antes NO habría disparado; por familia, sí.
            val escenarios = listOf(
                maliciosoEnPuerto(3306), maliciosoEnPuerto(5432), maliciosoEnPuerto(27017),
            )
            val vm = crearViewModel(
                partidaDao, eventoDao, escenarios,
                seleccionar = { actual, _ -> (actual ?: -1) + 1 },
            )
            advanceUntilIdle()

            // Rondas 1 (3306) y 2 (5432): aún por debajo del umbral de la familia.
            vm.onBloquear()
            advanceUntilIdle()
            assertNull(vm.estado.value.ultimoResultado!!.sugerencia)
            vm.onSiguienteAtaque()
            vm.onBloquear()
            advanceUntilIdle()
            assertNull(vm.estado.value.ultimoResultado!!.sugerencia)

            // Ronda 3 (27017): tercer acierto de "Bases de datos" → dispara.
            vm.onSiguienteAtaque()
            vm.onBloquear()
            advanceUntilIdle()
            val sugerencia = vm.estado.value.ultimoResultado!!.sugerencia
            assertNotNull(sugerencia)
            assertEquals(MapeoFamilias.BASES_DE_DATOS, sugerencia!!.familia)
            assertEquals(27017, sugerencia.puerto) // el puerto exacto que venía decidiendo
            assertEquals(AccionFirewall.DENY, sugerencia.accion)
        }

    // --- Fase 3: niveles de dificultad + progresión ---

    /** VM con el catálogo real y el selector de progresión por defecto (secuencial). */
    private fun vmConNivel(
        partidaDao: FakeEstadoPartidaDao,
        eventoDao: FakeEventoAtaqueDao,
        nivelInicial: Dificultad?,
    ): AtaqueEnVivoViewModel {
        val partidaRepo = PartidaRepository(partidaDao)
        return AtaqueEnVivoViewModel(
            uid = uid,
            partidaRepo = partidaRepo,
            eventoRepo = EventoAtaqueRepository(eventoDao),
            reglaRepo = ReglaFirewallRepository(FakeReglaFirewallDao()),
            regenerador = regeneradorNeutro(partidaRepo),
            nivelInicial = nivelInicial,
        )
    }

    @Test
    fun `sin elegir nivel se muestra el selector (sin escenario)`() = runTest(dispatcher) {
        val (partidaDao, eventoDao) = contexto()
        val vm = vmConNivel(partidaDao, eventoDao, nivelInicial = null)
        advanceUntilIdle()

        assertNull(vm.estado.value.nivel)
        assertNull(vm.estado.value.escenario)
        assertFalse(vm.estado.value.nivelCompletado)
    }

    @Test
    fun `un nivel solo sirve sus escenarios, en orden del catalogo, hasta completarse`() =
        runTest(dispatcher) {
            val (partidaDao, eventoDao) = contexto()
            val vm = vmConNivel(partidaDao, eventoDao, nivelInicial = Dificultad.FACIL)
            advanceUntilIdle()

            val esperados = CatalogoAtaques.escenarios.filter { it.dificultad == Dificultad.FACIL }
            val vistos = mutableListOf<EscenarioAtaque>()

            var guarda = 0
            while (!vm.estado.value.nivelCompletado && guarda++ < 100) {
                val esc = vm.estado.value.escenario!!
                assertEquals(Dificultad.FACIL, esc.dificultad)
                vistos.add(esc)
                vm.onPermitir() // decide algo para poder avanzar
                advanceUntilIdle()
                vm.onSiguienteAtaque()
            }

            assertTrue(vm.estado.value.nivelCompletado)
            assertNull(vm.estado.value.escenario)
            // La progresión recorrió exactamente los del nivel, en el orden del catálogo.
            assertEquals(esperados, vistos)
        }

    @Test
    fun `cambiar de nivel vuelve al selector, cambia el conjunto y reinicia contadores`() =
        runTest(dispatcher) {
            val (partidaDao, eventoDao) = contexto()
            val vm = vmConNivel(partidaDao, eventoDao, nivelInicial = Dificultad.FACIL)
            advanceUntilIdle()

            vm.onPermitir()
            advanceUntilIdle()
            assertEquals(1, vm.estado.value.rondas)
            assertEquals(Dificultad.FACIL, vm.estado.value.escenario!!.dificultad)

            vm.cambiarNivel()
            assertNull(vm.estado.value.nivel)
            assertNull(vm.estado.value.escenario)

            vm.elegirNivel(Dificultad.DIFICIL)
            advanceUntilIdle()
            assertEquals(Dificultad.DIFICIL, vm.estado.value.nivel)
            assertEquals(Dificultad.DIFICIL, vm.estado.value.escenario!!.dificultad)
            // Contadores reiniciados al cambiar de nivel.
            assertEquals(0, vm.estado.value.aciertos)
            assertEquals(0, vm.estado.value.rondas)
        }

    @Test
    fun `reciclar reinicia el nivel desde el primer escenario y limpia contadores`() =
        runTest(dispatcher) {
            val (partidaDao, eventoDao) = contexto()
            val vm = vmConNivel(partidaDao, eventoDao, nivelInicial = Dificultad.FACIL)
            advanceUntilIdle()
            val primero = vm.estado.value.escenario

            vm.onPermitir()
            advanceUntilIdle()
            vm.onSiguienteAtaque()
            vm.onPermitir()
            advanceUntilIdle()
            assertEquals(2, vm.estado.value.rondas)

            vm.reciclarNivel()
            advanceUntilIdle()
            assertEquals(primero, vm.estado.value.escenario)
            assertFalse(vm.estado.value.nivelCompletado)
            assertEquals(0, vm.estado.value.rondas)
            assertEquals(0, vm.estado.value.aciertos)
        }

    @Test
    fun `el puente sigue disparando si se repite el mismo puerto dentro del nivel`() =
        runTest(dispatcher) {
            val (partidaDao, eventoDao) = contexto()
            // Selector que se queda en el mismo escenario (puerto 22): simula repetir el patrón.
            val vm = crearViewModel(partidaDao, eventoDao, listOf(malicioso), seleccionar = { _, _ -> 0 })
            advanceUntilIdle()

            var sugerencia: SugerenciaRegla? = null
            repeat(3) {
                vm.onBloquear()
                advanceUntilIdle()
                sugerencia = vm.estado.value.ultimoResultado!!.sugerencia
                vm.onSiguienteAtaque()
            }

            // Al 3er acierto del mismo puerto, el puente sigue funcional (solo es menos frecuente
            // con la progresión, que normalmente cambia de escenario cada vez).
            assertNotNull(sugerencia)
            assertEquals(22, sugerencia!!.puerto)
            assertEquals("Bloquear", sugerencia!!.accionTexto)
        }

    // --- Fase 4: puente reglas → juego (automatización) ---

    private fun reglaDe(
        puerto: Int,
        accion: AccionFirewall,
        ip: String? = null,
        activa: Boolean = true,
    ) = ReglaFirewall(owner = uid, puerto = puerto, ip = ip, accion = accion, activa = activa)

    @Test
    fun `regla que aplica y acierta automatiza la ronda sin tocar puntaje ni contadores`() =
        runTest(dispatcher) {
            val (partidaDao, eventoDao) = contexto()
            // Regla DENY en el puerto 22 (malicioso) → bloquear es lo correcto.
            val reglaDao = FakeReglaFirewallDao(listOf(reglaDe(22, AccionFirewall.DENY)))
            val vm = crearViewModel(partidaDao, eventoDao, listOf(malicioso), reglaDao = reglaDao)
            advanceUntilIdle()

            val resultado = vm.estado.value.ultimoResultado!!
            // Se resolvió sola: es automatizada, acertó y bloqueó.
            assertTrue(resultado.automatizada)
            assertTrue(resultado.acierto)
            assertEquals(CategoriaResultado.BLOQUEO_CORRECTO, resultado.categoria)
            assertEquals(ResultadoEvento.BLOQUEADO, resultado.resultadoEvento)
            assertEquals(malicioso.leccionAcierto, resultado.leccion)

            // La tarjeta describe la regla que actuó.
            assertNotNull(resultado.automatizadaPor)
            assertEquals(22, resultado.automatizadaPor!!.puerto)
            assertEquals("Bloquear", resultado.automatizadaPor!!.accionTexto)

            // NO toca puntaje/salud/dinero ni contadores ni puente.
            assertEquals(0, resultado.deltaPuntaje)
            assertEquals(0, resultado.deltaSalud)
            assertEquals(0, resultado.deltaDinero)
            assertNull(resultado.sugerencia)
            assertEquals(0, vm.estado.value.aciertos)
            assertEquals(0, vm.estado.value.rondas)
            assertFalse(vm.estado.value.evaluandoRegla)

            // La partida queda intacta (sin cambios respecto a la base).
            assertEquals(100, partidaDao.almacen.getValue(uid).puntaje)
            assertEquals(1000, partidaDao.almacen.getValue(uid).dineroVirtual)
            assertEquals(100, partidaDao.almacen.getValue(uid).saludRed)

            // Pero SÍ se registra el evento (historial veraz).
            assertEquals(1, eventoDao.insertados.size)
            val evento = eventoDao.insertados.first()
            assertEquals(22, evento.puertoDestino)
            assertEquals(ResultadoEvento.BLOQUEADO, evento.resultado)
            assertTrue(evento.acierto)
        }

    @Test
    fun `sin regla que aplique se decide a mano (el default-DENY no auto-decide)`() =
        runTest(dispatcher) {
            val (partidaDao, eventoDao) = contexto()
            // Regla de OTRO puerto: no casa con el escenario del puerto 22.
            val reglaDao = FakeReglaFirewallDao(listOf(reglaDe(443, AccionFirewall.ALLOW)))
            val vm = crearViewModel(partidaDao, eventoDao, listOf(malicioso), reglaDao = reglaDao)
            advanceUntilIdle()

            // No se automatizó: no hay veredicto y ya no se está evaluando → se muestran botones.
            assertNull(vm.estado.value.ultimoResultado)
            assertFalse(vm.estado.value.evaluandoRegla)
            assertEquals(0, eventoDao.insertados.size)

            // Y la decisión manual funciona igual que hoy.
            vm.onBloquear()
            advanceUntilIdle()
            val resultado = vm.estado.value.ultimoResultado!!
            assertFalse(resultado.automatizada)
            assertTrue(resultado.acierto)
            assertEquals(15, resultado.deltaPuntaje)
            assertEquals(1, vm.estado.value.rondas)
            assertEquals(1, eventoDao.insertados.size)
        }

    @Test
    fun `una regla mal puesta automatiza una BRECHA (acierto false) sin penalizar metricas`() =
        runTest(dispatcher) {
            val (partidaDao, eventoDao) = contexto()
            // Regla ALLOW en el puerto 22, pero el escenario es malicioso → deja pasar un ataque.
            val reglaDao = FakeReglaFirewallDao(listOf(reglaDe(22, AccionFirewall.ALLOW)))
            val vm = crearViewModel(partidaDao, eventoDao, listOf(malicioso), reglaDao = reglaDao)
            advanceUntilIdle()

            val resultado = vm.estado.value.ultimoResultado!!
            assertTrue(resultado.automatizada)
            assertFalse(resultado.acierto)
            assertEquals(CategoriaResultado.BRECHA, resultado.categoria)
            assertEquals(ResultadoEvento.PERMITIDO, resultado.resultadoEvento)
            assertEquals(malicioso.leccionError, resultado.leccion)
            assertEquals("Permitir", resultado.automatizadaPor!!.accionTexto)

            // Descartada la penalización: la brecha automatizada NO resta salud/dinero.
            assertEquals(0, resultado.deltaSalud)
            assertEquals(0, resultado.deltaDinero)
            assertEquals(100, partidaDao.almacen.getValue(uid).saludRed)

            // Historial veraz: el evento refleja el fallo.
            val evento = eventoDao.insertados.first()
            assertEquals(ResultadoEvento.PERMITIDO, evento.resultado)
            assertFalse(evento.acierto)
        }

    @Test
    fun `una regla comodin (IP nula) aplica y la tarjeta muestra IP nula`() = runTest(dispatcher) {
        val (partidaDao, eventoDao) = contexto()
        // Regla sin IP (comodín) en el puerto 443 legítimo → permitir es lo correcto.
        val reglaDao = FakeReglaFirewallDao(listOf(reglaDe(443, AccionFirewall.ALLOW, ip = null)))
        val vm = crearViewModel(partidaDao, eventoDao, listOf(legitimo), reglaDao = reglaDao)
        advanceUntilIdle()

        val resultado = vm.estado.value.ultimoResultado!!
        assertTrue(resultado.automatizada)
        assertTrue(resultado.acierto)
        assertEquals(CategoriaResultado.PERMISO_CORRECTO, resultado.categoria)
        assertNull(resultado.automatizadaPor!!.ip)
        assertEquals("Permitir", resultado.automatizadaPor!!.accionTexto)
        // No incrementa contadores manuales.
        assertEquals(0, vm.estado.value.rondas)
    }

    // --- Fase 5: puente accionable (crear reglas desde la sugerencia) ---

    /** Lleva el puente al umbral repitiendo aciertos DENY sobre escenarios de una misma familia. */
    private fun TestScope.llegarAlUmbral(vm: AtaqueEnVivoViewModel) {
        repeat(UMBRAL) {
            vm.onBloquear()
            advanceUntilIdle()
            if (it < UMBRAL - 1) vm.onSiguienteAtaque()
        }
    }

    @Test
    fun `automatizarPuerto crea una sola regla para el puerto exacto con IP comodin`() =
        runTest(dispatcher) {
            val (partidaDao, eventoDao) = contexto()
            val reglaDao = FakeReglaFirewallDao()
            // Mismo puerto 22 repetido: alcanza el umbral de "Acceso remoto".
            val vm = crearViewModel(
                partidaDao, eventoDao, listOf(malicioso),
                seleccionar = { _, _ -> 0 }, reglaDao = reglaDao,
            )
            advanceUntilIdle()
            llegarAlUmbral(vm)
            assertNotNull(vm.estado.value.ultimoResultado!!.sugerencia)

            vm.automatizarPuerto()
            advanceUntilIdle()

            val reglas = reglaDao.obtenerActivasPorOwner(uid)
            assertEquals(1, reglas.size)
            assertEquals(22, reglas.first().puerto)
            assertEquals(AccionFirewall.DENY, reglas.first().accion)
            assertNull(reglas.first().ip)
            assertTrue(reglas.first().activa)
            // La tarjeta se retira y se publica un aviso.
            assertNull(vm.estado.value.ultimoResultado!!.sugerencia)
            assertNotNull(vm.estado.value.avisoReglas)
        }

    @Test
    fun `automatizarFamilia crea las reglas que faltan y no duplica las ya cubiertas`() =
        runTest(dispatcher) {
            val (partidaDao, eventoDao) = contexto()
            // Regla activa preexistente en el 22 (Acceso remoto), en un puerto que NO usan los
            // escenarios: así no auto-resuelve ninguna ronda y podemos probar el filtrado.
            val reglaDao = FakeReglaFirewallDao(listOf(reglaDe(22, AccionFirewall.DENY)))
            val escenarios = listOf(
                maliciosoEnPuerto(23), maliciosoEnPuerto(3389), maliciosoEnPuerto(5900),
            )
            val vm = crearViewModel(
                partidaDao, eventoDao, escenarios,
                seleccionar = { actual, _ -> (actual ?: -1) + 1 }, reglaDao = reglaDao,
            )
            advanceUntilIdle()
            llegarAlUmbral(vm)
            val sugerencia = vm.estado.value.ultimoResultado!!.sugerencia
            assertNotNull(sugerencia)
            assertEquals(MapeoFamilias.ACCESO_REMOTO, sugerencia!!.familia)

            vm.automatizarFamilia()
            advanceUntilIdle()

            val reglas = reglaDao.obtenerActivasPorOwner(uid)
            // 1 preexistente (22) + 3 nuevas (23, 3389, 5900) = 4, sin duplicar el 22.
            assertEquals(4, reglas.size)
            assertEquals(setOf(22, 23, 3389, 5900), reglas.map { it.puerto }.toSet())
            assertEquals(1, reglas.count { it.puerto == 22 })
            assertTrue(reglas.all { it.accion == AccionFirewall.DENY })
            assertTrue(reglas.all { it.ip == null })
            assertNull(vm.estado.value.ultimoResultado!!.sugerencia)
            assertNotNull(vm.estado.value.avisoReglas)
        }

    // --- Fase 6: nivel IMPOSIBLE (examen final "pelado") ---

    @Test
    fun `nivel IMPOSIBLE sirve solo escenarios IMPOSIBLE, en orden, hasta completar`() =
        runTest(dispatcher) {
            val (partidaDao, eventoDao) = contexto()
            val vm = vmConNivel(partidaDao, eventoDao, nivelInicial = Dificultad.IMPOSIBLE)
            advanceUntilIdle()

            val esperados = CatalogoAtaques.escenarios.filter { it.dificultad == Dificultad.IMPOSIBLE }
            val vistos = mutableListOf<EscenarioAtaque>()

            var guarda = 0
            while (!vm.estado.value.nivelCompletado && guarda++ < 100) {
                val esc = vm.estado.value.escenario!!
                assertEquals(Dificultad.IMPOSIBLE, esc.dificultad)
                vistos.add(esc)
                vm.onBloquear()
                advanceUntilIdle()
                vm.onSiguienteAtaque()
            }

            assertTrue(vm.estado.value.nivelCompletado)
            assertNull(vm.estado.value.escenario)
            assertEquals(esperados, vistos)
        }

    @Test
    fun `decidir en Imposible aplica consecuencias y registra evento (marcador vivo)`() =
        runTest(dispatcher) {
            val (partidaDao, eventoDao) = contexto()
            // Escenario IMPOSIBLE malicioso en el 22: bloquear = acierto.
            val vm = crearViewModel(
                partidaDao, eventoDao, listOf(imposibleEnPuerto(22)),
                nivel = Dificultad.IMPOSIBLE, seleccionar = { _, _ -> 0 },
            )
            advanceUntilIdle()

            vm.onBloquear()
            advanceUntilIdle()

            val resultado = vm.estado.value.ultimoResultado!!
            assertTrue(resultado.acierto)
            // El marcador NO se apaga en Imposible: los deltas se aplican igual que en 1-3.
            assertEquals(15, resultado.deltaPuntaje)
            assertEquals(50, resultado.deltaDinero)
            assertEquals(1, vm.estado.value.rondas)
            // Y el evento se registra (para que Estadísticas lo cuente).
            assertEquals(1, eventoDao.insertados.size)
            assertEquals(22, eventoDao.insertados.first().puertoDestino)
            assertTrue(eventoDao.insertados.first().acierto)
        }

    @Test
    fun `el puente NO sugiere en Imposible aunque se repita la familia`() = runTest(dispatcher) {
        val (partidaDao, eventoDao) = contexto()
        // 3 puertos distintos de "Bases de datos", todos IMPOSIBLE y maliciosos (DENY = acierto).
        val escenarios = listOf(
            imposibleEnPuerto(3306), imposibleEnPuerto(5432), imposibleEnPuerto(27017),
        )
        val vm = crearViewModel(
            partidaDao, eventoDao, escenarios,
            nivel = Dificultad.IMPOSIBLE, seleccionar = { actual, _ -> (actual ?: -1) + 1 },
        )
        advanceUntilIdle()

        // Tres aciertos de la misma familia: en cualquier otro nivel dispararía; en Imposible no.
        repeat(3) {
            vm.onBloquear()
            advanceUntilIdle()
            assertNull(vm.estado.value.ultimoResultado!!.sugerencia)
            if (it < 2) vm.onSiguienteAtaque()
        }
    }

    @Test
    fun `la auto-aplicacion de reglas SI actua en Imposible (consistencia)`() = runTest(dispatcher) {
        val (partidaDao, eventoDao) = contexto()
        // Regla DENY en el 22; escenario IMPOSIBLE malicioso en el 22 → la regla resuelve la ronda.
        val reglaDao = FakeReglaFirewallDao(listOf(reglaDe(22, AccionFirewall.DENY)))
        val vm = crearViewModel(
            partidaDao, eventoDao, listOf(imposibleEnPuerto(22)),
            nivel = Dificultad.IMPOSIBLE, seleccionar = { _, _ -> 0 }, reglaDao = reglaDao,
        )
        advanceUntilIdle()

        val resultado = vm.estado.value.ultimoResultado!!
        assertTrue(resultado.automatizada)
        assertTrue(resultado.acierto)
        assertEquals(22, resultado.automatizadaPor!!.puerto)
        // Se registra el evento (historial veraz), como en los demás niveles.
        assertEquals(1, eventoDao.insertados.size)
        assertEquals(22, eventoDao.insertados.first().puertoDestino)
    }

    private companion object {
        /** Copia local del umbral del puente para no acoplar el test al valor exacto. */
        const val UMBRAL = 3
    }
}
