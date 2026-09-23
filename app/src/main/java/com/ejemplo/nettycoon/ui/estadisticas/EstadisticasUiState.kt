package com.ejemplo.nettycoon.ui.estadisticas

import com.ejemplo.nettycoon.domain.firewall.Rango

/**
 * Nivel de dominio de una familia según su tasa de acierto. Los umbrales son PROVISIONALES
 * (los valida el equipo): ver [EstadisticasViewModel.UMBRAL_DOMINADA] y [UMBRAL_FLOJA].
 */
enum class NivelDominio {
    /** Tasa de acierto alta: el jugador domina esta familia. */
    DOMINADA,

    /** Zona intermedia: ni dominada ni floja. */
    NEUTRA,

    /** Tasa de acierto baja: familia a reforzar. */
    FLOJA,
}

/**
 * Resumen de una familia de decisión (derivada de `EventoAtaque.puertoDestino` vía
 * [MapeoFamilias]), para pintar las secciones "Lo que dominas" / "A reforzar".
 */
data class FamiliaResumen(
    val nombre: String,
    val total: Int,
    val aciertos: Int,
    val aciertoPct: Int,
    val nivel: NivelDominio,
)

/**
 * Estado de UI de la pantalla "Mi progreso".
 *
 * Modela los tres estados que la pantalla debe distinguir:
 * - **Cargando** ([cargando] = `true`): aún no llegó la primera emisión del historial.
 * - **Vacío** ([estaVacio] = `true`): el usuario no tiene ningún ataque en su historial
 *   (usuario nuevo); la pantalla muestra un mensaje en vez de métricas.
 * - **Con datos**: [totalAtaques] > 0 y el resto de métricas pobladas.
 *
 * Todas las métricas se derivan **solo de lectura** del historial de `EventoAtaque` del
 * usuario; no se persiste nada nuevo. La agrupación por familia es curatorial y provisional
 * (ver [MapeoFamilias]).
 */
data class EstadisticasUiState(
    val cargando: Boolean = true,
    val totalAtaques: Int = 0,
    val aciertos: Int = 0,
    /** Tasa de acierto en porcentaje entero [0, 100]. `0` cuando no hay ataques. */
    val tasaAciertoPct: Int = 0,
    /** Familias ordenadas de mayor a menor tasa de acierto. */
    val familias: List<FamiliaResumen> = emptyList(),
    /**
     * Rango del jugador (E4), derivado del puntaje de su partida. `null` mientras carga.
     *
     * Un usuario nuevo SIN partida guardada se trata como puntaje 0, es decir [Rango.APRENDIZ]: la
     * cabecera se muestra igual, no se queda en blanco ni bloquea la pantalla.
     */
    val rango: Rango? = null,
    /** Puntaje acumulado del jugador (0 si aún no hay partida). */
    val puntaje: Int = 0,
    /** Puntos que faltan para el siguiente rango; `null` si ya está en el máximo o aún carga. */
    val puntosParaSiguienteRango: Int? = null,
    val error: String? = null,
) {
    /** `true` cuando ya cargó y no hay historial que mostrar (usuario nuevo). */
    val estaVacio: Boolean get() = !cargando && error == null && totalAtaques == 0

    /** Familias que el jugador domina (para la sección "Lo que dominas"). */
    val familiasDominadas: List<FamiliaResumen>
        get() = familias.filter { it.nivel == NivelDominio.DOMINADA }

    /** Familias a reforzar (para la sección "A reforzar"). */
    val familiasFlojas: List<FamiliaResumen>
        get() = familias.filter { it.nivel == NivelDominio.FLOJA }
}
