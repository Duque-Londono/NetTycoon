package com.ejemplo.nettycoon.ui.firewall

import com.ejemplo.nettycoon.data.local.entity.AccionFirewall
import com.ejemplo.nettycoon.data.local.entity.EstadoPartida
import com.ejemplo.nettycoon.data.local.entity.ReglaFirewall
import com.ejemplo.nettycoon.data.repository.PartidaRepository
import com.ejemplo.nettycoon.data.repository.ReglaFirewallRepository
import com.ejemplo.nettycoon.domain.firewall.CuposReglas
import com.ejemplo.nettycoon.domain.firewall.Rango
import com.ejemplo.nettycoon.domain.firewall.UmbralesRango
import com.ejemplo.nettycoon.data.local.dao.EstadoPartidaDao
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
 * Pruebas JVM del [FirewallViewModel]: el CRUD completo y la validación del formulario.
 *
 * `viewModelScope` corre en `Dispatchers.Main`, que no existe en la JVM, así que se sustituye
 * por un [StandardTestDispatcher] controlado por el test (`setMain`/`resetMain`). Con
 * `advanceUntilIdle()` se dejan terminar las corrutinas antes de comprobar el estado.
 *
 * Se usa el [ReglaFirewallRepository] real sobre un DAO fake reactivo, para probar el mismo
 * camino que recorre producción.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirewallViewModelTest {

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

    /**
     * Crea el ViewModel. Por defecto el jugador va con puntaje de EXPERTO (cupo 12) para que los
     * tests que NO van del cupo no choquen con el; los que sí van del cupo pasan su puntaje.
     */
    private fun crearViewModel(
        dao: FakeReglaFirewallDaoReactivo,
        puntaje: Int = UmbralesRango.PUNTAJE_EXPERTO,
    ): FirewallViewModel {
        val partidaDao: EstadoPartidaDao = FakeEstadoPartidaDao().apply {
            almacen[uid] = EstadoPartida(owner = uid, puntaje = puntaje)
        }
        return FirewallViewModel(
            uid,
            ReglaFirewallRepository(dao),
            PartidaRepository(partidaDao),
        )
    }

    private fun regla(
        id: Long,
        puerto: Int = 443,
        ip: String? = null,
        accion: AccionFirewall = AccionFirewall.DENY,
        activa: Boolean = true,
        owner: String = uid,
    ) = ReglaFirewall(id = id, owner = owner, puerto = puerto, ip = ip, accion = accion, activa = activa)

    @Test
    fun `al iniciar carga las reglas del usuario y deja de cargar`() = runTest {
        val dao = FakeReglaFirewallDaoReactivo(listOf(regla(id = 1), regla(id = 2, puerto = 22)))
        val viewModel = crearViewModel(dao)

        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertEquals(2, estado.reglas.size)
        assertEquals(false, estado.cargando)
        assertNull(estado.error)
    }

    @Test
    fun `no muestra las reglas de otros usuarios`() = runTest {
        val dao = FakeReglaFirewallDaoReactivo(
            listOf(regla(id = 1), regla(id = 2, owner = "otro-usuario")),
        )
        val viewModel = crearViewModel(dao)

        advanceUntilIdle()

        assertEquals(listOf(1L), viewModel.estado.value.reglas.map { it.id })
    }

    @Test
    fun `sinReglas es true cuando termino de cargar y la lista esta vacia`() = runTest {
        val viewModel = crearViewModel(FakeReglaFirewallDaoReactivo())

        advanceUntilIdle()

        assertTrue(viewModel.estado.value.sinReglas)
    }

    @Test
    fun `crear regla valida la guarda con el owner correcto y limpia el formulario`() = runTest {
        val dao = FakeReglaFirewallDaoReactivo()
        val viewModel = crearViewModel(dao)
        advanceUntilIdle()

        viewModel.onPuertoCambiado("443")
        viewModel.onIpCambiada("203.0.113.9")
        viewModel.onAccionCambiada(AccionFirewall.ALLOW)
        viewModel.crearRegla()
        advanceUntilIdle()

        val guardada = dao.reglas.single()
        assertEquals(uid, guardada.owner)
        assertEquals(443, guardada.puerto)
        assertEquals("203.0.113.9", guardada.ip)
        assertEquals(AccionFirewall.ALLOW, guardada.accion)
        assertTrue(guardada.activa)

        // La lista de la UI se actualiza sola (observación reactiva) y el formulario se vacía.
        val estado = viewModel.estado.value
        assertEquals(1, estado.reglas.size)
        assertEquals("", estado.puertoTexto)
        assertEquals("", estado.ipTexto)
        assertNull(estado.errorFormulario)
    }

    @Test
    fun `crear regla sin ip la guarda como comodin null`() = runTest {
        val dao = FakeReglaFirewallDaoReactivo()
        val viewModel = crearViewModel(dao)
        advanceUntilIdle()

        viewModel.onPuertoCambiado("80")
        viewModel.crearRegla()
        advanceUntilIdle()

        assertNull(dao.reglas.single().ip)
    }

    @Test
    fun `crear regla con puerto fuera de rango no toca la base de datos`() = runTest {
        val dao = FakeReglaFirewallDaoReactivo()
        val viewModel = crearViewModel(dao)
        advanceUntilIdle()

        viewModel.onPuertoCambiado("65536")
        viewModel.crearRegla()
        advanceUntilIdle()

        assertTrue(dao.reglas.isEmpty())
        assertNotNull(viewModel.estado.value.errorFormulario)
        // El puerto escrito se conserva para que el usuario pueda corregirlo.
        assertEquals("65536", viewModel.estado.value.puertoTexto)
    }

    @Test
    fun `crear regla con ip mal formada no toca la base de datos`() = runTest {
        val dao = FakeReglaFirewallDaoReactivo()
        val viewModel = crearViewModel(dao)
        advanceUntilIdle()

        viewModel.onPuertoCambiado("443")
        viewModel.onIpCambiada("203.0.113")
        viewModel.crearRegla()
        advanceUntilIdle()

        assertTrue(dao.reglas.isEmpty())
        assertNotNull(viewModel.estado.value.errorFormulario)
    }

    @Test
    fun `editar el formulario borra el error de validacion anterior`() = runTest {
        val viewModel = crearViewModel(FakeReglaFirewallDaoReactivo())
        advanceUntilIdle()

        viewModel.crearRegla() // puerto vacío -> error
        advanceUntilIdle()
        assertNotNull(viewModel.estado.value.errorFormulario)

        viewModel.onPuertoCambiado("443")

        assertNull(viewModel.estado.value.errorFormulario)
    }

    @Test
    fun `el campo puerto descarta lo que no sean digitos y limita a cinco`() = runTest {
        val viewModel = crearViewModel(FakeReglaFirewallDaoReactivo())
        advanceUntilIdle()

        viewModel.onPuertoCambiado("44a3")
        assertEquals("443", viewModel.estado.value.puertoTexto)

        viewModel.onPuertoCambiado("1234567")
        assertEquals("12345", viewModel.estado.value.puertoTexto)
    }

    @Test
    fun `alternar activa invierte el estado de la regla y se refleja en la lista`() = runTest {
        val dao = FakeReglaFirewallDaoReactivo(listOf(regla(id = 1, activa = true)))
        val viewModel = crearViewModel(dao)
        advanceUntilIdle()

        viewModel.alternarActiva(viewModel.estado.value.reglas.single())
        advanceUntilIdle()

        assertEquals(false, dao.reglas.single().activa)
        assertEquals(false, viewModel.estado.value.reglas.single().activa)

        viewModel.alternarActiva(viewModel.estado.value.reglas.single())
        advanceUntilIdle()

        assertTrue(dao.reglas.single().activa)
    }

    @Test
    fun `desactivar una regla la saca de las que evalua el motor`() = runTest {
        val dao = FakeReglaFirewallDaoReactivo(listOf(regla(id = 1)))
        val viewModel = crearViewModel(dao)
        advanceUntilIdle()

        viewModel.alternarActiva(viewModel.estado.value.reglas.single())
        advanceUntilIdle()

        // Es la consulta que usa ProcesarAtaqueUseCase en cada ronda.
        assertTrue(dao.obtenerActivasPorOwner(uid).isEmpty())
        // Pero la regla sigue existiendo para el jugador.
        assertEquals(1, viewModel.estado.value.reglas.size)
    }

    @Test
    fun `eliminar quita la regla de la lista`() = runTest {
        val dao = FakeReglaFirewallDaoReactivo(listOf(regla(id = 1), regla(id = 2, puerto = 22)))
        val viewModel = crearViewModel(dao)
        advanceUntilIdle()

        viewModel.eliminarRegla(viewModel.estado.value.reglas.first { it.id == 1L })
        advanceUntilIdle()

        assertEquals(listOf(2L), viewModel.estado.value.reglas.map { it.id })
    }

    @Test
    fun `un fallo al guardar se publica como error y se puede limpiar`() = runTest {
        val dao = FakeReglaFirewallDaoReactivo()
        dao.errorAlEscribir = IllegalStateException("base de datos caida")
        val viewModel = crearViewModel(dao)
        advanceUntilIdle()

        viewModel.onPuertoCambiado("443")
        viewModel.crearRegla()
        advanceUntilIdle()

        val mensaje = viewModel.estado.value.error
        assertNotNull(mensaje)
        assertTrue(mensaje!!.contains("base de datos caida"))

        viewModel.limpiarError()

        assertNull(viewModel.estado.value.error)
    }
    // --- Cupo de reglas activas (E5) ---

    @Test
    fun `el cupo sale del rango, que sale del puntaje`() = runTest {
        val vm = crearViewModel(FakeReglaFirewallDaoReactivo(), puntaje = 0)
        advanceUntilIdle()

        assertEquals(Rango.APRENDIZ, vm.estado.value.rango)
        assertEquals(CuposReglas.CUPO_APRENDIZ, vm.estado.value.cupo)
        assertTrue(vm.estado.value.puedeCrear)
        assertFalse(vm.estado.value.sobreCupo)
    }

    @Test
    fun `bajo el cupo se puede crear`() = runTest {
        // 2 activas con cupo 3 (Aprendiz): queda hueco.
        val dao = FakeReglaFirewallDaoReactivo(listOf(regla(1, puerto = 22), regla(2, puerto = 80)))
        val vm = crearViewModel(dao, puntaje = 0)
        advanceUntilIdle()

        assertTrue(vm.estado.value.puedeCrear)
        vm.onPuertoCambiado("443")
        vm.crearRegla()
        advanceUntilIdle()

        assertEquals(3, dao.reglas.size)
        assertNull(vm.estado.value.errorFormulario)
    }

    @Test
    fun `en la frontera exacta del cupo ya no se puede crear y NO se escribe en Room`() = runTest {
        // Justo en el tope: 3 activas con cupo 3.
        val dao = FakeReglaFirewallDaoReactivo(
            listOf(regla(1, puerto = 22), regla(2, puerto = 80), regla(3, puerto = 443)),
        )
        val vm = crearViewModel(dao, puntaje = 0)
        advanceUntilIdle()

        assertFalse(vm.estado.value.puedeCrear)
        vm.onPuertoCambiado("3306")
        vm.crearRegla()
        advanceUntilIdle()

        assertEquals("No debe haberse creado ninguna regla", 3, dao.reglas.size)
        assertNotNull("Debe explicar el cupo", vm.estado.value.errorFormulario)
    }

    @Test
    fun `las reglas INACTIVAS no consumen cupo`() = runTest {
        // 3 creadas pero solo 2 activas, con cupo 3: sigue habiendo hueco.
        val dao = FakeReglaFirewallDaoReactivo(
            listOf(
                regla(1, puerto = 22),
                regla(2, puerto = 80),
                regla(3, puerto = 443, activa = false),
            ),
        )
        val vm = crearViewModel(dao, puntaje = 0)
        advanceUntilIdle()

        assertEquals(3, vm.estado.value.reglas.size)
        assertEquals(2, vm.estado.value.reglasActivas)
        assertTrue(vm.estado.value.puedeCrear)
    }

    @Test
    fun `desactivar libera cupo y permite crear de nuevo`() = runTest {
        val dao = FakeReglaFirewallDaoReactivo(
            listOf(regla(1, puerto = 22), regla(2, puerto = 80), regla(3, puerto = 443)),
        )
        val vm = crearViewModel(dao, puntaje = 0)
        advanceUntilIdle()
        assertFalse(vm.estado.value.puedeCrear)

        // La salida: desactivar (nunca se bloquea).
        vm.alternarActiva(dao.reglas.first { it.id == 3L })
        advanceUntilIdle()

        assertTrue(vm.estado.value.puedeCrear)
        assertEquals("La regla se conserva, solo queda inactiva", 3, dao.reglas.size)
    }

    @Test
    fun `reactivar en el tope se bloquea (no se puede esquivar el cupo)`() = runTest {
        // 3 activas + 1 inactiva, cupo 3. Reactivar la cuarta dejaría 4 activas.
        val dao = FakeReglaFirewallDaoReactivo(
            listOf(
                regla(1, puerto = 22),
                regla(2, puerto = 80),
                regla(3, puerto = 443),
                regla(4, puerto = 3306, activa = false),
            ),
        )
        val vm = crearViewModel(dao, puntaje = 0)
        advanceUntilIdle()

        vm.alternarActiva(dao.reglas.first { it.id == 4L })
        advanceUntilIdle()

        assertFalse("La regla 4 debe seguir inactiva", dao.reglas.first { it.id == 4L }.activa)
        assertEquals(3, vm.estado.value.reglasActivas)
        assertNotNull(vm.estado.value.error)
    }

    @Test
    fun `sobre el cupo tras bajar de rango no se crea nada y NO se borra ni desactiva ninguna`() = runTest {
        // 5 activas (cupo de Técnico) y el jugador cae a Aprendiz, cuyo cupo es 3.
        val dao = FakeReglaFirewallDaoReactivo(
            listOf(
                regla(1, puerto = 22), regla(2, puerto = 80), regla(3, puerto = 443),
                regla(4, puerto = 3306), regla(5, puerto = 25),
            ),
        )
        val vm = crearViewModel(dao, puntaje = 0)
        advanceUntilIdle()

        val estado = vm.estado.value
        assertTrue(estado.sobreCupo)
        assertEquals(5, estado.reglasActivas)
        assertEquals(CuposReglas.CUPO_APRENDIZ, estado.cupo)
        assertFalse(estado.puedeCrear)

        vm.onPuertoCambiado("5432")
        vm.crearRegla()
        advanceUntilIdle()

        // Lo esencial: NADA se toca. Ni se crea, ni se borra, ni se desactiva.
        assertEquals(5, dao.reglas.size)
        assertTrue("Ninguna regla debe haberse desactivado", dao.reglas.all { it.activa })
        assertNotNull(vm.estado.value.errorFormulario)
    }

    @Test
    fun `eliminar nunca se bloquea, ni estando sobre el cupo`() = runTest {
        val dao = FakeReglaFirewallDaoReactivo(
            listOf(
                regla(1, puerto = 22), regla(2, puerto = 80), regla(3, puerto = 443),
                regla(4, puerto = 3306), regla(5, puerto = 25),
            ),
        )
        val vm = crearViewModel(dao, puntaje = 0)
        advanceUntilIdle()
        assertTrue(vm.estado.value.sobreCupo)

        vm.eliminarRegla(dao.reglas.first { it.id == 5L })
        advanceUntilIdle()

        assertEquals(4, dao.reglas.size)
    }

    @Test
    fun `subir de rango sube el cupo`() = runTest {
        val dao = FakeReglaFirewallDaoReactivo(
            listOf(regla(1, puerto = 22), regla(2, puerto = 80), regla(3, puerto = 443)),
        )
        // Mismo número de reglas, pero con rango Analista el cupo es 8.
        val vm = crearViewModel(dao, puntaje = UmbralesRango.PUNTAJE_ANALISTA)
        advanceUntilIdle()

        assertEquals(Rango.ANALISTA, vm.estado.value.rango)
        assertEquals(CuposReglas.CUPO_ANALISTA, vm.estado.value.cupo)
        assertTrue(vm.estado.value.puedeCrear)
    }
}
