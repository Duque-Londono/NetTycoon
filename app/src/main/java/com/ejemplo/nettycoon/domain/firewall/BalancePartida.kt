package com.ejemplo.nettycoon.domain.firewall

/**
 * Constantes de balance del juego (number balancing). Mínimas y ajustables: cambiar el
 * equilibrio no requiere tocar la lógica de [ConsecuenciasPartida].
 *
 * Criterio: **defenderse activamente es lo más recompensado** — un bloqueo correcto premia
 * más que un permiso correcto.
 */
object BalancePartida {

    // --- Puntaje ---
    /** Malicioso + DENY: bloqueo correcto (lo más recompensado). */
    const val PUNTAJE_BLOQUEO_CORRECTO = 15
    /** Legítimo + ALLOW: permiso correcto. */
    const val PUNTAJE_PERMISO_CORRECTO = 10
    /** Legítimo + DENY: falso positivo (penalización leve). */
    const val PUNTAJE_FALSO_POSITIVO = -5

    // --- Salud de la red (0–100) ---
    /** Malicioso + ALLOW: brecha (pasó un ataque real). */
    const val SALUD_BRECHA = -20
    const val SALUD_MIN = 0
    const val SALUD_MAX = 100

    // --- Dinero virtual (>= 0) ---
    const val DINERO_ACIERTO = 50
    const val DINERO_BRECHA = -100
    const val DINERO_FALSO_POSITIVO = -25
    const val DINERO_MIN = 0

    // --- Nivel ---
    /** Puntos necesarios por nivel: nivel = 1 + puntaje / PUNTOS_POR_NIVEL. */
    const val PUNTOS_POR_NIVEL = 100
    const val NIVEL_MIN = 1
}
