package com.ejemplo.nettycoon.ui.firewall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ejemplo.nettycoon.data.local.entity.AccionFirewall
import com.ejemplo.nettycoon.data.local.entity.ReglaFirewall
import com.ejemplo.nettycoon.ui.theme.Espaciado
import com.ejemplo.nettycoon.ui.theme.NetTycoonTheme
import com.ejemplo.nettycoon.ui.theme.TipografiaDatosTecnicos

/**
 * Pantalla de gestión de reglas de firewall: el jugador crea, activa/desactiva y elimina las
 * reglas ALLOW/DENY que el motor usará al simular ataques.
 *
 * Es la pieza que faltaba para que el jugador **decida** de verdad: sin reglas propias, el motor
 * solo aplica la política por defecto (DENY). Las reglas **automatizan** las decisiones que el
 * jugador ya aprendió a tomar a mano en "Ataque en vivo".
 *
 * Punto de entrada con estado (conectado al ViewModel).
 */
@Composable
fun FirewallScreen(
    viewModel: FirewallViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    FirewallScreen(
        estado = estado,
        onPuertoCambiado = viewModel::onPuertoCambiado,
        onIpCambiada = viewModel::onIpCambiada,
        onAccionCambiada = viewModel::onAccionCambiada,
        onCrearRegla = viewModel::crearRegla,
        onAlternarActiva = viewModel::alternarActiva,
        onEliminarRegla = viewModel::eliminarRegla,
        onLimpiarError = viewModel::limpiarError,
        onVolver = onVolver,
        modifier = modifier,
    )
}

/** Versión sin estado (stateless) para previews y separación de responsabilidades. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirewallScreen(
    estado: FirewallUiState,
    onPuertoCambiado: (String) -> Unit,
    onIpCambiada: (String) -> Unit,
    onAccionCambiada: (AccionFirewall) -> Unit,
    onCrearRegla: () -> Unit,
    onAlternarActiva: (ReglaFirewall) -> Unit,
    onEliminarRegla: (ReglaFirewall) -> Unit,
    onLimpiarError: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Mis reglas") },
                navigationIcon = {
                    TextButton(onClick = onVolver) { Text("Volver") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Espaciado.md),
            verticalArrangement = Arrangement.spacedBy(Espaciado.md),
        ) {
            item { TarjetaIntro() }

            if (!estado.cargando) {
                item { TarjetaCupo(estado = estado) }
            }

            item {
                FormularioRegla(
                    puertoTexto = estado.puertoTexto,
                    ipTexto = estado.ipTexto,
                    accion = estado.accion,
                    errorFormulario = estado.errorFormulario,
                    puedeCrear = estado.puedeCrear,
                    onPuertoCambiado = onPuertoCambiado,
                    onIpCambiada = onIpCambiada,
                    onAccionCambiada = onAccionCambiada,
                    onCrearRegla = onCrearRegla,
                )
            }

            estado.error?.let { mensaje ->
                item { TarjetaError(mensaje = mensaje, onLimpiarError = onLimpiarError) }
            }

            if (estado.cargando) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (estado.sinReglas) {
                item { TarjetaSinReglas() }
            }

            items(estado.reglas, key = { it.id }) { regla ->
                FilaRegla(
                    regla = regla,
                    onAlternarActiva = { onAlternarActiva(regla) },
                    onEliminar = { onEliminarRegla(regla) },
                )
            }
        }
    }
}

/**
 * Indicador del CUPO de reglas activas (E5): cuántas tienes de cuántas puedes.
 *
 * Tres estados, con tres tonos distintos a propósito:
 * - **Con hueco:** informativo y discreto.
 * - **En el tope:** avisa de que no puedes crear más y dice cómo conseguir hueco.
 * - **Sobre el cupo** (bajaste de rango): lo explica sin dramatizar. NO es un error ni un castigo,
 *   y por eso no usa el rol de error del tema; el texto deja claro que no se le ha quitado
 *   ninguna regla y cuál es la salida.
 *
 * COPY BORRADOR: lo afina el equipo.
 */
