package com.ejemplo.nettycoon.domain.model

import com.ejemplo.nettycoon.data.local.entity.AccionFirewall
import com.ejemplo.nettycoon.data.local.entity.ResultadoEvento

/**
 * Salida del motor de firewall al evaluar un [Ataque] contra un conjunto de reglas.
 *
 * Reutiliza el enum [ResultadoEvento] de Room (BLOQUEADO/PERMITIDO) para que persistir
 * el evento en fases posteriores sea directo.
 */
data class ResultadoEvaluacion(
    /** Acción finalmente aplicada al ataque (DENY o ALLOW). */
    val accionAplicada: AccionFirewall,

    /**
     * Regla que determinó la acción, o `null` si no coincidió ninguna y se aplicó la
     * política por defecto.
     */
    val reglaCoincidente: ReglaEvaluable?,

    /** Cómo se resolvió el tráfico: BLOQUEADO (DENY) o PERMITIDO (ALLOW). */
    val resultado: ResultadoEvento,

    /** `true` si la decisión resultó correcta frente a la verdad del ataque. */
    val acierto: Boolean,

    /** Categoría de firewall del resultado (una de las 4). */
    val categoria: CategoriaResultado,
)
