package com.ejemplo.nettycoon.ui.panel

import com.ejemplo.nettycoon.data.local.entity.EstadoPartida
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
)
