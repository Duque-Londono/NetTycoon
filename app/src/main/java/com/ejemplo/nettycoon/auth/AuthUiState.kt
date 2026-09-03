package com.ejemplo.nettycoon.auth

/**
 * Estado de la operación de autenticación en curso.
 *
 * Modela el ciclo Inactivo → Cargando → (Éxito | Error). El resto de la UI reacciona
 * a estas variantes sin conocer detalles del SDK.
 */
sealed interface EstadoOperacion {
    data object Inactivo : EstadoOperacion
    data object Cargando : EstadoOperacion
    data object Exito : EstadoOperacion
    data class Error(val mensaje: String) : EstadoOperacion
}

/**
 * Estado de UI de las pantallas de autenticación (login y registro).
 *
 * Contiene los campos del formulario, el estado de la operación y qué campos deben
 * marcarse como inválidos tras la validación en cliente.
 */
data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmarPassword: String = "",
    val operacion: EstadoOperacion = EstadoOperacion.Inactivo,
    val emailInvalido: Boolean = false,
    val passwordInvalido: Boolean = false,
    val confirmarPasswordInvalido: Boolean = false,
) {
    /** El formulario está bloqueado mientras hay una operación en curso. */
    val cargando: Boolean get() = operacion is EstadoOperacion.Cargando

    /** Mensaje de error a mostrar, o `null` si no hay error. */
    val mensajeError: String? get() = (operacion as? EstadoOperacion.Error)?.mensaje
}
