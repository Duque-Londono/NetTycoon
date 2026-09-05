package com.ejemplo.nettycoon.domain.firewall

import com.ejemplo.nettycoon.data.local.entity.EventoAtaque
import com.ejemplo.nettycoon.data.repository.EventoAtaqueRepository
import com.ejemplo.nettycoon.data.repository.GeoIpRepository
import com.ejemplo.nettycoon.data.repository.GeoIpResultado
import com.ejemplo.nettycoon.data.repository.PartidaRepository
import com.ejemplo.nettycoon.data.repository.ReglaFirewallRepository
import com.ejemplo.nettycoon.domain.model.Ataque
import com.ejemplo.nettycoon.domain.model.DatosGeoIp
import com.ejemplo.nettycoon.domain.model.ResultadoEvaluacion
import com.ejemplo.nettycoon.domain.model.ResultadoRonda

/**
 * Caso de uso que orquesta una ronda completa del bucle del juego, uniendo el motor puro
 * (Fase A) con la persistencia Room (M2). `suspend`, sin dependencias de Android/UI.
 *
 * Ciclo:
 * 1. Recibir un ataque o generarlo con [GeneradorAtaques].
 * 2. Enriquecer con geo-IP real la [Ataque.ipAtacante] (best-effort; ver abajo).
 * 3. Obtener las reglas activas del `owner`, traducirlas a dominio y evaluar con [MotorFirewall].
 * 4. Construir el [EventoAtaque] ya con pais/isp y persistirlo (una sola escritura).
 * 5. Obtener/crear la partida, aplicar consecuencias y guardarla.
 *
 * **Geo-IP best-effort:** la consulta a [GeoIpRepository] NUNCA rompe la ronda. Si falla o no
 * hay red, `pais`/`isp` quedan `null` y el juego continúa igual. [GeoIpRepository.consultar] ya
 * es total (no lanza); aun así envolvemos la llamada en un `try/catch` defensivo (doble red).
 *
 * **Instanciación (sin DI nuevo):** un ViewModel (fase de UI posterior) construirá este caso
 * de uso pasándole los repositorios, que a su vez se crean desde los DAOs del singleton
 * `NetTycoonDatabase.obtenerInstancia(context)` y `GeoIpRepository()`.
 */
class ProcesarAtaqueUseCase(
    private val reglaRepo: ReglaFirewallRepository,
    private val eventoRepo: EventoAtaqueRepository,
    private val partidaRepo: PartidaRepository,
    private val geoIpRepo: GeoIpRepository,
    private val generador: GeneradorAtaques = GeneradorAtaques(),
) {
    /**
     * Ejecuta una ronda para [uid]. Si [ataque] es `null`, se genera uno.
     * Devuelve el [ResultadoRonda] con todo lo ocurrido.
     */
    suspend fun ejecutarRonda(uid: String, ataque: Ataque? = null): ResultadoRonda {
        val ataqueRonda = ataque ?: generador.generar()

        // 2: geo-IP best-effort (nunca rompe la ronda; null si falla o no hay red).
        val datosGeo = consultarGeoBestEffort(ataqueRonda.ipAtacante)

        // 3: reglas activas → dominio + evaluación pura.
        val reglas = reglaRepo.obtenerReglasActivas(uid).aReglasEvaluables()
        val evaluacion = MotorFirewall.evaluar(ataqueRonda, reglas)

        // 4: construir y persistir el evento (ya con pais/isp); recuperar su id generado.
        val evento = construirEvento(uid, ataqueRonda, evaluacion, datosGeo)
        val eventoId = eventoRepo.registrarEvento(evento)
        val eventoPersistido = evento.copy(id = eventoId)

        // 5: aplicar consecuencias sobre la partida y guardar.
        val partidaActual = partidaRepo.getOrCreatePartida(uid)
        val partidaActualizada = ConsecuenciasPartida.aplicar(partidaActual, evaluacion)
        partidaRepo.actualizarPartida(partidaActualizada)

        return ResultadoRonda(
            ataque = ataqueRonda,
            evaluacion = evaluacion,
            evento = eventoPersistido,
            estadoPartida = partidaActualizada,
        )
    }

    /**
     * Consulta geo-IP en modo best-effort. Devuelve los [DatosGeoIp] si la API respondió con
     * éxito, o `null` en cualquier otro caso (error de red/HTTP, o `Throwable` inesperado). La
     * ronda nunca se interrumpe por la red.
     */
    private suspend fun consultarGeoBestEffort(ip: String): DatosGeoIp? =
        try {
            when (val resultado = geoIpRepo.consultar(ip)) {
                is GeoIpResultado.Exito -> resultado.datos
                is GeoIpResultado.Error -> null
            }
        } catch (_: Throwable) {
            null
        }

    /** Mapea el [ResultadoEvaluacion] + [Ataque] + geo-IP a la entidad [EventoAtaque]. */
    private fun construirEvento(
        uid: String,
        ataque: Ataque,
        evaluacion: ResultadoEvaluacion,
        datosGeo: DatosGeoIp?,
    ): EventoAtaque = EventoAtaque(
        owner = uid,
        ipAtacante = ataque.ipAtacante,
        puertoDestino = ataque.puertoDestino,
        pais = datosGeo?.pais,
        isp = datosGeo?.isp,
        resultado = evaluacion.resultado,
        acierto = evaluacion.acierto,
    )
}
