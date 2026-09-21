package com.ejemplo.nettycoon.ui.estadisticas

import com.ejemplo.nettycoon.data.local.entity.EventoAtaque
import com.ejemplo.nettycoon.data.local.entity.ResultadoEvento
import com.ejemplo.nettycoon.data.repository.EventoAtaqueRepository
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
 * Pruebas JVM del [EstadisticasViewModel]: cálculo de métricas (total, tasa de acierto y
 * familias dominadas/flojas) a partir del historial de `EventoAtaque`, más los estados vacío y
 * de error.
 *
 * `viewModelScope` corre en `Dispatchers.Main`, que no existe en la JVM, así que se sustituye por
 * un [StandardTestDispatcher] controlado por el test. Con `advanceUntilIdle()` se dejan terminar
 * las corrutinas antes de comprobar el estado.
 *
 * Se usa el [EventoAtaqueRepository] real sobre un DAO fake reactivo, para probar el mismo camino
 * que recorre producción (sin interfaces de repositorio).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EstadisticasViewModelTest {

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

    private fun crearViewModel(dao: FakeEventoAtaqueDaoReactivo): EstadisticasViewModel =
        EstadisticasViewModel(uid, EventoAtaqueRepository(dao))

    private fun evento(
        id: Long,
        puerto: Int? = 443,
        acierto: Boolean = true,
        owner: String = uid,
        resultado: ResultadoEvento = ResultadoEvento.BLOQUEADO,
    ) = EventoAtaque(
        id = id,
        owner = owner,
        ipAtacante = "1.2.3.4",
        puertoDestino = puerto,
        resultado = resultado,
        acierto = acierto,
        ocurridoEn = id, // orden estable en el fake
    )

    @Test
    fun `sin historial queda en estado vacio y sin cargar`() = runTest {
        val viewModel = crearViewModel(FakeEventoAtaqueDaoReactivo())

        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertFalse(estado.cargando)
        assertTrue(estado.estaVacio)
        assertEquals(0, estado.totalAtaques)
        assertEquals(0, estado.tasaAciertoPct)
        assertTrue(estado.familias.isEmpty())
        assertNull(estado.error)
    }

    @Test
    fun `cuenta el total de ataques del usuario`() = runTest {
        val dao = FakeEventoAtaqueDaoReactivo(
            listOf(evento(1), evento(2), evento(3)),
        )
        val viewModel = crearViewModel(dao)

        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertFalse(estado.cargando)
        assertFalse(estado.estaVacio)
        assertEquals(3, estado.totalAtaques)
    }

    @Test
    fun `calcula la tasa de acierto redondeada`() = runTest {
        // 2 de 3 aciertos = 66.67% -> 67 redondeado.
        val dao = FakeEventoAtaqueDaoReactivo(
            listOf(
                evento(1, acierto = true),
                evento(2, acierto = true),
                evento(3, acierto = false),
            ),
        )
        val viewModel = crearViewModel(dao)

        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertEquals(2, estado.aciertos)
        assertEquals(67, estado.tasaAciertoPct)
    }

    @Test
    fun `agrupa por familia derivada del puerto`() = runTest {
        // 22 y 3389 -> "Acceso remoto"; 443 -> "Web".
        val dao = FakeEventoAtaqueDaoReactivo(
            listOf(
                evento(1, puerto = 22),
                evento(2, puerto = 3389),
                evento(3, puerto = 443),
            ),
        )
        val viewModel = crearViewModel(dao)

        advanceUntilIdle()

        val familias = viewModel.estado.value.familias.associateBy { it.nombre }
        assertEquals(2, familias["Acceso remoto"]?.total)
        assertEquals(1, familias["Web"]?.total)
    }

    @Test
    fun `un puerto sin mapa y un puerto nulo caen en Otros`() = runTest {
        val dao = FakeEventoAtaqueDaoReactivo(
            listOf(
                evento(1, puerto = 9999),
                evento(2, puerto = null),
            ),
        )
        val viewModel = crearViewModel(dao)

        advanceUntilIdle()

        val otros = viewModel.estado.value.familias.first { it.nombre == MapeoFamilias.OTROS }
        assertEquals(2, otros.total)
    }

    @Test
    fun `clasifica familias como dominada neutra y floja segun el umbral`() = runTest {
        // Acceso remoto (22): 2/2 = 100% -> DOMINADA (>=70).
        // Web (443): 1/2 = 50% -> NEUTRA (>=50 y <70).
        // Bases de datos (3306): 0/2 = 0% -> FLOJA (<50).
        val dao = FakeEventoAtaqueDaoReactivo(
            listOf(
                evento(1, puerto = 22, acierto = true),
                evento(2, puerto = 22, acierto = true),
                evento(3, puerto = 443, acierto = true),
                evento(4, puerto = 443, acierto = false),
                evento(5, puerto = 3306, acierto = false),
                evento(6, puerto = 3306, acierto = false),
            ),
        )
        val viewModel = crearViewModel(dao)

        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertEquals(
            NivelDominio.DOMINADA,
            estado.familias.first { it.nombre == "Acceso remoto" }.nivel,
        )
        assertEquals(
            NivelDominio.NEUTRA,
            estado.familias.first { it.nombre == "Web" }.nivel,
        )
        assertEquals(
            NivelDominio.FLOJA,
            estado.familias.first { it.nombre == "Bases de datos" }.nivel,
        )
        // Y las vistas derivadas separan bien dominadas de flojas.
        assertTrue(estado.familiasDominadas.any { it.nombre == "Acceso remoto" })
        assertTrue(estado.familiasFlojas.any { it.nombre == "Bases de datos" })
        assertFalse(estado.familiasDominadas.any { it.nombre == "Web" })
    }

    @Test
    fun `ignora eventos de otros usuarios`() = runTest {
        val dao = FakeEventoAtaqueDaoReactivo(
            listOf(
                evento(1, owner = uid),
                evento(2, owner = "otro-usuario"),
                evento(3, owner = uid),
            ),
        )
        val viewModel = crearViewModel(dao)

        advanceUntilIdle()

        assertEquals(2, viewModel.estado.value.totalAtaques)
    }

    @Test
    fun `las familias se ordenan de mayor a menor tasa de acierto`() = runTest {
        val dao = FakeEventoAtaqueDaoReactivo(
            listOf(
                evento(1, puerto = 3306, acierto = false), // Bases de datos: 0%
                evento(2, puerto = 22, acierto = true),    // Acceso remoto: 100%
            ),
        )
        val viewModel = crearViewModel(dao)

        advanceUntilIdle()

        val familias = viewModel.estado.value.familias
        assertEquals("Acceso remoto", familias.first().nombre)
        assertEquals("Bases de datos", familias.last().nombre)
    }

    @Test
    fun `un fallo al observar publica estado de error y deja de cargar`() = runTest {
        val dao = FakeEventoAtaqueDaoReactivo(listOf(evento(1)))
        dao.errorAlObservar = RuntimeException("boom")
        val viewModel = crearViewModel(dao)

        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertFalse(estado.cargando)
        assertNotNull(estado.error)
        assertFalse(estado.estaVacio) // error no es lo mismo que vacío
    }
}
