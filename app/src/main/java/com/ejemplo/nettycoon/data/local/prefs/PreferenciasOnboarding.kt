package com.ejemplo.nettycoon.data.local.prefs

import android.content.Context

/**
 * Persistencia del flag "el onboarding ya se vio", con **SharedPreferences** (no Room).
 *
 * Decisión de diseño (documentada a propósito):
 * - El flag es **por dispositivo/instalación**, NO por `uid` de Firebase. El onboarding solo
 *   enmarca el propósito educativo del juego la primera vez que alguien abre la app en ese
 *   dispositivo; no es un dato de partida ni de usuario, por eso vive fuera de Room y no se
 *   asocia a la sesión. Se reinicia únicamente si se desinstala la app o se borran sus datos.
 * - Se usa SharedPreferences (y no DataStore) para no añadir dependencias nuevas: es suficiente
 *   para un único booleano leído en el arranque del grafo de navegación.
 *
 * Esta clase es un wrapper delgado; toda la lógica de *decisión* de a dónde navegar vive en la
 * función pura [com.ejemplo.nettycoon.navigation.decidirDestinoInicial], que sí es testeable en JVM.
 */
class PreferenciasOnboarding(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(NOMBRE_ARCHIVO, Context.MODE_PRIVATE)

    /** `true` si el onboarding ya se completó o se saltó alguna vez en este dispositivo. */
    fun onboardingVisto(): Boolean = prefs.getBoolean(CLAVE_VISTO, false)

    /** Marca el onboarding como visto para que no vuelva a mostrarse en este dispositivo. */
    fun marcarOnboardingVisto() {
        prefs.edit().putBoolean(CLAVE_VISTO, true).apply()
    }

    private companion object {
        const val NOMBRE_ARCHIVO = "nettycoon_onboarding"
        const val CLAVE_VISTO = "onboarding_visto"
    }
}
