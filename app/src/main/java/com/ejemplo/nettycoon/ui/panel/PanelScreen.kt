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
import com.ejemplo.nettycoon.data.local.entity.EventoAtaque
import com.ejemplo.nettycoon.data.local.entity.ResultadoEvento
import com.ejemplo.nettycoon.domain.model.Ataque
import com.ejemplo.nettycoon.domain.model.CategoriaResultado
import com.ejemplo.nettycoon.domain.model.ResultadoRonda
import com.ejemplo.nettycoon.ui.theme.NetTycoonTheme

/**
 * Panel del juego (FASE 1): cierra el bucle de punta a punta con la pantalla más simple posible.
 *
 * Muestra el estado de la partida, un botón para simular un ataque (que ejecuta una ronda real
 * vía `ProcesarAtaqueUseCase`) y el resultado de la última ronda, con estados de UI visibles
 * (Cargando / Error). Sin CRUD de reglas ni configuración de red todavía: el motor opera con la
 * política por defecto (default-DENY).
 *
 * Punto de entrada con estado (conectado al ViewModel).
 */
@Composable
fun PanelScreen(
    viewModel: PanelViewModel,
    onIrAReglas: () -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    PanelScreen(
        estado = estado,
        onSimularAtaque = viewModel::simularAtaque,
        onLimpiarError = viewModel::limpiarError,
        onIrAReglas = onIrAReglas,
        onCerrarSesion = onCerrarSesion,
        modifier = modifier,
    )
}

/** Versión sin estado (stateless) para previews y separación de responsabilidades. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelScreen(
    estado: PanelUiState,
    onSimularAtaque: () -> Unit,
    onLimpiarError: () -> Unit,
    onIrAReglas: () -> Unit,
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
                onClick = onSimularAtaque,
                enabled = !estado.cargando,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Simular ataque")
            }

            OutlinedButton(
                onClick = onIrAReglas,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Mis reglas")
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

            estado.ultimaRonda?.let { ronda ->
                TarjetaUltimaRonda(ronda = ronda)
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
private fun TarjetaUltimaRonda(ronda: ResultadoRonda, modifier: Modifier = Modifier) {
    val evento = ronda.evento
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "Última ronda",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            FilaDato("IP atacante", evento.ipAtacante)
            FilaDato("País", evento.pais ?: "desconocido")
            FilaDato("ISP", evento.isp ?: "desconocido")
            FilaDato("Tráfico", textoResultado(evento.resultado))
            FilaDato(
                "Resultado",
                if (evento.acierto) "Acierto" else "Fallo",
            )
            FilaDato("Categoría", textoCategoria(ronda.evaluacion.categoria))
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

private fun textoResultado(resultado: ResultadoEvento): String = when (resultado) {
    ResultadoEvento.BLOQUEADO -> "Bloqueado"
    ResultadoEvento.PERMITIDO -> "Permitido"
}

private fun textoCategoria(categoria: CategoriaResultado): String = when (categoria) {
    CategoriaResultado.BLOQUEO_CORRECTO -> "Bloqueo correcto"
    CategoriaResultado.PERMISO_CORRECTO -> "Permiso correcto"
    CategoriaResultado.BRECHA -> "Brecha (pasó un ataque real)"
    CategoriaResultado.FALSO_POSITIVO -> "Falso positivo (bloqueaste tráfico legítimo)"
}

// --- Previews ---

@Preview(showBackground = true)
@Composable
private fun PanelScreenPreview() {
    NetTycoonTheme {
        PanelScreen(
            estado = PanelUiState(
                partida = EstadoPartida(owner = "demo", puntaje = 30, saludRed = 90, nivel = 2),
                ultimaRonda = ResultadoRonda(
                    ataque = Ataque("203.0.113.9", puertoDestino = 443, esMalicioso = true),
                    evaluacion = com.ejemplo.nettycoon.domain.model.ResultadoEvaluacion(
                        accionAplicada = com.ejemplo.nettycoon.data.local.entity.AccionFirewall.DENY,
                        reglaCoincidente = null,
                        resultado = ResultadoEvento.BLOQUEADO,
                        acierto = true,
                        categoria = CategoriaResultado.BLOQUEO_CORRECTO,
                    ),
                    evento = EventoAtaque(
                        owner = "demo",
                        ipAtacante = "203.0.113.9",
                        puertoDestino = 443,
                        pais = "Colombia",
                        isp = "ISP Ejemplo",
                        resultado = ResultadoEvento.BLOQUEADO,
                        acierto = true,
                    ),
                    estadoPartida = EstadoPartida(owner = "demo", puntaje = 30, saludRed = 90, nivel = 2),
                ),
            ),
            onSimularAtaque = {},
            onLimpiarError = {},
            onIrAReglas = {},
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
            onSimularAtaque = {},
            onLimpiarError = {},
            onIrAReglas = {},
            onCerrarSesion = {},
        )
    }
}
