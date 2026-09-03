package com.ejemplo.nettycoon.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel de autenticación.
 *
 * Es el único intermediario entre la UI y [AuthRepository]: la UI solo llama a estos
 * métodos y observa [estado]. No expone tipos del SDK de Firebase.
 */
class AuthViewModel(
    private val repositorio: AuthRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(AuthUiState())
    val estado: StateFlow<AuthUiState> = _estado.asStateFlow()

    /** Snapshot de sesión, usado por el gate de navegación para el destino inicial. */
    fun haySesion(): Boolean = repositorio.usuarioActual != null

    // --- Actualización de campos (limpia el error al escribir) ---

    fun onEmailChange(valor: String) = _estado.update {
        it.copy(email = valor, emailInvalido = false).sinError()
    }

    fun onPasswordChange(valor: String) = _estado.update {
        it.copy(password = valor, passwordInvalido = false).sinError()
    }

    fun onConfirmarPasswordChange(valor: String) = _estado.update {
        it.copy(confirmarPassword = valor, confirmarPasswordInvalido = false).sinError()
    }

    // --- Acciones de autenticación ---

    fun iniciarSesion() {
        val estadoValidado = validarCredenciales(_estado.value)
        _estado.value = estadoValidado
        if (estadoValidado.emailInvalido || estadoValidado.passwordInvalido) return

        ejecutarOperacion {
            repositorio.iniciarSesion(estadoValidado.email.trim(), estadoValidado.password)
        }
    }

    fun registrar() {
        var estadoValidado = validarCredenciales(_estado.value)
        // Validación adicional: la confirmación debe coincidir.
        if (estadoValidado.confirmarPassword != estadoValidado.password) {
            estadoValidado = estadoValidado.copy(
                confirmarPasswordInvalido = true,
                operacion = EstadoOperacion.Error("Las contraseñas no coinciden."),
            )
        }
        _estado.value = estadoValidado
        if (estadoValidado.emailInvalido ||
            estadoValidado.passwordInvalido ||
            estadoValidado.confirmarPasswordInvalido
        ) return

        ejecutarOperacion {
            repositorio.registrar(estadoValidado.email.trim(), estadoValidado.password)
        }
    }

    /** Cierra la sesión actual delegando en el repositorio. */
    fun cerrarSesion() = repositorio.cerrarSesion()

    /** Limpia el estado de error, volviendo a Inactivo. */
    fun limpiarError() = _estado.update {
        if (it.operacion is EstadoOperacion.Error) it.copy(operacion = EstadoOperacion.Inactivo) else it
    }

    /** Consume el éxito y resetea el formulario (tras navegar fuera de la pantalla de auth). */
    fun consumirExito() {
        _estado.value = AuthUiState()
    }

    // --- Internos ---

    private inline fun ejecutarOperacion(crossinline bloque: suspend () -> AuthResultado) {
        _estado.update { it.copy(operacion = EstadoOperacion.Cargando) }
        viewModelScope.launch {
            val nuevaOperacion = when (val resultado = bloque()) {
                is AuthResultado.Exito -> EstadoOperacion.Exito
                is AuthResultado.Error -> EstadoOperacion.Error(resultado.mensaje)
            }
            _estado.update { it.copy(operacion = nuevaOperacion) }
        }
    }

    private fun validarCredenciales(estado: AuthUiState): AuthUiState {
        val emailValido = estado.email.isNotBlank() &&
            Patterns.EMAIL_ADDRESS.matcher(estado.email.trim()).matches()
        val passwordValido = estado.password.length >= MIN_PASSWORD

        val mensaje = when {
            !emailValido -> "Ingresa un correo electrónico válido."
            !passwordValido -> "La contraseña debe tener al menos $MIN_PASSWORD caracteres."
            else -> null
        }

        return estado.copy(
            emailInvalido = !emailValido,
            passwordInvalido = !passwordValido,
            operacion = if (mensaje != null) EstadoOperacion.Error(mensaje) else estado.operacion,
        )
    }

    private fun AuthUiState.sinError(): AuthUiState =
        if (operacion is EstadoOperacion.Error) copy(operacion = EstadoOperacion.Inactivo) else this

    private companion object {
        const val MIN_PASSWORD = 6
    }
}
