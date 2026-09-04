package com.ejemplo.nettycoon.domain.firewall

import com.ejemplo.nettycoon.data.local.entity.EventoAtaque
import com.ejemplo.nettycoon.data.repository.EventoAtaqueRepository
import com.ejemplo.nettycoon.data.repository.PartidaRepository
import com.ejemplo.nettycoon.data.repository.ReglaFirewallRepository
import com.ejemplo.nettycoon.domain.model.Ataque
import com.ejemplo.nettycoon.domain.model.ResultadoEvaluacion
import com.ejemplo.nettycoon.domain.model.ResultadoRonda

/**
 * Caso de uso que orquesta una ronda completa del bucle del juego, uniendo el motor puro
 * (Fase A) con la persistencia Room (M2). `suspend`, sin dependencias de Android/UI.
 *
 * Ciclo:
 * 1. Obtener las reglas activas del `owner` y traducirlas a dominio.
 * 2. Recibir un ataque o generarlo con [GeneradorAtaques].
 * 3. Evaluar con [MotorFirewall].
 * 4. Construir el [EventoAtaque] a partir del resultado y persistirlo.
 * 5. Obtener/crear la partida, aplicar consecuencias y guardarla.
 *
 * **Instanciación (sin DI nuevo):** un ViewModel (fase de UI posterior) construirá este caso
 * de uso pasándole los tres repositorios, que a su vez se crean desde los DAOs del singleton
 * `NetTycoonDatabase.obtenerInstancia(context)`.
 */
class ProcesarAtaqueUseCase(
    private val reglaRepo: ReglaFirewallRepository,
    private val eventoRepo: EventoAtaqueRepository,
    private val partidaRepo: PartidaRepository,
    private val generador: GeneradorAtaques = GeneradorAtaques(),
) {
    /**
     * Ejecuta una ronda para [uid]. Si [ataque] es `null`, se genera uno.
     * Devuelve el [ResultadoRonda] con todo lo ocurrido.
     */
    suspend fun ejecutarRonda(uid: String, ataque: Ataque? = null): ResultadoRonda {
        val ataqueRonda = ataque ?: generador.generar()

        // 1 + 2: reglas activas → dominio.
        val reglas = reglaRepo.obtenerReglasActivas(uid).aReglasEvaluables()

        // 3: evaluación pura.
        val evaluacion = MotorFirewall.evaluar(ataqueRonda, reglas)

        // 4: construir y persistir el evento; recuperar su id generado.
        val evento = construirEvento(uid, ataqueRonda, evaluacion)
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

    /** Mapea el [ResultadoEvaluacion] + [Ataque] a la entidad [EventoAtaque] (pais/isp → M4). */
    private fun construirEvento(
        uid: String,
        ataque: Ataque,
        evaluacion: ResultadoEvaluacion,
    ): EventoAtaque = EventoAtaque(
        owner = uid,
        ipAtacante = ataque.ipAtacante,
        puertoDestino = ataque.puertoDestino,
        pais = null,
        isp = null,
        resultado = evaluacion.resultado,
        acierto = evaluacion.acierto,
    )
}
