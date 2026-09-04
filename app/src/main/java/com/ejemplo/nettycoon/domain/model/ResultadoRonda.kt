package com.ejemplo.nettycoon.domain.model

import com.ejemplo.nettycoon.data.local.entity.EstadoPartida
import com.ejemplo.nettycoon.data.local.entity.EventoAtaque

/**
 * Resultado completo de procesar una ronda del juego: el ataque que llegó, cómo lo evaluó
 * el motor, el evento persistido y el estado de partida ya actualizado.
 *
 * Es lo que un ViewModel (fase de UI, posterior) consumirá para renderizar la ronda.
 */
data class ResultadoRonda(
    val ataque: Ataque,
    val evaluacion: ResultadoEvaluacion,
    /** Evento tal como quedó registrado (incluye su `id` generado por Room). */
    val evento: EventoAtaque,
    /** Estado de partida ya con las consecuencias aplicadas. */
    val estadoPartida: EstadoPartida,
)
