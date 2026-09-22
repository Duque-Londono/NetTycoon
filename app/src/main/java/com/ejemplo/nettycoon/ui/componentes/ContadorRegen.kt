package com.ejemplo.nettycoon.ui.componentes

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.ejemplo.nettycoon.domain.firewall.TiempoRegen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Contador EN VIVO de la próxima recuperación de salud (E2.1). Muestra `"$prefijo m:ss"` con el
 * tiempo restante derivado del ancla real ([TiempoRegen]), y cada segundo se refresca.
 *
 * **La vida NO se otorga aquí**: cuando el tramo se cumple, este composable solo invoca
 * [onPasoPendiente] para que quien corresponda REAPLIQUE la regeneración real (que sí persiste
 * sobre el ancla en prefs). Así el anti-farmeo queda intacto: el contador solo pinta y dispara.
 *
 * Si la salud ya está al máximo, no dibuja nada. Los dos [LaunchedEffect] se cancelan solos al
 * salir de composición (ligados al ciclo de vida; sin fugas).
 *
 * @param salud salud actual (0..100).
 * @param ancla epoch millis del ancla de regeneración vigente.
 * @param onPasoPendiente se invoca (una vez por tramo) cuando ya toca aplicar la recuperación.
 * @param prefijo texto delante del tiempo (p. ej. "Próxima +5 en" o "Jugable en").
 */
@Composable
fun ContadorRegen(
    salud: Int,
    ancla: Long,
    onPasoPendiente: () -> Unit,
    prefijo: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    // Reloj de UI: avanza cada segundo para repintar el número.
    var ahora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(1000)
            ahora = System.currentTimeMillis()
        }
    }

    // Disparo de la reaplicación: se arma por tramo (se reinicia al cambiar ancla/salud) y dispara
    // UNA vez cuando ya hay un paso pendiente, para no reaplicar en bucle cada segundo.
    LaunchedEffect(ancla, salud) {
        while (isActive) {
            if (TiempoRegen.pasoPendiente(salud, ancla, System.currentTimeMillis())) {
                onPasoPendiente()
                break
            }
            delay(1000)
        }
    }

    val restante = TiempoRegen.restanteMs(salud, ancla, ahora) ?: return
    Text(
        text = "$prefijo ${formatoMmSs(restante)}",
        style = style,
        color = color,
        modifier = modifier,
    )
}

/** Formatea milisegundos a "m:ss", redondeando hacia arriba para no mostrar 0:00 antes de tiempo. */
private fun formatoMmSs(ms: Long): String {
    val totalSeg = (ms + 999) / 1000
    val minutos = totalSeg / 60
    val segundos = totalSeg % 60
    return "%d:%02d".format(minutos, segundos)
}
