package com.ejemplo.nettycoon.ui.network

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ejemplo.nettycoon.ui.theme.NetTycoonTheme

/**
 * Pantalla stub de Home (PASO 1). Placeholder del futuro tablero de juego /
 * configuración de red. Solo un texto centrado para validar la navegación.
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "NetTycoon — Home",
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    NetTycoonTheme {
        HomeScreen()
    }
}
