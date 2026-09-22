package com.ejemplo.nettycoon.ui.componentes

/**
 * Estado PRESENTACIONAL de la salud de la red, derivado del porcentaje de salud.
 *
 * Es solo de presentación: NO afecta al motor ni al balance del juego. Los umbrales viven
 * aquí para poder ajustarlos sin tocar la UI ni la lógica de juego.
 */
enum class EstadoSalud {
    /** >= 70 %: "Segura" (verde/success). */
    SEGURA,

    /** 40..69 %: "En riesgo" (ámbar/warning). */
    EN_RIESGO,

    /** < 40 %: "Comprometida" (rojo/danger). */
    COMPROMETIDA,
}

/**
 * Clasifica un porcentaje de salud (0..100) en un [EstadoSalud].
 *
 * Función pura, sin dependencias de Compose/Android, para ser testeable en JVM.
 * Umbrales (ajustables, presentacionales):
 * - `>= 70` -> [EstadoSalud.SEGURA]
 * - `40..69` -> [EstadoSalud.EN_RIESGO]
 * - `< 40` -> [EstadoSalud.COMPROMETIDA]
 *
 * @param saludPct porcentaje de salud; se admite cualquier entero y se interpreta por umbral
 *   (valores negativos caen en COMPROMETIDA; > 100 caen en SEGURA).
 */
fun estadoSalud(saludPct: Int): EstadoSalud = when {
    saludPct >= 70 -> EstadoSalud.SEGURA
    saludPct >= 40 -> EstadoSalud.EN_RIESGO
    else -> EstadoSalud.COMPROMETIDA
}
