package com.ejemplo.nettycoon.ui.network

import com.ejemplo.nettycoon.data.local.entity.EstadoPartida

/**
 * Estado de UI de la pantalla de configuración de red.
 *
 * Modela dos cosas a la vez, igual que `FirewallUiState`:
 * - **El formulario** ([ipRouterTexto], [puertoLanTexto], [puertoWanTexto]), como texto crudo
 *   porque es lo que el usuario escribe; la conversión y validación ocurren en el ViewModel al
 *   pulsar "Guardar".
 * - **La partida cargada** ([partida]): se conserva entera para poder guardar cambiando solo los
 *   tres campos de red y **preservar el resto de métricas** (puntaje, dinero, salud, nivel).
 *
 * Se distinguen dos errores porque se muestran en sitios distintos:
 * - [errorFormulario]: validación del formulario (IP mal escrita, puerto fuera de rango); se
 *   pinta bajo los campos y no bloquea el resto de la pantalla.
 * - [error]: fallo de la capa de datos (leer/guardar en Room); se pinta como tarjeta.
 *
 * [guardadoConExito] es la señal de feedback: cuando es `true` la pantalla muestra
 * "Configuración guardada ✓". Se limpia en cuanto el usuario edita cualquier campo, para que el
 * ✓ nunca refleje un estado obsoleto.
 */
data class ConfigRedUiState(
    val ipRouterTexto: String = "",
    val puertoLanTexto: String = "",
    val puertoWanTexto: String = "",
    val partida: EstadoPartida? = null,
    val cargando: Boolean = true,
    val errorFormulario: String? = null,
    val error: String? = null,
    val guardadoConExito: Boolean = false,
)
