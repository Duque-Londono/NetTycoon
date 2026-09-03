package com.ejemplo.nettycoon.auth

/**
 * Modelo de dominio del usuario autenticado.
 *
 * Se usa a propósito en lugar de exponer `FirebaseUser` fuera de la capa de datos,
 * para no acoplar el resto de la app al SDK de Firebase.
 */
data class UsuarioAuth(
    val uid: String,
    val email: String?,
)
