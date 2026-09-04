package com.ejemplo.nettycoon.domain

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ejemplo.nettycoon.data.local.NetTycoonDatabase
import com.ejemplo.nettycoon.data.local.entity.AccionFirewall
import com.ejemplo.nettycoon.data.local.entity.ReglaFirewall
import com.ejemplo.nettycoon.data.local.entity.ResultadoEvento
import com.ejemplo.nettycoon.data.repository.EventoAtaqueRepository
import com.ejemplo.nettycoon.data.repository.PartidaRepository
import com.ejemplo.nettycoon.data.repository.ReglaFirewallRepository
import com.ejemplo.nettycoon.domain.firewall.BalancePartida
import com.ejemplo.nettycoon.domain.firewall.ProcesarAtaqueUseCase
import com.ejemplo.nettycoon.domain.model.Ataque
import com.ejemplo.nettycoon.domain.model.CategoriaResultado
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba instrumentada E2E: ejecuta [ProcesarAtaqueUseCase] con los repositorios REALES
 * sobre una base Room in-memory, validando el cableado completo (evaluación → persistir
 * evento → actualizar partida) contra la BD real generada por KSP.
 */
@RunWith(AndroidJUnit4::class)
class ProcesarAtaqueIntegracionTest {

    private lateinit var db: NetTycoonDatabase
    private lateinit var useCase: ProcesarAtaqueUseCase
    private lateinit var eventoRepo: EventoAtaqueRepository
    private lateinit var partidaRepo: PartidaRepository
    private val uid = "uid-integracion"

    @Before
    fun crear() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NetTycoonDatabase::class.java,
        ).build()
        val reglaRepo = ReglaFirewallRepository(db.reglaFirewallDao())
        eventoRepo = EventoAtaqueRepository(db.eventoAtaqueDao())
        partidaRepo = PartidaRepository(db.estadoPartidaDao())
        useCase = ProcesarAtaqueUseCase(reglaRepo, eventoRepo, partidaRepo)
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun rondaCompletaPersisteEventoYActualizaPartida() = runBlocking {
        // Regla real DENY para 443.
        db.reglaFirewallDao().insertar(
            ReglaFirewall(owner = uid, puerto = 443, ip = null, accion = AccionFirewall.DENY),
        )
        val ataque = Ataque("203.0.113.9", puertoDestino = 443, esMalicioso = true)

        val ronda = useCase.ejecutarRonda(uid, ataque)

        // Bloqueo correcto.
        assertEquals(CategoriaResultado.BLOQUEO_CORRECTO, ronda.evaluacion.categoria)

        // Evento realmente en la BD.
        val eventos = eventoRepo.observarEventos(uid).first()
        assertEquals(1, eventos.size)
        assertEquals(ResultadoEvento.BLOQUEADO, eventos.first().resultado)
        assertTrue(eventos.first().acierto)

        // Partida realmente actualizada en la BD.
        val partida = partidaRepo.observarPartida(uid).first()
        assertEquals(BalancePartida.PUNTAJE_BLOQUEO_CORRECTO, partida?.puntaje)
    }
}
