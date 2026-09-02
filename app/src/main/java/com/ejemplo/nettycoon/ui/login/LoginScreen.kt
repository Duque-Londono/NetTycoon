package com.ejemplo.nettycoon.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ejemplo.nettycoon.ui.theme.NetTycoonTheme

/**
 * Pantalla stub de Login (PASO 1). Sin lógica de autenticación todavía:
 * solo un texto centrado y un botón para navegar al home y probar el grafo.
 */
@Composable
fun LoginScreen(
    onIrAHome: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "NetTycoon — Login",
            style = MaterialTheme.typography.headlineSmall
        )
        Button(
            onClick = onIrAHome,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text("Entrar")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    NetTycoonTheme {
        LoginScreen()
    }
}
