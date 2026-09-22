package com.ejemplo.nettycoon.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * ColorScheme del "Centro de operaciones" (tema oscuro).
 *
 * Se definen TODOS los roles de M3 usados por la app para que ninguno caiga al
 * default de Material. Las pantallas consumen estos roles vía
 * `MaterialTheme.colorScheme.*`; nunca los tokens crudos de Color.kt.
 */
private val EsquemaOscuroConsola = darkColorScheme(
    primary = ConsolaPrimary,
    onPrimary = ConsolaOnPrimary,
    primaryContainer = ConsolaPrimaryContainer,
    onPrimaryContainer = ConsolaOnPrimaryContainer,
    inversePrimary = ConsolaInversePrimary,

    secondary = ConsolaSecondary,
    onSecondary = ConsolaOnSecondary,
    secondaryContainer = ConsolaSecondaryContainer,
    onSecondaryContainer = ConsolaOnSecondaryContainer,

    tertiary = ConsolaTertiary,
    onTertiary = ConsolaOnTertiary,
    tertiaryContainer = ConsolaTertiaryContainer,
    onTertiaryContainer = ConsolaOnTertiaryContainer,

    background = ConsolaBackground,
    onBackground = ConsolaOnBackground,

    surface = ConsolaSurface,
    onSurface = ConsolaOnSurface,
    surfaceVariant = ConsolaSurfaceVariant,
    onSurfaceVariant = ConsolaOnSurfaceVariant,
    surfaceTint = ConsolaPrimary,

    surfaceDim = ConsolaSurfaceDim,
    surfaceBright = ConsolaSurfaceBright,
    surfaceContainerLowest = ConsolaSurfaceContainerLowest,
    surfaceContainerLow = ConsolaSurfaceContainerLow,
    surfaceContainer = ConsolaSurfaceContainer,
    surfaceContainerHigh = ConsolaSurfaceContainerHigh,
    surfaceContainerHighest = ConsolaSurfaceContainerHighest,

    inverseSurface = ConsolaInverseSurface,
    inverseOnSurface = ConsolaInverseOnSurface,

    outline = ConsolaOutline,
    outlineVariant = ConsolaOutlineVariant,
    scrim = ConsolaScrim,

    error = ConsolaError,
    onError = ConsolaOnError,
    errorContainer = ConsolaErrorContainer,
    onErrorContainer = ConsolaOnErrorContainer,
)

/**
 * Esqueleto de esquema CLARO. Vacío a propósito en esta fase: forzamos oscuro
 * siempre para tener identidad consistente. Se deja aquí, ya cableado en
 * [NetTycoonTheme], para poder reactivar un tema claro más adelante sin
 * reescribir la estructura (bastaría rellenar estos tokens y activar la rama).
 */
// private val EsquemaClaroConsola = lightColorScheme( /* pendiente fase futura */ )

/**
 * Colores semánticos EXTENDIDOS del juego, fuera del [androidx.compose.material3.ColorScheme]
 * de M3. Se exponen vía [LocalColoresJuego] para que las fases B/C/D los usen de forma
 * consistente. Reservados a RESULTADO (acierto/fallo) y SALUD, nunca a las acciones
 * Permitir/Bloquear.
 */
@Immutable
data class ColoresJuego(
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val danger: Color,
    val onDanger: Color,
)

private val ColoresJuegoOscuro = ColoresJuego(
    success = JuegoSuccess,
    onSuccess = JuegoOnSuccess,
    warning = JuegoWarning,
    onWarning = JuegoOnWarning,
    danger = JuegoDanger,
    onDanger = JuegoOnDanger,
)

/**
 * Acceso a los colores semánticos del juego dentro del árbol de composición.
 * Uso previsto (fases posteriores): `LocalColoresJuego.current.success`.
 */
val LocalColoresJuego = staticCompositionLocalOf { ColoresJuegoOscuro }

/**
 * Tema raíz de NetTycoon.
 *
 * - Tema OSCURO aplicado SIEMPRE: se ignora `isSystemInDarkTheme()` para mantener
 *   identidad visual consistente.
 * - Sin dynamic color (Material You): sobreescribiría el cian de marca.
 * - Expone [ColoresJuego] vía [LocalColoresJuego] además del ColorScheme de M3.
 *
 * El parámetro [oscuro] queda cableado para poder reintroducir un esquema claro
 * en el futuro sin tocar los llamadores.
 */
@Composable
fun NetTycoonTheme(
    oscuro: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = EsquemaOscuroConsola // oscuro siempre en esta fase

    CompositionLocalProvider(LocalColoresJuego provides ColoresJuegoOscuro) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
