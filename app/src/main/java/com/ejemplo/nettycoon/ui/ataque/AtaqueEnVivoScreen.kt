package com.ejemplo.nettycoon.ui.ataque

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import com.ejemplo.nettycoon.domain.model.CategoriaResultado
import com.ejemplo.nettycoon.ui.theme.NetTycoonTheme

/**
 * Pantalla "Ataque en vivo" (FASE 1 de UX pedagógica): invierte el bucle para que el jugador
 * APRENDA. Llega un escenario explicado en lenguaje humano, el jugador DECIDE permitir/bloquear
 * y recibe feedback inmediato con una lección, acierte o falle.
 *
 * Punto de entrada con estado (conectado al ViewModel).
 */
@Composable
fun AtaqueEnVivoScreen(
    viewModel: AtaqueEnVivoViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    AtaqueEnVivoScreen(
        estado = estado,
        onPermitir = viewModel::onPermitir,
        onBloquear = viewModel::onBloquear,
        onSiguienteAtaque = viewModel::onSiguienteAtaque,
        onLimpiarError = viewModel::limpiarError,
        onVolver = onVolver,
        modifier = modifier,
    )
}

/** Versión sin estado (stateless) para previews y separación de responsabilidades. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtaqueEnVivoScreen(
    estado: AtaqueEnVivoUiState,
    onPermitir: () -> Unit,
    onBloquear: () -> Unit,
    onSiguienteAtaque: () -> Unit,
    onLimpiarError: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Ataque en vivo") },
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
            TarjetaContador(aciertos = estado.aciertos, rondas = estado.rondas)

            TarjetaSituacion(escenario = estado.escenario)
            TarjetaPista(pista = estado.escenario.textoPista)

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

            val resultado = estado.ultimoResultado
            if (resultado == null) {
                BotonesDecision(
                    habilitado = !estado.cargando && estado.partida != null,
                    onPermitir = onPermitir,
                    onBloquear = onBloquear,
                )
            } else {
                TarjetaVeredicto(resultado = resultado)
                Button(
                    onClick = onSiguienteAtaque,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Siguiente ataque")
                }
            }
        }
    }
}

@Composable
private fun TarjetaContador(aciertos: Int, rondas: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Tu progreso", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Aciertos: $aciertos / $rondas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TarjetaSituacion(escenario: EscenarioAtaque, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Situación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(escenario.textoSituacion, style = MaterialTheme.typography.bodyLarge)
            FilaDato("Servicio", escenario.servicioNombre)
            FilaDato("Puerto", escenario.puerto.toString())
            FilaDato("IP origen", escenario.ipAtacante)
            FilaDato("País", escenario.pais)
            FilaDato("ISP", escenario.isp)
        }
    }
}

@Composable
private fun TarjetaPista(pista: String, modifier: Modifier = Modifier) {
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
            Text("💡 Pista", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(pista, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * Dos botones grandes de igual peso visual. No se colorea "Bloquear" como acción peligrosa: ambas
 * decisiones pueden ser la correcta según el caso, y sesgar el color enseñaría lo contrario.
 */
@Composable
private fun BotonesDecision(
    habilitado: Boolean,
    onPermitir: () -> Unit,
    onBloquear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Button(
            onClick = onPermitir,
            enabled = habilitado,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text("Permitir", style = MaterialTheme.typography.titleMedium)
        }
        Button(
            onClick = onBloquear,
            enabled = habilitado,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ),
        ) {
            Text("Bloquear", style = MaterialTheme.typography.titleMedium)
        }
    }
}

/**
 * Veredicto. El tratamiento visual depende SOLO de [ResultadoDecision.acierto]: un permiso
 * correcto se celebra igual que un bloqueo correcto (dejar pasar tráfico legítimo es tan valioso
 * como frenar un ataque).
 */
@Composable
private fun TarjetaVeredicto(resultado: ResultadoDecision, modifier: Modifier = Modifier) {
    val colores = if (resultado.acierto) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
    Card(modifier = modifier.fillMaxWidth(), colors = colores) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                if (resultado.acierto) "✅ ¡Bien hecho!" else "❌ Cuidado",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(resultado.leccion, style = MaterialTheme.typography.bodyLarge)
            Text(
                "Efecto en tu red",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            FilaDato("Puntaje", conSigno(resultado.deltaPuntaje))
            FilaDato("Salud de la red", conSigno(resultado.deltaSalud))
            FilaDato("Dinero virtual", conSigno(resultado.deltaDinero))
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
            TextButton(onClick = onLimpiarError, modifier = Modifier.align(Alignment.End)) {
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
        Text(valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

/** Formatea un delta con signo explícito: "+15", "-20", "0". */
private fun conSigno(valor: Int): String = when {
    valor > 0 -> "+$valor"
    else -> valor.toString()
}

// --- Previews ---

private val escenarioDemoMalicioso = CatalogoAtaques.escenarios[1] // SSH sospechoso.
private val escenarioDemoLegitimo = CatalogoAtaques.escenarios[0] // HTTPS legítimo.

@Preview(showBackground = true, name = "Sin decidir")
@Composable
private fun AtaqueEnVivoSinDecidirPreview() {
    NetTycoonTheme {
        AtaqueEnVivoScreen(
            estado = AtaqueEnVivoUiState(
                escenario = escenarioDemoMalicioso,
                partida = EstadoPartida(owner = "demo"),
                cargando = false,
                aciertos = 2,
                rondas = 3,
            ),
            onPermitir = {}, onBloquear = {}, onSiguienteAtaque = {},
            onLimpiarError = {}, onVolver = {},
        )
    }
}

@Preview(showBackground = true, name = "Acierto al permitir")
@Composable
private fun AtaqueEnVivoAciertoPermitirPreview() {
    NetTycoonTheme {
        AtaqueEnVivoScreen(
            estado = AtaqueEnVivoUiState(
                escenario = escenarioDemoLegitimo,
                partida = EstadoPartida(owner = "demo"),
                cargando = false,
                aciertos = 3,
                rondas = 3,
                ultimoResultado = ResultadoDecision(
                    acierto = true,
                    categoria = CategoriaResultado.PERMISO_CORRECTO,
                    resultadoEvento = com.ejemplo.nettycoon.data.local.entity.ResultadoEvento.PERMITIDO,
                    leccion = escenarioDemoLegitimo.leccionAcierto,
                    deltaPuntaje = 10,
                    deltaSalud = 0,
                    deltaDinero = 50,
                ),
            ),
            onPermitir = {}, onBloquear = {}, onSiguienteAtaque = {},
            onLimpiarError = {}, onVolver = {},
        )
    }
}

@Preview(showBackground = true, name = "Fallo (brecha)")
@Composable
private fun AtaqueEnVivoFalloPreview() {
    NetTycoonTheme {
        AtaqueEnVivoScreen(
            estado = AtaqueEnVivoUiState(
                escenario = escenarioDemoMalicioso,
                partida = EstadoPartida(owner = "demo"),
                cargando = false,
                aciertos = 1,
                rondas = 2,
                ultimoResultado = ResultadoDecision(
                    acierto = false,
                    categoria = CategoriaResultado.BRECHA,
                    resultadoEvento = com.ejemplo.nettycoon.data.local.entity.ResultadoEvento.PERMITIDO,
                    leccion = escenarioDemoMalicioso.leccionError,
                    deltaPuntaje = 0,
                    deltaSalud = -20,
                    deltaDinero = -100,
                ),
            ),
            onPermitir = {}, onBloquear = {}, onSiguienteAtaque = {},
            onLimpiarError = {}, onVolver = {},
        )
    }
}
