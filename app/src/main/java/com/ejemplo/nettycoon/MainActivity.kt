package com.ejemplo.nettycoon

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ejemplo.nettycoon.navigation.NetTycoonNavHost
import com.ejemplo.nettycoon.ui.theme.NetTycoonTheme

/**
 * Actividad única de NetTycoon: hospeda el NavHost raíz dentro del tema de la app.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Barras de sistema (status + navigation) en estilo oscuro para que combinen con
        // el fondo #0B0E14 del tema y no queden claras sobre contenido oscuro.
        val scrimOscuro = AndroidColor.parseColor("#0B0E14")
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(scrimOscuro),
            navigationBarStyle = SystemBarStyle.dark(scrimOscuro)
        )
        setContent {
            NetTycoonApp()
        }
    }
}

@Composable
private fun NetTycoonApp() {
    NetTycoonTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            NetTycoonNavHost(modifier = Modifier.padding(innerPadding))
        }
    }
}
