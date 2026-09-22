package com.ejemplo.nettycoon.ui.componentes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ejemplo.nettycoon.ui.theme.ColoresJuego
import com.ejemplo.nettycoon.ui.theme.Espaciado
import com.ejemplo.nettycoon.ui.theme.LocalColoresJuego
import com.ejemplo.nettycoon.ui.theme.NetTycoonTheme

/**
 * Medidor de salud STATELESS y reutilizable (lo usa el Panel y, en fase C, "Ataque en vivo").
 *
 * Pinta una barra horizontal cuyo llenado y color reflejan la salud, junto con la cifra
 * "X / [maximo]" y una etiqueta de estado (Segura / En riesgo / Comprometida).
 *
 * Animaciones (Compose puro):
 * - Al aparecer, el llenado anima de 0 al valor actual.
 * - Cuando [salud] cambia, el llenado y el color animan al nuevo estado (útil para mostrar el
 *   "golpe" al recibir daño en fase C).
 *
 * El estado/color/etiqueta salen de [estadoSalud] (lógica pura) + [LocalColoresJuego] (fase A).
 * Los umbrales son PRESENTACIONALES y no afectan al motor.
 *
 * @param salud salud actual (se recorta a 0..[maximo] para el llenado).
 * @param maximo salud máxima (> 0). Por defecto 100.
 */
@Composable
fun MedidorSalud(
    salud: Int,
    modifier: Modifier = Modifier,
    maximo: Int = 100,
) {
    val maximoSeguro = if (maximo > 0) maximo else 1
    val saludRecortada = salud.coerceIn(0, maximoSeguro)
    val porcentaje = saludRecortada * 100 / maximoSeguro
    val estado = estadoSalud(porcentaje)

    val colores = LocalColoresJuego.current
    val (colorObjetivo, contenidoObjetivo, etiqueta) = atributosDe(estado, colores)

    // Llenado: arranca en 0 al aparecer y anima al valor; reanima cuando cambia la salud.
    val fraccionObjetivo = saludRecortada.toFloat() / maximoSeguro
    var objetivo by remember { mutableStateOf(0f) }
    LaunchedEffect(fraccionObjetivo) { objetivo = fraccionObjetivo }
    val fraccionAnimada by animateFloatAsState(
        targetValue = objetivo,
        label = "fraccionSalud",
    )
    val colorAnimado by animateColorAsState(
        targetValue = colorObjetivo,
        label = "colorSalud",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Espaciado.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "$saludRecortada / $maximoSeguro",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            // Etiqueta de estado en "chip" con el color semántico.
            Surface(
                color = colorAnimado,
                contentColor = contenidoObjetivo,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = etiqueta,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = Espaciado.sm, vertical = Espaciado.xs),
                )
            }
        }

        // Barra: track + relleno animado.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraccionAnimada)
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.small)
                    .background(colorAnimado),
            )
        }
    }
}

/** Mapea el estado presentacional a (color, colorContenido, etiqueta). */
private fun atributosDe(
    estado: EstadoSalud,
    colores: ColoresJuego,
): Triple<Color, Color, String> = when (estado) {
    EstadoSalud.SEGURA -> Triple(colores.success, colores.onSuccess, "Segura")
    EstadoSalud.EN_RIESGO -> Triple(colores.warning, colores.onWarning, "En riesgo")
    EstadoSalud.COMPROMETIDA -> Triple(colores.danger, colores.onDanger, "Comprometida")
}

// --- Previews ---

@Preview(showBackground = true)
@Composable
private fun MedidorSaludSeguraPreview() {
    NetTycoonTheme {
        Surface { MedidorSalud(salud = 90, modifier = Modifier.padding(Espaciado.md)) }
    }
}

@Preview(showBackground = true)
@Composable
private fun MedidorSaludEnRiesgoPreview() {
    NetTycoonTheme {
        Surface { MedidorSalud(salud = 40, modifier = Modifier.padding(Espaciado.md)) }
    }
}

@Preview(showBackground = true)
@Composable
private fun MedidorSaludComprometidaPreview() {
    NetTycoonTheme {
        Surface { MedidorSalud(salud = 20, modifier = Modifier.padding(Espaciado.md)) }
    }
}
