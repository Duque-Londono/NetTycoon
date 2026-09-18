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

    companion object {
        /** Nombre del argumento de ruta que transporta el uid del jugador. */
        const val ARG_UID = "uid"
    }
}
