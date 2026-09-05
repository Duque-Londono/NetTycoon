package com.ejemplo.nettycoon.domain.firewall

import com.ejemplo.nettycoon.data.local.entity.AccionFirewall
import com.ejemplo.nettycoon.data.local.entity.ReglaFirewall
import com.ejemplo.nettycoon.data.local.entity.ResultadoEvento
import com.ejemplo.nettycoon.data.repository.EventoAtaqueRepository
import com.ejemplo.nettycoon.data.repository.GeoIpRepository
import com.ejemplo.nettycoon.data.repository.GeoIpResultado
import com.ejemplo.nettycoon.data.repository.PartidaRepository
import com.ejemplo.nettycoon.data.repository.ReglaFirewallRepository
import com.ejemplo.nettycoon.domain.firewall.fakes.FakeEstadoPartidaDao
import com.ejemplo.nettycoon.domain.firewall.fakes.FakeEventoAtaqueDao
import com.ejemplo.nettycoon.domain.firewall.fakes.FakeReglaFirewallDao
import com.ejemplo.nettycoon.domain.model.Ataque
import com.ejemplo.nettycoon.domain.model.CategoriaResultado
import com.ejemplo.nettycoon.domain.model.DatosGeoIp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fake de [GeoIpRepository] para tests JVM (sin red). Devuelve siempre el [GeoIpResultado]
 * que se le configure. Aprovecha que el repo es `open` (mismo patrón de repos concretos,
 * sin interfaz).
 */
private class FakeGeoIpRepository(
    private val resultado: GeoIpResultado,
) : GeoIpRepository() {
    override suspend fun consultar(ip: String): GeoIpResultado = resultado
}

/**
 * Unit tests JVM del [ProcesarAtaqueUseCase]. Se construyen los repositorios REALES sobre
 * DAOs fake en memoria (sin tocar la API de M2 ni necesitar emulador). Cubre el ciclo
 * completo y el mapeo ResultadoEvaluacion → EventoAtaque.
 */
class ProcesarAtaqueUseCaseTest {

    private val uid = "uid-1"

    private fun useCase(
        reglas: List<ReglaFirewall>,
        geoIp: GeoIpResultado = GeoIpResultado.Error("sin red"),
    ): Triple<ProcesarAtaqueUseCase, FakeEventoAtaqueDao, FakeEstadoPartidaDao> {
        val reglaDao = FakeReglaFirewallDao(reglas)
        val eventoDao = FakeEventoAtaqueDao()
        val partidaDao = FakeEstadoPartidaDao()
        val uc = ProcesarAtaqueUseCase(
            reglaRepo = ReglaFirewallRepository(reglaDao),
            eventoRepo = EventoAtaqueRepository(eventoDao),
            partidaRepo = PartidaRepository(partidaDao),
            // Por defecto la geo-IP FALLA → pais/isp null: así los tests existentes ven el
            // mismo comportamiento que conocían.
            geoIpRepo = FakeGeoIpRepository(geoIp),
        )
        return Triple(uc, eventoDao, partidaDao)
    }

    @Test
    fun `ronda con DENY sobre ataque malicioso persiste evento y suma puntaje`() = runBlocking {
        val regla = ReglaFirewall(owner = uid, puerto = 443, ip = null, accion = AccionFirewall.DENY)
        val (uc, eventoDao, partidaDao) = useCase(listOf(regla))
        val ataque = Ataque("9.9.9.9", puertoDestino = 443, esMalicioso = true)

        val ronda = uc.ejecutarRonda(uid, ataque)

        // Evaluación correcta (bloqueo correcto).
        assertEquals(CategoriaResultado.BLOQUEO_CORRECTO, ronda.evaluacion.categoria)
        assertTrue(ronda.evaluacion.acierto)

        // Evento persistido con el mapeo correcto y con id generado.
        assertEquals(1, eventoDao.insertados.size)
        val ev = eventoDao.insertados.first()
        assertEquals(uid, ev.owner)
        assertEquals("9.9.9.9", ev.ipAtacante)
        assertEquals(443, ev.puertoDestino)
        assertEquals(ResultadoEvento.BLOQUEADO, ev.resultado)
        assertTrue(ev.acierto)
        assertNotEquals(0L, ronda.evento.id) // id generado por el DAO

        // Partida actualizada y guardada.
        assertEquals(BalancePartida.PUNTAJE_BLOQUEO_CORRECTO, ronda.estadoPartida.puntaje)
        assertEquals(ronda.estadoPartida, partidaDao.almacen[uid])
    }

