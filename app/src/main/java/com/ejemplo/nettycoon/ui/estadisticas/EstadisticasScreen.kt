package com.ejemplo.nettycoon.ui.estadisticas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ejemplo.nettycoon.ui.theme.NetTycoonTheme

/**
 * Pantalla "Mi progreso": da uso al historial de `EventoAtaque` (rondas manuales y automatizadas)
 * para que el jugador vea su avance —total de ataques, tasa de acierto y familias que domina o
 * debe reforzar—, reforzando la sensación de que está aprendiendo.
 *
 * Distingue los tres estados que exige la tarea: **cargando**, **vacío** (usuario nuevo sin
 * historial) y **con datos**.
 *
 * Los textos son PLACEHOLDER para validación del equipo (regla del proyecto).
 *
 * Punto de entrada con estado (conectado al ViewModel).
 */
@Composable
fun EstadisticasScreen(
    viewModel: EstadisticasViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    EstadisticasScreen(estado = estado, onVolver = onVolver, modifier = modifier)
}

/** Versión sin estado (stateless) para previews y separación de responsabilidades. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadisticasScreen(
    estado: EstadisticasUiState,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Mi progreso") },
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
            when {
                estado.cargando -> ContenidoCargando()
                estado.error != null -> TarjetaMensaje(
                    titulo = "No se pudo cargar",
                    cuerpo = estado.error,
                    esError = true,
                )
                estado.estaVacio -> TarjetaMensaje(
                    titulo = "Aún no hay progreso",
                    // PLACEHOLDER: texto pedagógico a validar por el equipo.
                    cuerpo = "Todavía no has enfrentado ataques. Juega una ronda para empezar a ver tu progreso.",
                )
                else -> ContenidoConDatos(estado)
            }
        }
    }
}

@Composable
private fun ContenidoCargando() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ContenidoConDatos(estado: EstadisticasUiState) {
    // Métricas globales.
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Tu desempeño",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            FilaMetrica("Ataques enfrentados", estado.totalAtaques.toString())
            FilaMetrica("Tasa de acierto", "${estado.tasaAciertoPct}%")
            FilaMetrica("Aciertos / Total", "${estado.aciertos} / ${estado.totalAtaques}")
        }
    }

    // Lo que dominas.
    if (estado.familiasDominadas.isNotEmpty()) {
        SeccionFamilias(
            titulo = "Lo que dominas",
            familias = estado.familiasDominadas,
        )
    }

    // A reforzar.
    if (estado.familiasFlojas.isNotEmpty()) {
        SeccionFamilias(
            titulo = "A reforzar",
            familias = estado.familiasFlojas,
        )
    }
}

@Composable
private fun SeccionFamilias(titulo: String, familias: List<FamiliaResumen>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            familias.forEach { familia ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    FilaMetrica(
                        etiqueta = familia.nombre,
                        valor = "${familia.aciertoPct}% (${familia.aciertos}/${familia.total})",
                    )
                    // Descripción de una línea (PLACEHOLDER, a validar por el equipo).
                    MapeoFamilias.descripcionDe(familia.nombre)?.let { descripcion ->
                        Text(
                            descripcion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaMensaje(
    titulo: String,
    cuerpo: String,
    esError: Boolean = false,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (esError) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(cuerpo, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun FilaMetrica(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium)
        Text(valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

// --- Previews ---

@Preview(showBackground = true)
@Composable
private fun EstadisticasConDatosPreview() {
    NetTycoonTheme {
        EstadisticasScreen(
            estado = EstadisticasUiState(
                cargando = false,
                totalAtaques = 12,
                aciertos = 9,
                tasaAciertoPct = 75,
                familias = listOf(
                    FamiliaResumen("Acceso remoto", total = 4, aciertos = 4, aciertoPct = 100, nivel = NivelDominio.DOMINADA),
                    FamiliaResumen("Web", total = 4, aciertos = 3, aciertoPct = 75, nivel = NivelDominio.DOMINADA),
                    FamiliaResumen("Bases de datos", total = 4, aciertos = 1, aciertoPct = 25, nivel = NivelDominio.FLOJA),
                ),
            ),
            onVolver = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EstadisticasVacioPreview() {
    NetTycoonTheme {
        EstadisticasScreen(
            estado = EstadisticasUiState(cargando = false),
            onVolver = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EstadisticasCargandoPreview() {
    NetTycoonTheme {
        EstadisticasScreen(
            estado = EstadisticasUiState(cargando = true),
            onVolver = {},
        )
    }
}
