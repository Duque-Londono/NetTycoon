package com.ejemplo.nettycoon.ui.tienda

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ejemplo.nettycoon.data.local.entity.EstadoPartida
import com.ejemplo.nettycoon.ui.componentes.MedidorSalud
import com.ejemplo.nettycoon.ui.theme.Espaciado
import com.ejemplo.nettycoon.ui.theme.NetTycoonTheme

/**
 * Tienda (E3): la pantalla donde el dinero virtual por fin sirve para algo.
 *
 * Ofrece dos cosas — reparar salud y un escudo de un solo uso — y deja explícito en el texto que
 * **esperar sigue siendo gratis**: comprar es un atajo, nunca el único camino (la regeneración de
 * E2 es el piso garantizado). La pantalla no calcula precios ni decide qué se puede comprar: el
 * ViewModel le entrega cada opción ya resuelta.
 *
 * Punto de entrada con estado (conectado al ViewModel).
 */
@Composable
fun TiendaScreen(
    viewModel: TiendaViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    TiendaScreen(
        estado = estado,
        onComprar = viewModel::comprar,
        onLimpiarAviso = viewModel::limpiarAviso,
        onLimpiarError = viewModel::limpiarError,
        onVolver = onVolver,
        modifier = modifier,
    )
}

/** Versión sin estado (stateless) para previews y separación de responsabilidades. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TiendaScreen(
    estado: TiendaUiState,
    onComprar: (ArticuloTienda) -> Unit,
    onLimpiarAviso: () -> Unit,
    onLimpiarError: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Tienda") },
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
            if (estado.partida != null) {
                TarjetaEstado(partida = estado.partida)
            }

            estado.aviso?.let { mensaje ->
                TarjetaMensaje(
                    mensaje = mensaje,
                    onCerrar = onLimpiarAviso,
                    contenedor = MaterialTheme.colorScheme.secondaryContainer,
                    contenido = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            estado.opciones.forEach { opcion ->
                TarjetaOpcion(opcion = opcion, onComprar = { onComprar(opcion.articulo) })
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
                TarjetaMensaje(
                    mensaje = mensaje,
                    onCerrar = onLimpiarError,
                    contenedor = MaterialTheme.colorScheme.errorContainer,
                    contenido = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

/** Cabecera: con cuánto dinero cuenta el jugador, cómo está su red y si lleva escudo puesto. */
@Composable
private fun TarjetaEstado(partida: EstadoPartida, modifier: Modifier = Modifier) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Tu dinero",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "\$${partida.dineroVirtual}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            MedidorSalud(salud = partida.saludRed, maximo = 100)
            if (partida.escudoActivo) {
                Text(
                    "🛡️ Escudo activo: absorberá el próximo golpe de salud.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/**
 * Un artículo del escaparate. El botón se deshabilita cuando la compra no es posible y, en ese
 * caso, se muestra el motivo: el jugador nunca se queda adivinando por qué no puede comprar.
 */
@Composable
private fun TarjetaOpcion(
    opcion: OpcionTienda,
    onComprar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Espaciado.md),
            verticalArrangement = Arrangement.spacedBy(Espaciado.sm),
        ) {
            Text(
                opcion.titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(opcion.descripcion, style = MaterialTheme.typography.bodyMedium)

            opcion.razonDeshabilitada?.let { razon ->
                Text(
                    razon,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Button(
                onClick = onComprar,
                enabled = opcion.habilitada,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Comprar por \$${opcion.precio}")
            }
        }
    }
}

/** Tarjeta de mensaje descartable (aviso de compra o error), con los colores que se le pasen. */
@Composable
private fun TarjetaMensaje(
    mensaje: String,
    onCerrar: () -> Unit,
    contenedor: Color,
    contenido: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = contenedor,
            contentColor = contenido,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Espaciado.md),
            verticalArrangement = Arrangement.spacedBy(Espaciado.sm),
        ) {
            Text(mensaje, style = MaterialTheme.typography.bodyMedium)
            TextButton(
                onClick = onCerrar,
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
private fun TiendaScreenPreview() {
    NetTycoonTheme {
        TiendaScreen(
            estado = TiendaUiState(
                partida = EstadoPartida(owner = "demo", saludRed = 60, dineroVirtual = 450),
                opciones = listOf(
                    OpcionTienda(
                        articulo = ArticuloTienda.Cura(25),
                        titulo = "Reparar +25 de salud",
                        descripcion = "Recupera 25 de salud al instante, a \$6 por punto. " +
                            "Esperar sigue siendo gratis.",
                        precio = 150,
                        habilitada = true,
                    ),
                    OpcionTienda(
                        articulo = ArticuloTienda.Escudo,
                        titulo = "Escudo (un solo uso)",
                        descripcion = "Absorbe el próximo golpe de salud, sea una brecha o un " +
                            "falso positivo.",
                        precio = 100,
                        habilitada = true,
                    ),
                ),
                cargando = false,
            ),
            onComprar = {},
            onLimpiarAviso = {},
            onLimpiarError = {},
            onVolver = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TiendaScreenSinSaldoPreview() {
    NetTycoonTheme {
        TiendaScreen(
            estado = TiendaUiState(
                partida = EstadoPartida(
                    owner = "demo",
                    saludRed = 20,
                    dineroVirtual = 30,
                    escudoActivo = true,
                ),
                opciones = listOf(
                    OpcionTienda(
                        articulo = ArticuloTienda.Cura(25),
                        titulo = "Reparar +25 de salud",
                        descripcion = "Recupera 25 de salud al instante, a \$6 por punto. " +
                            "Esperar sigue siendo gratis.",
                        precio = 150,
                        habilitada = false,
                        razonDeshabilitada = "Te faltan \$120.",
                    ),
                    OpcionTienda(
                        articulo = ArticuloTienda.Escudo,
                        titulo = "Escudo (un solo uso)",
                        descripcion = "Absorbe el próximo golpe de salud.",
                        precio = 100,
                        habilitada = false,
                        razonDeshabilitada = "Ya tienes un escudo activo: no son apilables.",
                    ),
                ),
                cargando = false,
            ),
            onComprar = {},
            onLimpiarAviso = {},
            onLimpiarError = {},
            onVolver = {},
        )
    }
}
