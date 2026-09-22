package com.ejemplo.nettycoon.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Escala de espaciado del "Centro de operaciones".
 *
 * Fuente única de separaciones (paddings, gaps, márgenes) para mantener ritmo visual
 * consistente en las fases B–D. SOLO se define aquí; aún no se aplica en pantallas.
 *
 * Uso previsto: `Modifier.padding(Espaciado.md)`.
 */
object Espaciado {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}
