package com.ejemplo.nettycoon.ui.firewall

import com.ejemplo.nettycoon.data.local.entity.AccionFirewall
import com.ejemplo.nettycoon.data.local.entity.ReglaFirewall
import com.ejemplo.nettycoon.domain.firewall.Rango

/**
 * Estado de UI de la pantalla de reglas de firewall (CRUD).
 *
 * Modela dos cosas a la vez:
 * - **La lista** de reglas del jugador ([reglas]), que llega de Room de forma reactiva.
 * - **El formulario** de creación ([puertoTexto], [ipTexto], [accion]), que se mantiene como
 *   texto crudo porque es lo que el usuario escribe; la conversión y validación ocurren en el
 *   ViewModel al pulsar "Crear regla".
 *
 * Se distinguen dos errores porque se muestran en sitios distintos:
 * - [errorFormulario]: validación del formulario (puerto fuera de rango, IP mal escrita); se
 *   pinta bajo los campos y no bloquea el resto de la pantalla.
 * - [error]: fallo de la capa de datos (leer/guardar/borrar en Room); se pinta como tarjeta.
 *
 * Reutiliza la entidad [ReglaFirewall] tal cual, igual que hace `PanelUiState`: en este
 * proyecto no se introducen modelos de presentación paralelos.
 */
data class FirewallUiState(
    val reglas: List<ReglaFirewall> = emptyList(),
    val puertoTexto: String = "",
    val ipTexto: String = "",
    val accion: AccionFirewall = AccionFirewall.DENY,
    val errorFormulario: String? = null,
    val cargando: Boolean = true,
    /**
     * Rango del jugador (E4), del que se deriva el cupo de reglas activas (E5). `null` mientras
     * carga; se trata como APRENDIZ si aún no hay partida.
     */
    val rango: Rango? = null,
    /** Cuántas reglas ACTIVAS puede tener a la vez con su rango actual. */
    val cupo: Int = 0,
    val error: String? = null,
) {
    /** `true` cuando ya cargó y el jugador todavía no tiene ninguna regla (estado vacío). */
    val sinReglas: Boolean get() = !cargando && reglas.isEmpty()

    /**
     * Reglas ACTIVAS: las únicas que el motor evalúa y, por tanto, las únicas que consumen cupo.
     *
     * Que el cupo cuente activas y no creadas es deliberado: limita cuánta automatización está
     * VIGENTE, no cuántas reglas ha escrito el jugador. Por eso pasarse de cupo siempre se puede
     * resolver desactivando, sin borrar nada.
     */
    val reglasActivas: Int get() = reglas.count { it.activa }

    /** Cupo libre; 0 si está en el tope o por encima. */
    val cupoDisponible: Int get() = (cupo - reglasActivas).coerceAtLeast(0)

    /**
     * `true` si tiene MÁS reglas activas que su cupo. Ocurre al BAJAR de rango (un falso positivo
     * resta puntaje): no es un error ni un castigo, y nunca se le borra ni desactiva nada. Solo
     * significa que no puede crear ni activar más hasta volver a estar bajo el cupo.
     */
    val sobreCupo: Boolean get() = reglasActivas > cupo

    /** `true` si puede crear o activar una regla más sin pasarse del cupo. */
    val puedeCrear: Boolean get() = !cargando && reglasActivas < cupo
}
