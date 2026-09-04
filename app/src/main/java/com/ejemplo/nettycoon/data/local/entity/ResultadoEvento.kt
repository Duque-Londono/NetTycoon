package com.ejemplo.nettycoon.data.local.entity

/**
 * Resultado con el que se resolvió un [EventoAtaque]: si el tráfico terminó
 * bloqueado o permitido.
 *
 * Es ortogonal a si la decisión del jugador fue un acierto (ver
 * [EventoAtaque.acierto]): se puede permitir correctamente o permitir por error.
 *
 * Se persiste como `String` (su [name]) mediante un TypeConverter.
 */
enum class ResultadoEvento {
    /** El ataque fue bloqueado. */
    BLOQUEADO,

    /** El ataque fue permitido (pasó el firewall). */
    PERMITIDO,
}
