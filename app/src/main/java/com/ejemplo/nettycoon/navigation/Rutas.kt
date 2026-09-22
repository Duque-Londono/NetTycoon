package com.ejemplo.nettycoon.navigation

import android.net.Uri

/**
 * Rutas de navegación de NetTycoon.
 *
 * Cada destino del grafo se declara aquí como un objeto sellado para evitar strings
 * sueltos por el código. Los destinos con argumentos declaran el patrón con placeholder
 * (`firewall/{uid}`) y un constructor de ruta que arma el valor concreto, de modo que
 * ninguna pantalla tenga que concatenar strings a mano.
 */
sealed class Rutas(val ruta: String) {
    data object Login : Rutas("login")
    data object Registro : Rutas("registro")
    data object Home : Rutas("home")

    /**
     * Onboarding de primer contacto (carrusel pedagógico). Sin argumentos: es un destino
     * transitorio al que el gate dirige solo cuando hay sesión y el flag "onboarding_visto"
     * está en `false` (ver [decidirDestinoInicial]).
     */
    data object Onboarding : Rutas("onboarding")

    /**
     * Revisita MANUAL del onboarding desde el panel ("Cómo se juega"). Reutiliza la misma
     * `OnboardingScreen`, pero al terminar/saltar vuelve al panel (`popBackStack`): NO marca el flag
     * "onboarding_visto" (ya está visto) ni altera la sesión. Es una ruta aparte de [Onboarding]
     * precisamente porque su destino de salida es distinto, y así el flujo de primer contacto queda
     * intacto.
     */
    data object OnboardingManual : Rutas("onboarding-manual")

    /**
     * Pantalla de reglas de firewall. Recibe el `uid` del jugador como argumento de ruta:
     * así el destino es autocontenido (sobrevive a recreaciones del proceso sin depender de
     * un estado compartido) y el ViewModel no necesita consultar Firebase.
     */
    data object Firewall : Rutas("firewall/{$ARG_UID}") {
        /** Construye la ruta concreta para un [uid] dado. */
        fun crearRuta(uid: String): String = "firewall/${Uri.encode(uid)}"
    }

    /**
     * Pantalla de configuración de red. Recibe el `uid` del jugador como argumento de ruta,
     * con el mismo patrón que [Firewall]: el destino es autocontenido y su ViewModel no
     * necesita consultar Firebase.
     */
    data object ConfigRed : Rutas("config-red/{$ARG_UID}") {
        /** Construye la ruta concreta para un [uid] dado. */
        fun crearRuta(uid: String): String = "config-red/${Uri.encode(uid)}"
    }

    /**
     * Pantalla "Ataque en vivo": el jugador decide permitir/bloquear un escenario pedagógico.
     * Recibe el `uid` del jugador como argumento de ruta, con el mismo patrón que [Firewall] y
     * [ConfigRed]: el destino es autocontenido y su ViewModel no necesita consultar Firebase.
     */
    data object AtaqueEnVivo : Rutas("ataque-en-vivo/{$ARG_UID}") {
        /** Construye la ruta concreta para un [uid] dado. */
        fun crearRuta(uid: String): String = "ataque-en-vivo/${Uri.encode(uid)}"
    }

    /**
     * Pantalla "Mi progreso": estadísticas derivadas del historial de ataques del jugador.
     * Recibe el `uid` como argumento de ruta, con el mismo patrón que [Firewall]/[ConfigRed]/
     * [AtaqueEnVivo]: el destino es autocontenido y su ViewModel no necesita consultar Firebase.
     */
    data object Estadisticas : Rutas("estadisticas/{$ARG_UID}") {
        /** Construye la ruta concreta para un [uid] dado. */
        fun crearRuta(uid: String): String = "estadisticas/${Uri.encode(uid)}"
    }

    /**
     * Tienda (E3): donde el jugador gasta el dinero virtual en reparar salud o en un escudo de un
     * solo uso. Recibe el `uid` como argumento de ruta, con el mismo patrón que las demás: el
     * destino es autocontenido y su ViewModel no necesita consultar Firebase.
     */
    data object Tienda : Rutas("tienda/{$ARG_UID}") {
        /** Construye la ruta concreta para un [uid] dado. */
        fun crearRuta(uid: String): String = "tienda/${Uri.encode(uid)}"
    }

    companion object {
        /** Nombre del argumento de ruta que transporta el uid del jugador. */
        const val ARG_UID = "uid"
    }
}
