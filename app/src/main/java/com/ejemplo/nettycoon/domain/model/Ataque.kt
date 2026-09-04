package com.ejemplo.nettycoon.domain.model

/**
 * Ataque de entrada que el motor de firewall debe evaluar.
 *
 * Modelo de dominio puro (sin dependencias de Room/Android). La "verdad" del ataque
 * ([esMalicioso]) es lo que permite calcular si la decisión del jugador fue un acierto.
 */
data class Ataque(
    /** IP del atacante. */
    val ipAtacante: String,

    /** Puerto objetivo del ataque. */
    val puertoDestino: Int,

    /** Verdad pedagógica: `true` si el ataque debería bloquearse; `false` si es legítimo. */
    val esMalicioso: Boolean,
)
