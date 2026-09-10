package com.ejemplo.nettycoon.ui.firewall

import com.ejemplo.nettycoon.data.local.entity.AccionFirewall
import com.ejemplo.nettycoon.data.local.entity.ReglaFirewall

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
    val error: String? = null,
) {
    /** `true` cuando ya cargó y el jugador todavía no tiene ninguna regla (estado vacío). */
    val sinReglas: Boolean get() = !cargando && reglas.isEmpty()
}
