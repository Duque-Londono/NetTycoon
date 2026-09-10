package com.ejemplo.nettycoon.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ejemplo.nettycoon.auth.AuthRepository
import com.ejemplo.nettycoon.auth.AuthRepositoryFirebase
import com.ejemplo.nettycoon.auth.AuthViewModel
import com.ejemplo.nettycoon.auth.AuthViewModelFactory
import com.ejemplo.nettycoon.ui.firewall.FirewallScreen
import com.ejemplo.nettycoon.ui.firewall.FirewallViewModel
import com.ejemplo.nettycoon.ui.firewall.FirewallViewModelFactory
import com.ejemplo.nettycoon.ui.login.LoginScreen
import com.ejemplo.nettycoon.ui.login.RegistroScreen
import com.ejemplo.nettycoon.ui.panel.PanelScreen
import com.ejemplo.nettycoon.ui.panel.PanelViewModel
import com.ejemplo.nettycoon.ui.panel.PanelViewModelFactory

/**
 * NavHost raíz de NetTycoon con el gate condicional de autenticación.
 *
 * - El destino inicial depende de la sesión: si hay usuario → Home; si no → Login.
 * - Login/Registro con éxito → Home limpiando el back stack (popUpTo Login inclusive).
 * - Cerrar sesión en Home → Login limpiando el back stack (popUpTo Home inclusive).
 * - Home → Firewall (reglas del jugador) pasando el `uid` como argumento de ruta.
 *
 * El repositorio y el [AuthViewModel] se crean aquí (composición raíz) y se comparten
 * entre las pantallas mediante una fábrica simple, sin framework de DI.
 */
@Composable
fun NetTycoonNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    repositorio: AuthRepository = remember { AuthRepositoryFirebase() },
) {
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(repositorio))

    // Gate: destino inicial según la sesión actual (evaluado una sola vez al componer).
    val startDestination = remember {
        if (authViewModel.haySesion()) Rutas.Home.ruta else Rutas.Login.ruta
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(Rutas.Login.ruta) {
            LoginScreen(
                viewModel = authViewModel,
                onNavegarARegistro = { navController.navigate(Rutas.Registro.ruta) },
                onAuthExitoso = {
                    authViewModel.consumirExito()
                    navController.navigate(Rutas.Home.ruta) {
                        popUpTo(Rutas.Login.ruta) { inclusive = true }
                    }
                },
                onBypassDev = {
                    navController.navigate(Rutas.Home.ruta) {
                        popUpTo(Rutas.Login.ruta) { inclusive = true }
                    }
                },
            )
        }

        composable(Rutas.Registro.ruta) {
            RegistroScreen(
                viewModel = authViewModel,
                onVolverALogin = { navController.popBackStack() },
                onAuthExitoso = {
                    authViewModel.consumirExito()
                    navController.navigate(Rutas.Home.ruta) {
                        popUpTo(Rutas.Login.ruta) { inclusive = true }
                    }
                },
            )
        }

        composable(Rutas.Home.ruta) {
            val context = LocalContext.current.applicationContext
            // El uid lo aporta la sesión ya existente (el ViewModel no consulta Firebase).
            // Fallback "dev-local" solo aplica al bypass de desarrollo, que está candado a
            // BuildConfig.DEBUG en LoginScreen: en release nunca se entra sin sesión.
            val uid = repositorio.usuarioActual?.uid ?: "dev-local"
            val panelViewModel: PanelViewModel = viewModel(
                factory = PanelViewModelFactory(context, uid),
            )
            PanelScreen(
                viewModel = panelViewModel,
                onIrAReglas = { navController.navigate(Rutas.Firewall.crearRuta(uid)) },
                onCerrarSesion = {
                    authViewModel.cerrarSesion()
                    authViewModel.consumirExito()
                    navController.navigate(Rutas.Login.ruta) {
                        popUpTo(Rutas.Home.ruta) { inclusive = true }
                    }
                },
            )
        }

        // Destino con argumento de ruta: el uid viaja en la propia ruta, así la pantalla es
        // autocontenida y su ViewModel no necesita consultar Firebase.
        composable(
            route = Rutas.Firewall.ruta,
            arguments = listOf(navArgument(Rutas.ARG_UID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val context = LocalContext.current.applicationContext
            val uid = backStackEntry.arguments?.getString(Rutas.ARG_UID).orEmpty()
            val firewallViewModel: FirewallViewModel = viewModel(
                factory = FirewallViewModelFactory(context, uid),
            )
            FirewallScreen(
                viewModel = firewallViewModel,
                onVolver = { navController.popBackStack() },
            )
        }
    }
}
