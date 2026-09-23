package com.ejemplo.nettycoon.domain.firewall

/**
 * Cupo de reglas ACTIVAS por rango. PROVISIONALES: los valida el equipo tras jugarlo.
 *
 * Calibrados contra los **25 puertos distintos** que aparecen en el catálogo de escenarios: ni
 * siquiera el cupo máximo cubre la mitad, así que **siempre queda juego manual** donde la salud y
 * la tienda pesan. Ese es justo el objetivo de E5: que automatizar no permita esquivar la
 * economía.
 *
 * Efecto buscado: una familia grande (Correo o Archivos, 7 puertos cada una) **no cabe** en los
 * rangos bajos, así que automatizar una familia entera desde el puente deja de ser gratis y la
 * disyuntiva "precisión vs. comodidad" se vuelve una decisión real.
 *
 * Viven en su propio objeto (y no en [BalancePartida]) por la misma razón que [UmbralesRango]: es
 * progresión, no balance de combate ni de economía.
 */
object CuposReglas {
    const val CUPO_APRENDIZ = 3
    const val CUPO_TECNICO = 5
    const val CUPO_ANALISTA = 8
    const val CUPO_EXPERTO = 12
}

/**
 * Cuántas reglas puede tener ACTIVAS a la vez un jugador de este [rango].
 *
 * Función pura, sin dependencias de Android/Room, testeable en JVM (patrón [rangoPorPuntaje] /
 * [RegenSalud]).
 *
 * **Cuenta activas, no creadas.** Es una distinción deliberada: el cupo limita cuánta
 * automatización está VIGENTE, no cuántas reglas ha llegado a escribir el jugador. Gracias a eso,
 * quedarse por encima del cupo (al bajar de rango) siempre se puede resolver **desactivando** una
 * regla, sin borrar nada y sin perder trabajo.
 *
 * Como el rango se deriva del puntaje, el cupo también: **no se persiste ningún campo nuevo** ni se
 * toca el esquema de Room.
 */
fun cupoDeReglas(rango: Rango): Int = when (rango) {
    Rango.APRENDIZ -> CuposReglas.CUPO_APRENDIZ
    Rango.TECNICO -> CuposReglas.CUPO_TECNICO
    Rango.ANALISTA -> CuposReglas.CUPO_ANALISTA
    Rango.EXPERTO -> CuposReglas.CUPO_EXPERTO
}

/** Atajo: cupo que corresponde a un [puntaje], pasando por [rangoPorPuntaje]. */
fun cupoPorPuntaje(puntaje: Int): Int = cupoDeReglas(rangoPorPuntaje(puntaje))
