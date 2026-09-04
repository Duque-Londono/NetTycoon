package com.ejemplo.nettycoon.domain.model

import com.ejemplo.nettycoon.data.local.entity.AccionFirewall

/**
 * Regla de firewall en su forma mínima para ser evaluada por el motor.
 *
 * Es un modelo de dominio propio, desacoplado de la entidad Room `ReglaFirewall`: el
 * motor no necesita `id`, `owner`, `creadaEn` ni `activa` (el filtrado por reglas
 * activas lo hace quien invoca al motor). La traducción entidad → dominio corresponde a
 * las capas superiores (ViewModel/repositorio) en fases posteriores.
 *
 * Reutiliza el enum [AccionFirewall] existente para que el mapeo de vuelta a Room sea
 * directo.
 */
data class ReglaEvaluable(
    /** Puerto que la regla vigila (coincidencia exacta). */
    val puerto: Int,

    /** IP objetivo. `null` actúa como comodín: coincide con cualquier IP. */
    val ip: String? = null,

    /** Acción a aplicar si la regla coincide. */
    val accion: AccionFirewall,
)
