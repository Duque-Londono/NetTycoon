package com.ejemplo.nettycoon.domain.firewall

import com.ejemplo.nettycoon.data.local.entity.AccionFirewall
import com.ejemplo.nettycoon.data.local.entity.ResultadoEvento
import com.ejemplo.nettycoon.domain.model.Ataque
import com.ejemplo.nettycoon.domain.model.CategoriaResultado
import com.ejemplo.nettycoon.domain.model.ReglaEvaluable
import com.ejemplo.nettycoon.domain.model.ResultadoEvaluacion

/**
 * Motor de evaluación de reglas de firewall. Lógica pura, sin estado ni dependencias de
 * Android/Room: trabaja solo con los datos en memoria que se le pasan.
 *
 * **Semántica de coincidencia:** una [ReglaEvaluable] coincide con un [Ataque] cuando el
 * puerto casa exactamente y la IP casa (igualdad exacta, o `ip == null` como comodín).
 *
 * **Precedencia:** si varias reglas coinciden, **gana DENY** (una prohibición explícita no
 * la anula un permiso). Es determinista, independiente del orden de las reglas.
 *
 * **Política por defecto:** si ninguna regla coincide, se aplica [politicaPorDefecto]
 * (por defecto DENY, el estándar seguro de firewall).
 *
 * Se asume que la lista recibida ya contiene **solo reglas activas**; el filtrado por
 * `activa` es responsabilidad de quien invoca al motor.
 */
object MotorFirewall {

    fun evaluar(
        ataque: Ataque,
        reglas: List<ReglaEvaluable>,
        politicaPorDefecto: AccionFirewall = AccionFirewall.DENY,
    ): ResultadoEvaluacion {
        val coincidentes = reglas.filter { coincide(it, ataque) }

        // Precedencia: gana DENY. Si no hay coincidentes, aplica la política por defecto.
        val reglaCoincidente: ReglaEvaluable? = when {
            coincidentes.isEmpty() -> null
            else -> coincidentes.firstOrNull { it.accion == AccionFirewall.DENY }
                ?: coincidentes.first()
        }

        val accionAplicada = reglaCoincidente?.accion ?: politicaPorDefecto

        return construirResultado(ataque, accionAplicada, reglaCoincidente)
    }

    /** Una regla coincide si el puerto es exacto y la IP casa (exacta o comodín `null`). */
    private fun coincide(regla: ReglaEvaluable, ataque: Ataque): Boolean =
        regla.puerto == ataque.puertoDestino &&
            (regla.ip == null || regla.ip == ataque.ipAtacante)

    private fun construirResultado(
        ataque: Ataque,
        accion: AccionFirewall,
        reglaCoincidente: ReglaEvaluable?,
    ): ResultadoEvaluacion {
        val resultado = when (accion) {
            AccionFirewall.DENY -> ResultadoEvento.BLOQUEADO
            AccionFirewall.ALLOW -> ResultadoEvento.PERMITIDO
        }

        val acierto = (ataque.esMalicioso && accion == AccionFirewall.DENY) ||
            (!ataque.esMalicioso && accion == AccionFirewall.ALLOW)

        val categoria = when {
            ataque.esMalicioso && accion == AccionFirewall.DENY -> CategoriaResultado.BLOQUEO_CORRECTO
            !ataque.esMalicioso && accion == AccionFirewall.ALLOW -> CategoriaResultado.PERMISO_CORRECTO
            ataque.esMalicioso && accion == AccionFirewall.ALLOW -> CategoriaResultado.BRECHA
            else -> CategoriaResultado.FALSO_POSITIVO
        }

        return ResultadoEvaluacion(
            accionAplicada = accion,
            reglaCoincidente = reglaCoincidente,
            resultado = resultado,
            acierto = acierto,
            categoria = categoria,
        )
    }
}
