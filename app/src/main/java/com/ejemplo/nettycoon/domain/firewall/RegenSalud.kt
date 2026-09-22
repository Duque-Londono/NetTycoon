package com.ejemplo.nettycoon.domain.firewall

/**
 * Regeneración lenta y GRATUITA de la salud de la red con el paso del TIEMPO REAL (E2).
 *
 * Función pura, sin dependencias de Android/Room, para ser testeable en JVM (patrón [EstadoSalud]).
 * El cálculo se basa en un timestamp ANCLA guardado (el instante desde el que se cuenta el tiempo),
 * no en que la app esté abierta ni en eventos de UI: por eso no se puede farmear cerrando/abriendo
 * la app ni depende de mantenerla en primer plano.
 *
 * Regla de diseño: **+[PASO_SALUD] de salud cada [PASO_MINUTOS] minutos**, hasta [SALUD_MAX].
 *
 * Anti-explotación:
 * - La salud sale del tiempo transcurrido entre [ancla] y `ahora`, no de abrir la pantalla.
 * - El ancla AVANZA por los pasos consumidos (`ancla + pasos*paso`), **conservando el resto** de
 *   minutos; nunca se resetea a `ahora` (salvo al estar/quedar llena), así no se pierde progreso
 *   pero tampoco se regala tiempo repitiendo el cálculo dentro de la misma ventana.
 * - Si `ahora` es anterior al ancla (reloj movido hacia atrás), `transcurrido` queda por debajo de
 *   un paso y no se regenera nada.
 */
object RegenSalud {

    /** Salud que se recupera por cada paso de tiempo. */
    const val PASO_SALUD = 5

    /** Minutos de tiempo real por cada paso de regeneración. */
    const val PASO_MINUTOS = 5L

    /** Tope de salud (mismo que [BalancePartida.SALUD_MAX]). */
    const val SALUD_MAX = 100

    /** Duración de un paso en milisegundos. */
    const val PASO_MS = PASO_MINUTOS * 60_000L

    /** Resultado del recálculo de regen: la salud al día y el ancla que hay que persistir. */
    data class ResultadoRegen(val saludNueva: Int, val anclaNueva: Long)

    /**
     * Recalcula la salud regenerada y el ancla a guardar, a partir de la salud actual, el ancla
     * previa y el instante actual.
     *
     * @param saludActual salud persistida (0..100).
     * @param ancla epoch millis desde el que se cuenta el tiempo transcurrido.
     * @param ahora epoch millis actual.
     */
    fun calcular(saludActual: Int, ancla: Long, ahora: Long): ResultadoRegen {
        // Ya llena: no se banquea tiempo (el ancla se pone al día para contar desde ahora si baja).
        if (saludActual >= SALUD_MAX) return ResultadoRegen(SALUD_MAX, ahora)

        val transcurrido = ahora - ancla
        // Menos de un paso (incluye reloj hacia atrás): nada que regenerar, se conserva el ancla.
        if (transcurrido < PASO_MS) return ResultadoRegen(saludActual, ancla)

        val pasos = transcurrido / PASO_MS
        val saludNueva = (saludActual + pasos * PASO_SALUD).coerceAtMost(SALUD_MAX.toLong()).toInt()
        // Al tope: no se banquea el resto. Si no, se avanza el ancla solo por los pasos consumidos.
        val anclaNueva = if (saludNueva >= SALUD_MAX) ahora else ancla + pasos * PASO_MS
        return ResultadoRegen(saludNueva, anclaNueva)
    }
}
