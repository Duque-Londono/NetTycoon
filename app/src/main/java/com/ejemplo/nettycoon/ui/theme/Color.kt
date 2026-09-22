package com.ejemplo.nettycoon.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Tokens de color del sistema de diseño "Centro de operaciones" (tema oscuro).
 *
 * Estos valores son la fuente única de verdad del color de la app. NO se usan
 * directamente en las pantallas: se inyectan en el [androidx.compose.material3.ColorScheme]
 * (ver Theme.kt) y las pantallas consumen los roles de `MaterialTheme.colorScheme`.
 *
 * Nomenclatura: `Consola<Rol>` para no chocar con nombres de roles de M3.
 */

// --- Superficies y fondo ---
val ConsolaBackground = Color(0xFF0B0E14)
val ConsolaOnBackground = Color(0xFFE6EAF0)
val ConsolaSurface = Color(0xFF151A22)
val ConsolaOnSurface = Color(0xFFE6EAF0)
val ConsolaSurfaceVariant = Color(0xFF1C232E) // paneles elevados
val ConsolaOnSurfaceVariant = Color(0xFF9AA5B1) // texto secundario
val ConsolaOutline = Color(0xFF2A323D) // bordes
val ConsolaOutlineVariant = Color(0xFF222A34) // bordes tenues / divisores

// Escala de contenedores de superficie (para tarjetas/paneles con distinta elevación)
val ConsolaSurfaceDim = Color(0xFF0B0E14)
val ConsolaSurfaceBright = Color(0xFF2A323D)
val ConsolaSurfaceContainerLowest = Color(0xFF090C11)
val ConsolaSurfaceContainerLow = Color(0xFF12161D)
val ConsolaSurfaceContainer = Color(0xFF151A22)
val ConsolaSurfaceContainerHigh = Color(0xFF1C232E)
val ConsolaSurfaceContainerHighest = Color(0xFF232B37)

// --- Primary (cian) ---
val ConsolaPrimary = Color(0xFF22D3EE)
val ConsolaOnPrimary = Color(0xFF04222A)
val ConsolaPrimaryContainer = Color(0xFF0E3A44)
val ConsolaOnPrimaryContainer = Color(0xFFA5F0FB)

// --- Secondary (índigo) ---
val ConsolaSecondary = Color(0xFF6366F1)
val ConsolaOnSecondary = Color(0xFFEEF0FF)
val ConsolaSecondaryContainer = Color(0xFF23264D)
val ConsolaOnSecondaryContainer = Color(0xFFC7CBFF)

// --- Tertiary (violeta suave; distinto de cian/índigo/verde) ---
val ConsolaTertiary = Color(0xFFC4B5FD)
val ConsolaOnTertiary = Color(0xFF241A38)
val ConsolaTertiaryContainer = Color(0xFF2E2547)
val ConsolaOnTertiaryContainer = Color(0xFFE9E2FF)

// --- Error / amenaza (rojo; reservado a error real, no a la acción "Bloquear") ---
val ConsolaError = Color(0xFFF87171)
val ConsolaOnError = Color(0xFF2A0A0A)
val ConsolaErrorContainer = Color(0xFF3A1212)
val ConsolaOnErrorContainer = Color(0xFFFCC7C7)

// --- Roles de inversión y varios (definidos para no caer al default de M3) ---
val ConsolaInverseSurface = Color(0xFFE6EAF0)
val ConsolaInverseOnSurface = Color(0xFF151A22)
val ConsolaInversePrimary = Color(0xFF0E3A44)
val ConsolaScrim = Color(0xFF000000)

// ---------------------------------------------------------------------------
// Colores semánticos EXTENDIDOS (M3 no los trae como roles).
// Se exponen desde el tema vía CompositionLocal (ver ColoresJuego en Theme.kt).
// Reservados a RESULTADO (acierto/fallo) y SALUD; NUNCA para las acciones
// Permitir/Bloquear (eso chivaría la respuesta). Solo definidos aquí.
// ---------------------------------------------------------------------------
val JuegoSuccess = Color(0xFF34D399) // acierto / seguro
val JuegoOnSuccess = Color(0xFF052E22)
val JuegoWarning = Color(0xFFFBBF24) // riesgo / salud media
val JuegoOnWarning = Color(0xFF2A1E00)
val JuegoDanger = Color(0xFFF87171) // fallo / salud baja (comparte valor con error M3)
val JuegoOnDanger = Color(0xFF2A0A0A)
