package com.ejemplo.nettycoon.domain.firewall

/**
 * Cálculo del **tiempo restante hasta la próxima recuperación de salud** (+[RegenSalud.PASO_SALUD])
 * y de si ya hay un paso pendiente de aplicar. Es solo para la UI (contador en vivo, E2.1).
 *
 * Función pura, sin Android, testeable en JVM. **No duplica** [RegenSalud.calcular]: deriva del
 * MISMO ancla y de las MISMAS constantes de tiempo, así que el contador y la regeneración real
 * nunca se desincronizan. El contador jamás otorga vida: la salud siempre la calcula
 * [RegenSalud] sobre el ancla real (anti-farmeo intacto); esto solo dice cuánto falta para
 * pintarlo y cuándo conviene reaplicar la regen.
 */
object TiempoRegen {

    /**
     * Milisegundos que faltan para la próxima recuperación, o `null` si la salud ya está al máximo
     * (en cuyo caso no se muestra contador).
     *
     * Se cuenta el avance dentro del tramo actual de [RegenSalud.PASO_MS] como
     * `(ahora - ancla) mod PASO_MS`; el restante es lo que queda para completar el tramo. Un
     * `ahora` anterior al ancla (reloj hacia atrás) se trata como tramo recién empezado (5:00).
     */
    fun restanteMs(saludActual: Int, ancla: Long, ahora: Long): Long? {
        if (saludActual >= RegenSalud.SALUD_MAX) return null
        val transcurrido = (ahora - ancla).coerceAtLeast(0)
        val enTramo = transcurrido % RegenSalud.PASO_MS
        return if (enTramo == 0L) RegenSalud.PASO_MS else RegenSalud.PASO_MS - enTramo
    }

    /**
     * `true` si ya pasó al menos un tramo completo desde el ancla y la salud aún no está llena, es
     * decir, hay una recuperación pendiente de aplicar. La UI lo usa para disparar la reaplicación
     * de la regen (que sí persiste), no para sumar vida por su cuenta.
     */
    fun pasoPendiente(saludActual: Int, ancla: Long, ahora: Long): Boolean =
        saludActual < RegenSalud.SALUD_MAX && (ahora - ancla) >= RegenSalud.PASO_MS
}
