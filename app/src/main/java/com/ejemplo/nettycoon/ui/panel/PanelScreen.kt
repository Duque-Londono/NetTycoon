package com.ejemplo.nettycoon.ui.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.ejemplo.nettycoon.domain.firewall.Rango
import com.ejemplo.nettycoon.domain.firewall.siguienteRango
import com.ejemplo.nettycoon.ui.componentes.ContadorRegen
import com.ejemplo.nettycoon.ui.componentes.MedidorSalud
import com.ejemplo.nettycoon.ui.theme.Espaciado
import com.ejemplo.nettycoon.ui.theme.NetTycoonTheme

/**
 * Panel del juego: hub central. Muestra el estado de la partida como un tablero de mando con la
 * SALUD DE LA RED como protagonista, y da acceso a las pantallas del juego.
 *
 * La simulación de ronda ya no ocurre aquí: se traslada a "Ataque en vivo". Los miembros
 * `simularAtaque`/`ultimaRonda` del [PanelViewModel] quedan sin uso a propósito en esta fase
 * (deuda anotada a limpiar más adelante); no se reabren en esta tarea.
 *
 * Punto de entrada con estado (conectado al ViewModel).
 */
@Composable
fun PanelScreen(
    viewModel: PanelViewModel,
    onIrAAtaqueEnVivo: () -> Unit,
    onIrAReglas: () -> Unit,
    onIrAConfigRed: () -> Unit,
    onIrAEstadisticas: () -> Unit,
    onIrATienda: () -> Unit,
    onVerOnboarding: () -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    PanelScreen(
        estado = estado,
        onLimpiarError = viewModel::limpiarError,
        onRefrescarRegen = viewModel::refrescarRegen,
        onIrAAtaqueEnVivo = onIrAAtaqueEnVivo,
        onIrAReglas = onIrAReglas,
        onIrAConfigRed = onIrAConfigRed,
        onIrAEstadisticas = onIrAEstadisticas,
        onIrATienda = onIrATienda,
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
    onRefrescarRegen: () -> Unit,
    onIrAAtaqueEnVivo: () -> Unit,
    onIrAReglas: () -> Unit,
    onIrAConfigRed: () -> Unit,
    onIrAEstadisticas: () -> Unit,
    onIrATienda: () -> Unit,
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
                    IconButton(onClick = onCerrarSesion) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Cerrar sesión",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Espaciado.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Espaciado.md),
        ) {
            TarjetaSalud(
                partida = estado.partida,
                anclaRegen = estado.anclaRegen,
                onRefrescarRegen = onRefrescarRegen,
            )

            estado.rango?.let { rango ->
                TarjetaRango(
                    rango = rango,
                    puntaje = estado.partida?.puntaje ?: 0,
                    puntosParaSiguiente = estado.puntosParaSiguienteRango,
                )
            }

            if (estado.partida != null) {
                MetricasSecundarias(partida = estado.partida)
            }

            // Acción primaria destacada.
            Button(
                onClick = onIrAAtaqueEnVivo,
                modifier = Modifier.fillMaxWidth(),
            ) {
                BotonContenido(Icons.Filled.Bolt, "Ataque en vivo")
            }

            OutlinedButton(onClick = onIrAReglas, modifier = Modifier.fillMaxWidth()) {
                BotonContenido(Icons.AutoMirrored.Filled.Rule, "Mis reglas")
            }
            OutlinedButton(onClick = onIrAConfigRed, modifier = Modifier.fillMaxWidth()) {
                BotonContenido(Icons.Filled.Router, "Configurar red")
            }
            OutlinedButton(onClick = onIrAEstadisticas, modifier = Modifier.fillMaxWidth()) {
                BotonContenido(Icons.Filled.BarChart, "Mi progreso")
            }
            OutlinedButton(onClick = onIrATienda, modifier = Modifier.fillMaxWidth()) {
                BotonContenido(Icons.Filled.Storefront, "Tienda")
            }
            OutlinedButton(onClick = onVerOnboarding, modifier = Modifier.fillMaxWidth()) {
                BotonContenido(Icons.AutoMirrored.Filled.HelpOutline, "Cómo se juega")
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

/** Icono + texto para los botones de navegación, con la separación estándar de M3. */
@Composable
private fun BotonContenido(icono: ImageVector, texto: String) {
    Icon(icono, contentDescription = null, modifier = Modifier.size(18.dp))
    Text(texto, modifier = Modifier.padding(start = ButtonDefaults.IconSpacing))
}

/** Tarjeta protagonista: salud de la red con el medidor animado y el contador de regen en vivo. */
@Composable
private fun TarjetaSalud(
    partida: EstadoPartida?,
    anclaRegen: Long?,
    onRefrescarRegen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Espaciado.md),
            verticalArrangement = Arrangement.spacedBy(Espaciado.sm),
        ) {
            Text(
                "Salud de la red",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (partida == null) {
                Text("Cargando partida…", style = MaterialTheme.typography.bodyMedium)
            } else {
                MedidorSalud(salud = partida.saludRed, maximo = 100)
                // Contador solo mientras la salud no está llena (E2.1); a 100 no se muestra.
                if (partida.saludRed in 1..99 && anclaRegen != null) {
                    ContadorRegen(
                        salud = partida.saludRed,
                        ancla = anclaRegen,
                        onPasoPendiente = onRefrescarRegen,
                        prefijo = "Próxima +5 en",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta de IDENTIDAD del jugador (E4): su rango, derivado del puntaje.
 *
 * Va a ancho completo y con el color primario porque el rango es "quién eres", no una métrica más;
 * por eso no se metió en la fila de celdas junto a Puntaje/Dinero/Nivel, donde habría quedado
 * disfrazado de dato menor.
 *
 * La línea de progreso ("faltan N para X") es la que da GRANULARIDAD: el rango solo cambia tres
 * veces en toda la partida, así que sin ella el jugador pasaría cientos de puntos sin ver moverse
 * nada. En el rango máximo esa línea se sustituye por el total de puntos.
 *
 * El rango NO bloquea nada: es un título, no una llave.
 */
@Composable
private fun TarjetaRango(
    rango: Rango,
    puntaje: Int,
    puntosParaSiguiente: Int?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(Espaciado.md),
            horizontalArrangement = Arrangement.spacedBy(Espaciado.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.MilitaryTech,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(Espaciado.xs)) {
                Text(
                    rango.etiqueta,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(rango.descripcion, style = MaterialTheme.typography.bodyMedium)

                val siguiente = siguienteRango(puntaje)
                val progreso = if (puntosParaSiguiente != null && siguiente != null) {
                    "$puntaje pts · faltan $puntosParaSiguiente para ${siguiente.etiqueta}"
                } else {
                    "$puntaje pts · rango máximo alcanzado"
                }
                Text(
                    progreso,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/** Fila de métricas secundarias (celdas del tablero): puntaje, dinero, nivel. */
@Composable
private fun MetricasSecundarias(partida: EstadoPartida, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Espaciado.sm),
    ) {
        CeldaMetrica(
            icono = Icons.Filled.Star,
            etiqueta = "Puntaje",
            valor = partida.puntaje.toString(),
            modifier = Modifier.weight(1f),
        )
        CeldaMetrica(
            icono = Icons.Filled.Paid,
            etiqueta = "Dinero",
            valor = "$${partida.dineroVirtual}",
            modifier = Modifier.weight(1f),
        )
        CeldaMetrica(
            icono = Icons.Filled.MilitaryTech,
            etiqueta = "Nivel",
            valor = partida.nivel.toString(),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CeldaMetrica(
    icono: ImageVector,
    etiqueta: String,
    valor: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Espaciado.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Espaciado.xs),
        ) {
            Icon(
                icono,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                valor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                etiqueta,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
            modifier = Modifier.padding(Espaciado.md),
            verticalArrangement = Arrangement.spacedBy(Espaciado.sm),
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
private fun PanelScreenPreview() {
    NetTycoonTheme {
        PanelScreen(
            estado = PanelUiState(
                partida = EstadoPartida(owner = "demo", puntaje = 30, saludRed = 40, nivel = 2),
                anclaRegen = System.currentTimeMillis(),
            ),
            onLimpiarError = {},
            onRefrescarRegen = {},
            onIrAAtaqueEnVivo = {},
            onIrAReglas = {},
            onIrAConfigRed = {},
            onIrAEstadisticas = {},
            onIrATienda = {},
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
            onRefrescarRegen = {},
            onIrAAtaqueEnVivo = {},
            onIrAReglas = {},
            onIrAConfigRed = {},
            onIrAEstadisticas = {},
            onIrATienda = {},
            onVerOnboarding = {},
            onCerrarSesion = {},
        )
    }
}
