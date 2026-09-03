package com.ejemplo.nettycoon.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Fábrica mínima para inyectar el [AuthRepository] en el [AuthViewModel].
 *
 * Evita introducir un framework de inyección de dependencias: es suficiente para
 * pasar la dependencia al ViewModel de forma explícita.
 */
class AuthViewModelFactory(
    private val repositorio: AuthRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            "ViewModel desconocido: ${modelClass.name}"
        }
        return AuthViewModel(repositorio) as T
    }
}
