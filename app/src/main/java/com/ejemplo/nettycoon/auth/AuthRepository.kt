package com.ejemplo.nettycoon.auth

import kotlinx.coroutines.flow.Flow

/**
 * Contrato mínimo de autenticación (PARTE A: solo cableado).
 *
 * No expone tipos del SDK de Firebase: trabaja con [UsuarioAuth] y [AuthResultado].
 */
interface AuthRepository {

    /** Registra un usuario nuevo con email y contraseña. */
    suspend fun registrar(email: String, password: String): AuthResultado

    /** Inicia sesión con email y contraseña. */
    suspend fun iniciarSesion(email: String, password: String): AuthResultado

    /** Cierra la sesión actual. */
    fun cerrarSesion()

    /** Usuario autenticado en este instante, o `null` si no hay sesión (snapshot). */
    val usuarioActual: UsuarioAuth?

    /** Estado de sesión reactivo: emite el usuario actual (o `null`) ante cada cambio. */
    val estadoSesion: Flow<UsuarioAuth?>
}
