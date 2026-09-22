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
    // La salud es OPERATIVA: baja por INSEGURIDAD (brecha) y por INDISPONIBILIDAD (falso
    // positivo: bloquear tráfico legítimo deja la red sin servicio). La brecha es más grave.
    /** Malicioso + ALLOW: brecha (pasó un ataque real). */
    const val SALUD_BRECHA = -20
    /** Legítimo + DENY: falso positivo (la red deja de funcionar para usuarios legítimos). */
    const val SALUD_FALSO_POSITIVO = -10
    const val SALUD_MIN = 0
    const val SALUD_MAX = 100

    // --- Dinero virtual (>= 0) ---
    const val DINERO_ACIERTO = 50
    const val DINERO_BRECHA = -100
    const val DINERO_FALSO_POSITIVO = -25
    const val DINERO_MIN = 0

    // --- Tienda (E3): en qué GASTAR el dinero ---
    // Calibrado contra la economía real de arriba: un acierto da +50$, una brecha cuesta -100$.
    // La regeneración gratuita de E2 sigue siendo el piso garantizado; curar es un ATAJO de pago,
    // nunca la única salida, así que estos precios pueden ser exigentes sin bloquear a nadie.

    /**
     * Precio por cada +1 de salud al curar. Se cobra por punto (y no por paquete cerrado) para que
     * no haya arbitraje entre paquetes: ninguno sale más barato por punto que otro.
     *
     * Referencia: reparar una brecha (-20 de salud) cuesta 120$ ≈ 2,4 aciertos, frente a 20 minutos
     * de espera gratis.
     */
    const val PRECIO_CURA_POR_PUNTO = 6

    /** Paquete chico de cura: +25 de salud (150$ a pleno). */
    const val CURA_PAQUETE_CHICO = 25

    /** Paquete grande de cura: +50 de salud (300$ a pleno). */
    const val CURA_PAQUETE_GRANDE = 50

    /**
     * Precio del escudo de un uso. Es una APUESTA, no un ahorro seguro: si absorbe una brecha
     * (-20) evita 120$ de cura, pero si absorbe un falso positivo (-10) solo evita 60$.
     */
    const val PRECIO_ESCUDO = 100

    // --- Nivel ---
    /** Puntos necesarios por nivel: nivel = 1 + puntaje / PUNTOS_POR_NIVEL. */
    const val PUNTOS_POR_NIVEL = 100
    const val NIVEL_MIN = 1
}
