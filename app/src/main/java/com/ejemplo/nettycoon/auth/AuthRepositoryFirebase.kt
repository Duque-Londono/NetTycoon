package com.ejemplo.nettycoon.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Implementación de [AuthRepository] sobre [FirebaseAuth].
 *
 * - Las llamadas de Firebase (basadas en `Task`) se envuelven con `await()`; no se usan
 *   callbacks crudos.
 * - Las excepciones del SDK se mapean a [AuthResultado.Error] con mensajes claros en español.
 */
class AuthRepositoryFirebase(
    private val auth: FirebaseAuth = Firebase.auth,
) : AuthRepository {

    override suspend fun registrar(email: String, password: String): AuthResultado =
        ejecutar { auth.createUserWithEmailAndPassword(email, password).await().user }

    override suspend fun iniciarSesion(email: String, password: String): AuthResultado =
        ejecutar { auth.signInWithEmailAndPassword(email, password).await().user }

    override fun cerrarSesion() {
        auth.signOut()
    }

    override val usuarioActual: UsuarioAuth?
        get() = auth.currentUser?.aUsuarioAuth()

    override val estadoSesion: Flow<UsuarioAuth?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.aUsuarioAuth())
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /** Ejecuta una operación de auth y traduce el resultado/excepción a [AuthResultado]. */
    private suspend inline fun ejecutar(bloque: () -> FirebaseUser?): AuthResultado =
        try {
            val usuario = bloque()
                ?: return AuthResultado.Error("No se pudo obtener el usuario autenticado.")
            AuthResultado.Exito(usuario.aUsuarioAuth())
        } catch (e: Exception) {
            AuthResultado.Error(mensajeDeError(e), e)
        }

    private fun FirebaseUser.aUsuarioAuth() = UsuarioAuth(uid = uid, email = email)

    private fun mensajeDeError(e: Exception): String = when (e) {
        is FirebaseAuthWeakPasswordException ->
            "La contraseña es demasiado débil (mínimo 6 caracteres)."
        is FirebaseAuthInvalidCredentialsException ->
            "El correo o la contraseña no son válidos."
        is FirebaseAuthUserCollisionException ->
            "Ya existe una cuenta registrada con ese correo."
        is FirebaseAuthInvalidUserException ->
            "La cuenta no existe o fue deshabilitada."
        is FirebaseNetworkException ->
            "Error de red. Revisa tu conexión e inténtalo de nuevo."
        is FirebaseAuthException ->
            "Error de autenticación: ${e.message ?: "desconocido"}."
        else ->
            "Ocurrió un error inesperado: ${e.message ?: "desconocido"}."
    }
}
