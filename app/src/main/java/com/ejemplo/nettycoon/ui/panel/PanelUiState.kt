package com.ejemplo.nettycoon.ui.panel

import com.ejemplo.nettycoon.data.local.entity.EstadoPartida
import com.ejemplo.nettycoon.domain.firewall.Rango
import com.ejemplo.nettycoon.domain.firewall.puntajeParaSiguienteRango
import com.ejemplo.nettycoon.domain.firewall.rangoPorPuntaje
import com.ejemplo.nettycoon.domain.model.ResultadoRonda

/**
 * Estado de UI del panel del juego (FASE 1 del bucle).
 *
 * Modela lo que la pantalla necesita pintar en cada momento:
 * - [partida]: estado actual de la "empresa virtual" (puntaje, salud, dinero, nivel).
 * - [ultimaRonda]: resultado de la última simulación de ataque, o `null` si aún no hubo ninguna.
 * - [cargando]: `true` durante la carga inicial de la partida y mientras corre una ronda
 *   (incluida la consulta a la API geo-IP).
 * - [error]: mensaje a mostrar si algo falla, o `null`.
 *
 * Reutiliza [EstadoPartida] y [ResultadoRonda] tal cual; en esta fase no se introducen
 * modelos de presentación adicionales.
 */
data class PanelUiState(
    val partida: EstadoPartida? = null,
    val ultimaRonda: ResultadoRonda? = null,
    val cargando: Boolean = false,
    /**
     * Ancla de regeneración vigente (epoch millis), para derivar en la UI el contador de la próxima
     * recuperación de salud (E2.1). `null` hasta que se resuelve la carga inicial. No es fuente de
     * vida: la salud siempre sale de la regen real sobre el ancla en prefs.
     */
    val anclaRegen: Long? = null,
    val error: String? = null,
) {
    /**
     * Rango del jugador (E4), DERIVADO del puntaje de la partida; `null` mientras no hay partida
     * cargada.
     *
     * Se calcula aquí, como propiedad del estado, y no en el ViewModel: al ser una función pura sin
     * estado propio no hay nada que orquestar, y así el rango nunca puede quedar desincronizado del
     * puntaje que se está mostrando. Tampoco se persiste (ver [rangoPorPuntaje]).
     */
    val rango: Rango? get() = partida?.let { rangoPorPuntaje(it.puntaje) }

    /** Puntos que faltan para el siguiente rango; `null` sin partida o ya en el rango máximo. */
    val puntosParaSiguienteRango: Int?
        get() = partida?.let { puntajeParaSiguienteRango(it.puntaje) }
}