    @Test
    fun `sin reglas el ataque malicioso pasa por default-DENY y es acierto`() = runBlocking {
        val (uc, eventoDao, _) = useCase(emptyList())
        val ataque = Ataque("8.8.8.8", puertoDestino = 80, esMalicioso = true)

        val ronda = uc.ejecutarRonda(uid, ataque)

        assertEquals(AccionFirewall.DENY, ronda.evaluacion.accionAplicada)
        assertEquals(CategoriaResultado.BLOQUEO_CORRECTO, ronda.evaluacion.categoria)
        assertEquals(1, eventoDao.insertados.size)
    }

    @Test
    fun `ataque legitimo permitido por ALLOW es brecha inversa - permiso correcto`() = runBlocking {
        val regla = ReglaFirewall(owner = uid, puerto = 80, ip = null, accion = AccionFirewall.ALLOW)
        val (uc, _, _) = useCase(listOf(regla))
        val ataque = Ataque("1.1.1.1", puertoDestino = 80, esMalicioso = false)

        val ronda = uc.ejecutarRonda(uid, ataque)

        assertEquals(CategoriaResultado.PERMISO_CORRECTO, ronda.evaluacion.categoria)
        assertEquals(ResultadoEvento.PERMITIDO, ronda.evento.resultado)
        assertTrue(ronda.evento.acierto)
    }

    @Test
    fun `reglas inactivas se ignoran - ataque cae en default`() = runBlocking {
        val inactiva = ReglaFirewall(owner = uid, puerto = 443, ip = null, accion = AccionFirewall.ALLOW, activa = false)
        val (uc, _, _) = useCase(listOf(inactiva))
        val ataque = Ataque("2.2.2.2", puertoDestino = 443, esMalicioso = true)

        val ronda = uc.ejecutarRonda(uid, ataque)

        // La ALLOW inactiva no aplica; default-DENY -> bloqueo correcto.
        assertEquals(AccionFirewall.DENY, ronda.evaluacion.accionAplicada)
    }

    @Test
    fun `geo-IP con exito rellena pais e isp en el evento`() = runBlocking {
        val geo = GeoIpResultado.Exito(DatosGeoIp(pais = "Colombia", isp = "ISP X"))
        val (uc, eventoDao, _) = useCase(emptyList(), geoIp = geo)
        val ataque = Ataque("9.9.9.9", puertoDestino = 443, esMalicioso = true)

        val ronda = uc.ejecutarRonda(uid, ataque)

        val ev = eventoDao.insertados.single()
        assertEquals("Colombia", ev.pais)
        assertEquals("ISP X", ev.isp)
        // La ronda se resuelve con normalidad.
        assertEquals("Colombia", ronda.evento.pais)
        assertTrue(ronda.evaluacion.acierto)
    }

    @Test
    fun `geo-IP con fallo deja pais e isp null y la ronda continua normal`() = runBlocking {
        val (uc, eventoDao, partidaDao) = useCase(
            emptyList(),
            geoIp = GeoIpResultado.Error("Error de red"),
        )
        val ataque = Ataque("8.8.8.8", puertoDestino = 80, esMalicioso = true)

        val ronda = uc.ejecutarRonda(uid, ataque)

        // Sin geo: pais/isp null, pero el evento se persiste igual.
        val ev = eventoDao.insertados.single()
        assertNull(ev.pais)
        assertNull(ev.isp)
        // Evaluación y partida intactas (default-DENY sobre ataque malicioso = acierto).
        assertEquals(CategoriaResultado.BLOQUEO_CORRECTO, ronda.evaluacion.categoria)
        assertEquals(ronda.estadoPartida, partidaDao.almacen[uid])
    }
}
