package com.ejemplo.nettycoon.domain.model

/**
 * Las 4 categorías reales de firewall que surgen de cruzar la "verdad" del ataque
 * (malicioso/legítimo) con la acción aplicada (DENY/ALLOW).
 *
 * - [BLOQUEO_CORRECTO]: malicioso + DENY  → acierto.
 * - [PERMISO_CORRECTO]: legítimo  + ALLOW → acierto.
 * - [BRECHA]:           malicioso + ALLOW → fallo (pasó un ataque real).
 * - [FALSO_POSITIVO]:   legítimo  + DENY  → fallo (se bloqueó tráfico legítimo).
 */
enum class CategoriaResultado {
    BLOQUEO_CORRECTO,
    PERMISO_CORRECTO,
    BRECHA,
    FALSO_POSITIVO,
}
