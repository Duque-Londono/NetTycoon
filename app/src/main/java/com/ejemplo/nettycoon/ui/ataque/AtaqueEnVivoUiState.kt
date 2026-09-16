package com.ejemplo.nettycoon.ui.ataque

import com.ejemplo.nettycoon.data.local.entity.EstadoPartida
import com.ejemplo.nettycoon.data.local.entity.ResultadoEvento
import com.ejemplo.nettycoon.domain.model.CategoriaResultado

/**
 * Estado de UI de la pantalla "Ataque en vivo".
 *
 * - [escenario]: el escenario que el jugador está viendo ahora.
 * - [partida]: estado actual de la partida (para mostrar métricas y aplicarles consecuencias).
 * - [ultimoResultado]: veredicto de la decisión tomada sobre [escenario], o `null` si el jugador
 *   aún no ha decidido (fase de "situación + pista").
 * - [aciertos] / [rondas]: contador visible de progreso pedagógico.
 * - [cargando]: `true` durante la carga inicial de la partida.
 * - [error]: mensaje a mostrar si algo falla (persistencia), o `null`.
 */
data class AtaqueEnVivoUiState(
    val escenario: EscenarioAtaque,
    val partida: EstadoPartida? = null,
    val ultimoResultado: ResultadoDecision? = null,
    val aciertos: Int = 0,
    val rondas: Int = 0,
    val cargando: Boolean = true,
    val error: String? = null,
) {
    /** `true` si el jugador ya decidió sobre el escenario actual (hay veredicto que mostrar). */
    val decisionTomada: Boolean get() = ultimoResultado != null
}

/**
 * Veredicto de una decisión del jugador, ya calculado y con las consecuencias aplicadas.
 *
 * Los `delta*` son el efecto real sobre la partida (diferencia antes/después de aplicar las
 * consecuencias del dominio), para mostrárselo al jugador de forma transparente.
 */
data class ResultadoDecision(
    val acierto: Boolean,
    val categoria: CategoriaResultado,
    /** Cómo se resolvió el tráfico según la decisión: BLOQUEADO o PERMITIDO. */
    val resultadoEvento: ResultadoEvento,
    /** Lección pedagógica correspondiente (de acierto o de error). */
    val leccion: String,
    val deltaPuntaje: Int,
    val deltaSalud: Int,
    val deltaDinero: Int,
)