@Composable
private fun TarjetaCupo(estado: FirewallUiState, modifier: Modifier = Modifier) {
    val contenedor = when {
        estado.sobreCupo || !estado.puedeCrear -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contenido = when {
        estado.sobreCupo || !estado.puedeCrear -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = contenedor,
            contentColor = contenido,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Espaciado.md),
            verticalArrangement = Arrangement.spacedBy(Espaciado.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Reglas activas",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    buildString {
                        append("${estado.reglasActivas} / ${estado.cupo}")
                        if (estado.sobreCupo) append(" · sobre tu cupo")
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                when {
                    estado.sobreCupo ->
                        "Bajaste de rango, así que tu cupo se redujo a ${estado.cupo}. No te " +
                            "hemos quitado ninguna regla: siguen todas aquí y las que estén " +
                            "activas siguen funcionando. Para crear o activar otra, desactiva " +
                            "antes alguna con su interruptor."
                    !estado.puedeCrear ->
                        "Alcanzaste tu cupo. Desactiva una regla para crear otra, o sube de rango " +
                            "para automatizar más."
                    else ->
                        "Tu rango te permite ${estado.cupo} reglas activas a la vez. Automatizar " +
                            "no puede cubrirlo todo: el resto lo decides tú, a mano."
                },
                style = MaterialTheme.typography.bodySmall,
            )
            estado.rango?.let {
                Text(
                    "Rango actual: ${it.etiqueta}",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

/** Explica, en lenguaje simple, qué es esta pantalla y para qué sirven las reglas. */
@Composable
private fun TarjetaIntro(modifier: Modifier = Modifier) {
    // Meta-info en rol NEUTRO (surfaceVariant): con Bloquear = secondary(índigo), un intro en
    // índigo colisionaría con la acción. El neutro deja el color de acción sin competencia.
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Espaciado.md),
            verticalArrangement = Arrangement.spacedBy(Espaciado.xs + 2.dp),
        ) {
            Text(
                "¿Qué son las reglas?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Las reglas automatizan tus decisiones: en vez de decidir cada ataque a mano " +
                    "(como en 'Ataque en vivo'), el firewall aplicará esto solo. Elige un puerto, " +
                    "decide si permitirlo o bloquearlo, y el motor lo hará por ti cada vez.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormularioRegla(
    puertoTexto: String,
    ipTexto: String,
    accion: AccionFirewall,
    errorFormulario: String?,
    puedeCrear: Boolean,
    onPuertoCambiado: (String) -> Unit,
    onIpCambiada: (String) -> Unit,
    onAccionCambiada: (AccionFirewall) -> Unit,
    onCrearRegla: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Espaciado.md),
            verticalArrangement = Arrangement.spacedBy(Espaciado.sm + Espaciado.xs),
        ) {
            Text(
                "Nueva regla",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            // --- Vía guiada (principal): elegir un puerto del catálogo ---
            SelectorPuerto(puertoTexto = puertoTexto, onPuertoElegido = onPuertoCambiado)

            // Explicación inmediata del puerto elegido/escrito (si está en el catálogo).
            CatalogoPuertos.buscar(puertoTexto.toIntOrNull())?.let { puerto ->
                Text(
                    CatalogoPuertos.etiqueta(puerto.numero),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(puerto.explicacion, style = MaterialTheme.typography.bodySmall)
            }

            // --- Vía avanzada: escribir un puerto manual (fuera del catálogo) ---
            OutlinedTextField(
                value = puertoTexto,
                onValueChange = onPuertoCambiado,
                label = { Text("Puerto (avanzado: escríbelo a mano)") },
                placeholder = { Text("Ej. 8080") },
                singleLine = true,
                isError = errorFormulario != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = ipTexto,
                onValueChange = onIpCambiada,
                label = { Text("IP (opcional)") },
                placeholder = { Text("Vacío = cualquier IP") },
                singleLine = true,
                isError = errorFormulario != null,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Déjala vacía para que la regla aplique a cualquier origen. Escribe una IP para " +
                    "que aplique solo a ese origen concreto.",
                style = MaterialTheme.typography.bodySmall,
            )

            Text("Acción", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(Espaciado.sm)) {
                AccionFirewall.entries.forEach { opcion ->
                    // Guarda del color: al seleccionar, Permitir = primary (cian) y Bloquear =
                    // secondary (índigo). Neutros y distintos, nunca verde/rojo.
                    FilterChip(
                        selected = accion == opcion,
                        onClick = { onAccionCambiada(opcion) },
                        label = { Text(textoAccion(opcion)) },
                        colors = coloresChipAccion(opcion),
                    )
                }
            }
            Text(
                "Permitir: dejas pasar ese tráfico (permitir de más = dejas entrar amenazas). " +
                    "Bloquear: lo frenas (bloquear de más = dejas sin servicio a usuarios " +
                    "legítimos, un 'falso positivo').",
                style = MaterialTheme.typography.bodySmall,
            )

            errorFormulario?.let { mensaje ->
                Text(
                    mensaje,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            // Con el cupo lleno el botón se deshabilita: la explicación la da la TarjetaCupo,
            // justo encima, para no repetir el mismo mensaje dos veces en la misma pantalla.
            Button(
                onClick = onCrearRegla,
                enabled = puedeCrear,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (puedeCrear) "Crear regla" else "Cupo lleno")
            }
        }
    }
}

/**
 * Colores del [FilterChip] de acción según la guarda del color: seleccionado, Permitir usa el rol
 * primary (cian) y Bloquear el secondary (índigo). Sin seleccionar, ambos quedan neutros (default).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun coloresChipAccion(accion: AccionFirewall) = when (accion) {
    AccionFirewall.ALLOW -> FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    )
    AccionFirewall.DENY -> FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.secondary,
        selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
    )
}

/**
 * Selector guiado de puerto (menú desplegable M3). Al elegir una opción, rellena el campo puerto
 * reutilizando el mismo `onPuertoElegido` (= `onPuertoCambiado` del ViewModel), sin cambiar el
 * contrato. Usa la etiqueta coherente "puerto — servicio".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorPuerto(
    puertoTexto: String,
    onPuertoElegido: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandido by remember { mutableStateOf(false) }
    val seleccionado = CatalogoPuertos.buscar(puertoTexto.toIntOrNull())
    val textoCampo = seleccionado?.let { CatalogoPuertos.etiqueta(it.numero) } ?: ""

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = textoCampo,
            onValueChange = {},
            readOnly = true,
            label = { Text("Elige un puerto común") },
            placeholder = { Text("Toca para ver la lista") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false },
        ) {
            CatalogoPuertos.puertos.forEach { puerto ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                CatalogoPuertos.etiqueta(puerto.numero),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(puerto.explicacion, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    onClick = {
                        onPuertoElegido(puerto.numero.toString())
                        expandido = false
                    },
                )
            }
        }
    }
}

@Composable
private fun FilaRegla(
    regla: ReglaFirewall,
    onAlternarActiva: () -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Espaciado.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Espaciado.xs),
            ) {
                // Acción + servicio (texto humano); el color de la acción sigue la guarda
                // (Permitir = cian, Bloquear = índigo), nunca verde/rojo.
                Text(
                    "${textoAccion(regla.accion)} · ${CatalogoPuertos.nombreDe(regla.puerto) ?: "Puerto personalizado"}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorAccion(regla.accion),
                )
                // Datos técnicos en monoespaciado (puerto e IP).
                Text(
                    "Puerto ${regla.puerto} · ${regla.ip?.let { "IP $it" } ?: "cualquier IP"}",
                    style = TipografiaDatosTecnicos,
                )
                Text(
                    if (regla.activa) "Activa" else "Inactiva (el motor la ignora)",
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = onEliminar) { Text("Eliminar") }
            }
            Switch(checked = regla.activa, onCheckedChange = { onAlternarActiva() })
        }
    }
}

@Composable
private fun TarjetaSinReglas(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Espaciado.md),
            verticalArrangement = Arrangement.spacedBy(Espaciado.xs),
        ) {
            Text(
                "Aún no tienes reglas",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Mientras no crees ninguna, el firewall bloquea todo el tráfico por defecto " +
                    "(política DENY). Crea tu primera regla arriba para empezar a decidir tú: " +
                    "por ejemplo, permitir el puerto 443 (webs seguras).",
                style = MaterialTheme.typography.bodyMedium,
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

private fun textoAccion(accion: AccionFirewall): String = when (accion) {
    AccionFirewall.ALLOW -> "Permitir"
    AccionFirewall.DENY -> "Bloquear"
}

/**
 * Color de la acción según la guarda pedagógica: Permitir = primary (cian), Bloquear = secondary
 * (índigo). NEUTROS y distintos, nunca verde/rojo — el rojo queda reservado a errores/fallos
 * reales, no a la acción "Bloquear".
 */
@Composable
private fun colorAccion(accion: AccionFirewall): Color = when (accion) {
    AccionFirewall.ALLOW -> MaterialTheme.colorScheme.primary
    AccionFirewall.DENY -> MaterialTheme.colorScheme.secondary
}

// --- Previews ---

@Preview(showBackground = true)
@Composable
private fun FirewallScreenPreview() {
    NetTycoonTheme {
        FirewallScreen(
            estado = FirewallUiState(
                cargando = false,
                puertoTexto = "22",
                reglas = listOf(
                    ReglaFirewall(
                        id = 1,
                        owner = "demo",
                        puerto = 443,
                        ip = null,
                        accion = AccionFirewall.ALLOW,
                    ),
                    ReglaFirewall(
                        id = 2,
                        owner = "demo",
                        puerto = 23,
                        ip = "203.0.113.9",
                        accion = AccionFirewall.DENY,
                        activa = false,
                    ),
                ),
            ),
            onPuertoCambiado = {},
            onIpCambiada = {},
            onAccionCambiada = {},
            onCrearRegla = {},
            onAlternarActiva = {},
            onEliminarRegla = {},
            onLimpiarError = {},
            onVolver = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FirewallScreenSinReglasPreview() {
    NetTycoonTheme {
        FirewallScreen(
            estado = FirewallUiState(cargando = false),
            onPuertoCambiado = {},
            onIpCambiada = {},
            onAccionCambiada = {},
            onCrearRegla = {},
            onAlternarActiva = {},
            onEliminarRegla = {},
            onLimpiarError = {},
            onVolver = {},
        )
    }
}
