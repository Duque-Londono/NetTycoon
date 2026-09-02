package com.ejemplo.nettycoon

import android.os.Bundle
import androidx.activity.ComponentActivity
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
        enableEdgeToEdge()
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
