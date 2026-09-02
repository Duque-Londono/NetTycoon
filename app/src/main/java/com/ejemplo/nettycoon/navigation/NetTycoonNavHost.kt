package com.ejemplo.nettycoon.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ejemplo.nettycoon.ui.login.LoginScreen
import com.ejemplo.nettycoon.ui.network.HomeScreen

/**
 * NavHost raíz de NetTycoon.
 *
 * PASO 1: solo dos destinos placeholder ("login" y "home") para validar que la
 * navegación funciona. El gate condicional de autenticación (sesión → home;
 * sin sesión → login) se implementará en un paso posterior; por ahora se arranca
 * en [Rutas.Login].
 */
@Composable
fun NetTycoonNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Rutas.Login.ruta,
        modifier = modifier
    ) {
        composable(Rutas.Login.ruta) {
            LoginScreen(
                onIrAHome = {
                    navController.navigate(Rutas.Home.ruta) {
                        popUpTo(Rutas.Login.ruta) { inclusive = true }
                    }
                }
            )
        }
        composable(Rutas.Home.ruta) {
            HomeScreen()
        }
    }
}
