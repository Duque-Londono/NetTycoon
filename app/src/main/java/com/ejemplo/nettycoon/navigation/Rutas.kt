package com.ejemplo.nettycoon.navigation

/**
 * Rutas de navegación de NetTycoon.
 *
 * Cada destino del grafo se declara aquí como un objeto sellado para evitar strings
 * sueltos por el código. En pasos posteriores se agregarán rutas con argumentos
 * (p. ej. detalle de un ataque) y el gate condicional de autenticación.
 */
sealed class Rutas(val ruta: String) {
    data object Login : Rutas("login")
    data object Registro : Rutas("registro")
    data object Home : Rutas("home")
}
