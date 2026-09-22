package com.ejemplo.nettycoon.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ejemplo.nettycoon.ui.theme.Espaciado
import com.ejemplo.nettycoon.ui.theme.NetTycoonTheme
import kotlinx.coroutines.launch

/**
 * Onboarding de primer contacto: carrusel de 4 páginas pedagógicas que enmarca la misión del
 * juego antes de entrar al panel. Se muestra una sola vez por dispositivo (el gate lo decide con
 * el flag persistido; ver [com.ejemplo.nettycoon.navigation.decidirDestinoInicial]).
 *
 * No tiene ViewModel a propósito: no hay lógica asíncrona, red ni BD que exponer. El estado del
 * carrusel es UI local y las decisiones (marcar visto + navegar) se elevan como callbacks
 * ([onTerminar], [onSaltar]) hacia el NavHost, que es dueño del flag y de la navegación.
 *
 * @param onTerminar se invoca al pulsar "Empezar" en la última página.
 * @param onSaltar se invoca al pulsar "Saltar" en cualquier página.
 */
@Composable
fun OnboardingScreen(
    onTerminar: () -> Unit,
    onSaltar: () -> Unit,
    modifier: Modifier = Modifier,
    paginas: List<PaginaOnboarding> = PAGINAS_ONBOARDING,
) {
    val pagerState = rememberPagerState(pageCount = { paginas.size })
    val scope = rememberCoroutineScope()
    val esUltima = pagerState.currentPage == paginas.lastIndex
    val esPrimera = pagerState.currentPage == 0

    // Surface con el fondo del tema para asegurar la identidad oscura de la pantalla completa.
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
    Column(modifier = Modifier.fillMaxSize()) {
        // "Saltar" siempre visible, arriba a la derecha.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Espaciado.sm, vertical = Espaciado.sm),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onSaltar) {
                Text(text = "Saltar")
            }
        }

        // Carrusel: ocupa el espacio flexible entre la barra superior y los controles.
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { indice ->
            PaginaOnboardingContenido(pagina = paginas[indice])
        }

        // Indicador de progreso (puntos).
        IndicadorPasos(
            total = paginas.size,
            actual = pagerState.currentPage,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Espaciado.md),
        )

        // Controles: "Atrás" (salvo en la 1ª) + "Siguiente"/"Empezar".
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Espaciado.lg)
                .padding(bottom = Espaciado.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Reserva el espacio del botón "Atrás" aunque esté oculto, para que "Siguiente"
            // no salte de posición entre páginas.
            AnimatedVisibility(visible = !esPrimera) {
                TextButton(
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    },
                ) {
                    Text(text = "Atrás")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (esUltima) {
                        onTerminar()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
            ) {
                Text(text = if (esUltima) "Empezar" else "Siguiente")
            }
        }
    }
    }
}

/** Contenido de una página: título y cuerpo centrados. */
@Composable
private fun PaginaOnboardingContenido(
    pagina: PaginaOnboarding,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Espaciado.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = pagina.titulo,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Espaciado.md))
        Text(
            text = pagina.texto,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Indicador de pasos con puntos: el actual resaltado con el color primario. */
@Composable
private fun IndicadorPasos(
    total: Int,
    actual: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Espaciado.sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { indice ->
            val activo = indice == actual
            val color: Color = if (activo) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
            Box(
                modifier = Modifier
                    .size(if (activo) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    NetTycoonTheme {
        Surface {
            OnboardingScreen(onTerminar = {}, onSaltar = {})
        }
    }
}
