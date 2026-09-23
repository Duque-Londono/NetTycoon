package com.ejemplo.nettycoon.domain.firewall

/**
 * Umbrales de puntaje de cada [Rango]. PROVISIONALES: los valida el equipo tras jugarlo.
 *
 * Calibrados contra lo que da una decisión en [BalancePartida] (+15 bloqueo correcto, +10 permiso
 * correcto, −5 falso positivo → media ≈ 12,5 por acierto) y contra las 39 situaciones del catálogo:
 * - Técnico ≈ 12 aciertos (un nivel jugado limpio).
 * - Analista ≈ 32 aciertos (casi todo el catálogo).
 * - Experto ≈ 64 aciertos (≈ 1,6 pasadas completas).
 *
 * Viven en su propio objeto (y no en [BalancePartida]) porque el rango no es balance de combate ni
 * de economía: es progresión, y se DERIVA del puntaje sin tocar el estado persistido.
 */
object UmbralesRango {
    const val PUNTAJE_TECNICO = 150
    const val PUNTAJE_ANALISTA = 400
    const val PUNTAJE_EXPERTO = 800
}

/**
 * Rango del jugador según su puntaje acumulado: los puntos son **medida de maestría, no moneda**.
 *
 * El rango es un TÍTULO visible (Panel y "Mi progreso"): **no bloquea niveles** ni ninguna función
 * del juego — se respeta la decisión de que el jugador empiece por el nivel de dificultad que
 * quiera. Tampoco se persiste: se deriva del `EstadoPartida.puntaje` que ya existe, así que no
 * añade ningún campo a Room ni requiere migración.
 *
 * Los rangos se declaran en orden ASCENDENTE de [puntajeMinimo]; [rangoPorPuntaje] depende de ese
 * orden.
 *
 * (En E5 el rango pasará además a determinar el cupo de reglas activas.)
 *
 * @property etiqueta nombre visible para el jugador.
 * @property descripcion una línea que explica qué significa el rango. COPY BORRADOR: lo afina el
 *   equipo.
 * @property puntajeMinimo puntaje a partir del cual se alcanza este rango.
 */
enum class Rango(
    val etiqueta: String,
    val descripcion: String,
    val puntajeMinimo: Int,
) {
    APRENDIZ(
        etiqueta = "Aprendiz",
        descripcion = "Estás empezando a reconocer qué tráfico es de fiar y cuál no.",
        puntajeMinimo = 0,
    ),
    TECNICO(
        etiqueta = "Técnico en Seguridad",
        descripcion = "Ya distingues lo legítimo de lo sospechoso en los casos habituales.",
        puntajeMinimo = UmbralesRango.PUNTAJE_TECNICO,
    ),
    ANALISTA(
        etiqueta = "Analista",
        descripcion = "Decides con criterio, incluso cuando el caso es ambiguo.",
        puntajeMinimo = UmbralesRango.PUNTAJE_ANALISTA,
    ),
    EXPERTO(
        etiqueta = "Experto en Seguridad",
        descripcion = "Dominas el oficio: aciertas donde otros se dejan engañar.",
        puntajeMinimo = UmbralesRango.PUNTAJE_EXPERTO,
    ),
}

/**
 * Rango que corresponde a un [puntaje]. Función pura, sin dependencias de Android/Room, para ser
 * testeable en JVM (patrón [RegenSalud] / `EstadoSalud`).
 *
 * Es **total**: acepta cualquier entero. Un puntaje negativo (imposible hoy, porque
 * [ConsecuenciasPartida] pone suelo en 0, pero se cubre por si el balance cambiara) cae en
 * [Rango.APRENDIZ], y por arriba satura en [Rango.EXPERTO].
 *
 * Al derivarse siempre del puntaje ACTUAL, **bajar de rango funciona solo**: el falso positivo
 * resta 5 puntos, y si eso cruza un umbral hacia abajo el rango baja sin necesidad de guardar nada.
 */
fun rangoPorPuntaje(puntaje: Int): Rango =
    Rango.entries.lastOrNull { puntaje >= it.puntajeMinimo } ?: Rango.APRENDIZ

/**
 * Rango inmediatamente superior al que da [puntaje], o `null` si ya se está en el máximo.
 */
fun siguienteRango(puntaje: Int): Rango? =
    Rango.entries.getOrNull(rangoPorPuntaje(puntaje).ordinal + 1)

/**
 * Puntos que faltan para alcanzar el siguiente rango, o `null` si ya se está en [Rango.EXPERTO].
 *
 * Existe para dar GRANULARIDAD a la UI: el rango solo cambia tres veces en toda la partida, así que
 * sin esto el jugador pasaría cientos de puntos sin ver moverse nada. Nunca devuelve negativo.
 */
fun puntajeParaSiguienteRango(puntaje: Int): Int? =
    siguienteRango(puntaje)?.let { (it.puntajeMinimo - puntaje).coerceAtLeast(0) }
