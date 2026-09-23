package com.ejemplo.nettycoon.ui.estadisticas

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ejemplo.nettycoon.domain.firewall.MapeoFamilias
import com.ejemplo.nettycoon.domain.firewall.Rango
import com.ejemplo.nettycoon.domain.firewall.siguienteRango
import com.ejemplo.nettycoon.ui.theme.Espaciado
import com.ejemplo.nettycoon.ui.theme.LocalColoresJuego
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
                .padding(Espaciado.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Espaciado.md),
        ) {
            // Cabecera de identidad (E4): va FUERA del when a propósito, para que el rango se
            // vea también cuando el historial está vacío (usuario nuevo = Aprendiz, 0 pts).
            estado.rango?.let { rango ->
                CabeceraRango(
                    rango = rango,
                    puntaje = estado.puntaje,
                    puntosParaSiguiente = estado.puntosParaSiguienteRango,
                )
            }

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

/**
 * Cabecera de identidad de "Mi progreso" (E4): el rango del jugador, derivado de su puntaje.
 *
 * Se muestra SIEMPRE que haya estado cargado, incluso sin historial: un usuario nuevo ve
 * "Aprendiz / 0 pts" en vez de una pantalla sin cabecera. El rango no bloquea nada; es un título.
 */
@Composable
private fun CabeceraRango(
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
        Column(
            modifier = Modifier.padding(Espaciado.md),
            verticalArrangement = Arrangement.spacedBy(Espaciado.xs),
        ) {
            Text(
                "Tu rango",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                rango.etiqueta,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(rango.descripcion, style = MaterialTheme.typography.bodyMedium)

            val siguiente = siguienteRango(puntaje)
            Text(
                if (puntosParaSiguiente != null && siguiente != null) {
                    "$puntaje pts · faltan $puntosParaSiguiente para ${siguiente.etiqueta}"
                } else {
                    "$puntaje pts · rango máximo alcanzado"
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
            )
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
    // Métricas globales, con la Tasa de acierto DESTACADA (tipografía de display del tema).
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Espaciado.md),
            verticalArrangement = Arrangement.spacedBy(Espaciado.sm),
        ) {
            Text(
                "Tu desempeño",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            // Métrica protagonista: número grande + etiqueta.
            Text(
                "${estado.tasaAciertoPct}%",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Tasa de acierto",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilaMetrica("Ataques enfrentados", estado.totalAtaques.toString())
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
            modifier = Modifier.padding(Espaciado.md),
            verticalArrangement = Arrangement.spacedBy(Espaciado.sm + Espaciado.xs),
        ) {
            Text(
                titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            familias.forEach { familia ->
                Column(verticalArrangement = Arrangement.spacedBy(Espaciado.xs)) {
                    FilaMetrica(
                        etiqueta = familia.nombre,
                        valor = "${familia.aciertoPct}% (${familia.aciertos}/${familia.total})",
                    )
                    BarraProgreso(porcentaje = familia.aciertoPct, nivel = familia.nivel)
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

/**
 * Barra de progreso de dominio de una familia. Reusa el ENFOQUE de `MedidorSalud` (track +
 * relleno animado con Compose puro) sin refactorizar el componente compartido: el llenado anima
 * de 0 al valor al aparecer.
 *
 * El color respeta los umbrales YA existentes (vía [FamiliaResumen.nivel], que el ViewModel calcula
 * con DOMINADA ≥70 / FLOJA <50): DOMINADA = success (verde), FLOJA = danger (rojo), NEUTRA = neutro
 * (primary). Aquí verde/rojo es semántico de RESULTADO/desempeño, no de acción.
 */
@Composable
private fun BarraProgreso(
    porcentaje: Int,
    nivel: NivelDominio,
    modifier: Modifier = Modifier,
) {
    val colores = LocalColoresJuego.current
    val color: Color = when (nivel) {
        NivelDominio.DOMINADA -> colores.success
        NivelDominio.FLOJA -> colores.danger
        NivelDominio.NEUTRA -> MaterialTheme.colorScheme.primary
    }

    val fraccionObjetivo = (porcentaje.coerceIn(0, 100)) / 100f
    var objetivo by remember { mutableStateOf(0f) }
    LaunchedEffect(fraccionObjetivo) { objetivo = fraccionObjetivo }
    val fraccionAnimada by animateFloatAsState(targetValue = objetivo, label = "fraccionDominio")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraccionAnimada)
                .fillMaxHeight()
                .clip(MaterialTheme.shapes.small)
                .background(color),
        )
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
            modifier = Modifier.padding(Espaciado.md),
            verticalArrangement = Arrangement.spacedBy(Espaciado.sm),
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
