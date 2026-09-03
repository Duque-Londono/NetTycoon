package com.ejemplo.nettycoon.ui.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ejemplo.nettycoon.ui.theme.NetTycoonTheme

/**
 * Pantalla stub de Home. Placeholder del futuro tablero de juego / configuración de red.
 *
 * Por ahora incluye solo una acción mínima de cerrar sesión, que dispara el retorno a
 * Login a través del gate de navegación.
 */
@Composable
fun HomeScreen(
    onCerrarSesion: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "NetTycoon — Home",
            style = MaterialTheme.typography.headlineSmall,
        )
        OutlinedButton(
            onClick = onCerrarSesion,
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text("Cerrar sesión")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    NetTycoonTheme {
        HomeScreen()
    }
}
