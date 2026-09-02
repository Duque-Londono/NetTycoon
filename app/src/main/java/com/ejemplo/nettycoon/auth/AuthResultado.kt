package com.ejemplo.nettycoon.auth

/**
 * Resultado de una operación de autenticación.
 *
 * Distingue explícitamente Éxito (con el usuario resultante) y Error (con un mensaje
 * claro para mostrar y la causa original para diagnóstico). Evita propagar excepciones
 * crudas de Firebase hacia capas superiores.
 */
sealed interface AuthResultado {
    data class Exito(val usuario: UsuarioAuth) : AuthResultado
    data class Error(val mensaje: String, val causa: Throwable? = null) : AuthResultado
}
