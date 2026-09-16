package com.ejemplo.nettycoon.ui.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ejemplo.nettycoon.ui.theme.NetTycoonTheme

/**
 * Pantalla de configuración de red: el jugador ve y edita la configuración básica de su partida
 * (IP del router y puertos LAN/WAN). Es intencionadamente mínima: sin DHCP/NAT/SSID/latencia,
 * solo los tres campos que viven en `EstadoPartida`.
 *
 * Punto de entrada con estado (conectado al ViewModel).
 */
@Composable
fun ConfigRedScreen(
    viewModel: ConfigRedViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    ConfigRedScreen(
        estado = estado,
        onIpRouterCambiado = viewModel::onIpRouterCambiado,
        onPuertoLanCambiado = viewModel::onPuertoLanCambiado,
        onPuertoWanCambiado = viewModel::onPuertoWanCambiado,
        onGuardar = viewModel::guardar,
        onLimpiarError = viewModel::limpiarError,
        onVolver = onVolver,
        modifier = modifier,
    )
}

/** Versión sin estado (stateless) para previews y separación de responsabilidades. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigRedScreen(
    estado: ConfigRedUiState,
    onIpRouterCambiado: (String) -> Unit,
    onPuertoLanCambiado: (String) -> Unit,
    onPuertoWanCambiado: (String) -> Unit,
    onGuardar: () -> Unit,
    onLimpiarError: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Configuración de red") },
                navigationIcon = {
                    TextButton(onClick = onVolver) { Text("Volver") }
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
            TarjetaIntro()

            OutlinedTextField(
                value = estado.ipRouterTexto,
                onValueChange = onIpRouterCambiado,
                label = { Text("IP del router") },
                placeholder = { Text("192.168.0.1") },
                singleLine = true,
                isError = estado.errorFormulario != null,
                supportingText = {
                    Text(
                        "Es la dirección de tu router dentro de tu red: la puerta de entrada por " +
                            "la que pasan todos los dispositivos. En casa suele ser algo como " +
                            "192.168.0.1 o 192.168.1.1.",
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            TarjetaLanVsWan()

            OutlinedTextField(
                value = estado.puertoLanTexto,
                onValueChange = onPuertoLanCambiado,
                label = { Text("Puerto LAN") },
                singleLine = true,
                isError = estado.errorFormulario != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = {
                    Text(
                        "LAN es tu red INTERNA (tus propios equipos: computadores, impresoras…). " +
                            "Este es el puerto que usan para comunicarse dentro de tu red.",
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = estado.puertoWanTexto,
                onValueChange = onPuertoWanCambiado,
                label = { Text("Puerto WAN") },
                singleLine = true,
                isError = estado.errorFormulario != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = {
                    Text(
                        "WAN es la conexión HACIA INTERNET (el mundo exterior). Este es el puerto " +
                            "por el que tu red se comunica con afuera.",
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            estado.errorFormulario?.let { mensaje ->
                Text(
                    text = mensaje,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = onGuardar,
                enabled = !estado.cargando,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Guardar")
            }

            if (estado.cargando) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            if (estado.guardadoConExito) {
                TarjetaGuardado()
            }

            estado.error?.let { mensaje ->
                TarjetaError(mensaje = mensaje, onLimpiarError = onLimpiarError)
            }
        }
    }
}

/**
 * Explica qué es esta pantalla y la distingue de "Mis reglas", para que el jugador no confunda
 * estos puertos (su propia infraestructura) con los de los ataques (qué tráfico permitir/bloquear).
 * Mismo estilo de tarjeta introductoria que `FirewallScreen`, para que se sientan de la misma familia.
 */
@Composable
private fun TarjetaIntro(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "¿Qué es esta pantalla?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Aquí defines cómo está montada tu red: la dirección de tu router y los puertos " +
                    "que usa. Es la información base de tu infraestructura; no es donde decides " +
                    "qué bloquear (eso es 'Mis reglas').",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** Mnemónico del concepto que más cuesta a un novato: LAN (adentro) vs. WAN (afuera). */
@Composable
private fun TarjetaLanVsWan(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Text(
            "🏠 LAN = adentro (tu casa/oficina)  ·  🌐 WAN = afuera (internet)",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun TarjetaGuardado(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Text(
            "Configuración guardada ✓",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
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

// --- Previews ---

@Preview(showBackground = true)
@Composable
private fun ConfigRedScreenPreview() {
    NetTycoonTheme {
        ConfigRedScreen(
            estado = ConfigRedUiState(
                cargando = false,
                ipRouterTexto = "192.168.0.1",
                puertoLanTexto = "80",
                puertoWanTexto = "443",
            ),
            onIpRouterCambiado = {},
            onPuertoLanCambiado = {},
            onPuertoWanCambiado = {},
            onGuardar = {},
            onLimpiarError = {},
            onVolver = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfigRedScreenGuardadoPreview() {
    NetTycoonTheme {
        ConfigRedScreen(
            estado = ConfigRedUiState(
                cargando = false,
                ipRouterTexto = "10.0.0.1",
                puertoLanTexto = "8080",
                puertoWanTexto = "443",
                guardadoConExito = true,
            ),
            onIpRouterCambiado = {},
            onPuertoLanCambiado = {},
            onPuertoWanCambiado = {},
            onGuardar = {},
            onLimpiarError = {},
            onVolver = {},
        )
    }
}
