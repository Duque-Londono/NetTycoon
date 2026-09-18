package com.ejemplo.nettycoon.ui.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ejemplo.nettycoon.data.local.entity.EstadoPartida
import com.ejemplo.nettycoon.ui.theme.NetTycoonTheme

/**
 * Panel del juego: hub central. Muestra el estado de la partida y da acceso a las tres
 * pantallas: "Ataque en vivo" (donde el jugador decide y aprende), "Mis reglas" (CRUD para
 * automatizar decisiones) y "Configurar red".
 *
 * La simulación de ronda ya no ocurre aquí: se traslada a la pantalla "Ataque en vivo". Los
 * miembros `simularAtaque`/`ultimaRonda` del [PanelViewModel] quedan sin uso a propósito en esta
 * fase (deuda anotada a limpiar más adelante); no se reabren en esta tarea.
 *
 * Punto de entrada con estado (conectado al ViewModel).
 */
@Composable
fun PanelScreen(
    viewModel: PanelViewModel,
    onIrAAtaqueEnVivo: () -> Unit,
    onIrAReglas: () -> Unit,
    onIrAConfigRed: () -> Unit,
    onVerOnboarding: () -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    PanelScreen(
        estado = estado,
        onLimpiarError = viewModel::limpiarError,
        onIrAAtaqueEnVivo = onIrAAtaqueEnVivo,
        onIrAReglas = onIrAReglas,
        onIrAConfigRed = onIrAConfigRed,
        onVerOnboarding = onVerOnboarding,
        onCerrarSesion = onCerrarSesion,
        modifier = modifier,
    )
}

/** Versión sin estado (stateless) para previews y separación de responsabilidades. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelScreen(
    estado: PanelUiState,
    onLimpiarError: () -> Unit,
    onIrAAtaqueEnVivo: () -> Unit,
    onIrAReglas: () -> Unit,
    onIrAConfigRed: () -> Unit,
    onVerOnboarding: () -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("NetTycoon") },
                actions = {
                    TextButton(onClick = onCerrarSesion) { Text("Cerrar sesión") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TarjetaPartida(partida = estado.partida)

            Button(
                onClick = onIrAAtaqueEnVivo,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Ataque en vivo")
            }

            OutlinedButton(
                onClick = onIrAReglas,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Mis reglas")
            }

            OutlinedButton(
                onClick = onIrAConfigRed,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Configurar red")
            }

            OutlinedButton(
                onClick = onVerOnboarding,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cómo se juega")
            }

            if (estado.cargando) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            estado.error?.let { mensaje ->
                TarjetaError(mensaje = mensaje, onLimpiarError = onLimpiarError)
            }
        }
    }
}

@Composable
private fun TarjetaPartida(partida: EstadoPartida?, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "Tu red",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (partida == null) {
                Text("Cargando partida…", style = MaterialTheme.typography.bodyMedium)
            } else {
                FilaDato("Puntaje", partida.puntaje.toString())
                FilaDato("Salud de la red", "${partida.saludRed} / 100")
                FilaDato("Dinero virtual", "$${partida.dineroVirtual}")
                FilaDato("Nivel", partida.nivel.toString())
            }
        }
    }
}

@Composable
private fun TarjetaError(
    mensaje: String,
    onLimpiarError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(mensaje, style = MaterialTheme.typography.bodyMedium)
            TextButton(
                onClick = onLimpiarError,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Entendido")
            }
        }
    }
}

@Composable
private fun FilaDato(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium)
        Text(
            valor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

// --- Previews ---

@Preview(showBackground = true)
@Composable
private fun PanelScreenPreview() {
    NetTycoonTheme {
        PanelScreen(
            estado = PanelUiState(
                partida = EstadoPartida(owner = "demo", puntaje = 30, saludRed = 90, nivel = 2),
            ),
            onLimpiarError = {},
            onIrAAtaqueEnVivo = {},
            onIrAReglas = {},
            onIrAConfigRed = {},
            onVerOnboarding = {},
            onCerrarSesion = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PanelScreenCargandoPreview() {
    NetTycoonTheme {
        PanelScreen(
            estado = PanelUiState(
                partida = EstadoPartida(owner = "demo"),
                cargando = true,
            ),
            onLimpiarError = {},
            onIrAAtaqueEnVivo = {},
            onIrAReglas = {},
            onIrAConfigRed = {},
            onVerOnboarding = {},
            onCerrarSesion = {},
        )
    }
}
