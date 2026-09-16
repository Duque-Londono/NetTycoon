package com.ejemplo.nettycoon.ui.ataque

import com.ejemplo.nettycoon.data.local.entity.EstadoPartida
import com.ejemplo.nettycoon.data.local.entity.ResultadoEvento
import com.ejemplo.nettycoon.domain.model.CategoriaResultado

/**
 * Estado de UI de la pantalla "Ataque en vivo".
 *
 * Modela tres momentos según [nivel] / [escenario] / [nivelCompletado]:
 * - **Selector de nivel** ([nivel] `== null`): el jugador aún no ha elegido dificultad; no hay
 *   escenario activo ([escenario] `== null`).
 * - **Jugando** ([nivel] `!= null` y [escenario] `!= null`): hay un escenario que decidir.
 * - **Nivel completado** ([nivel] `!= null` y [nivelCompletado] `== true`): se agotaron los
 *   escenarios del nivel; se ofrece repetir o cambiar de nivel.
 *
 * - [escenario]: el escenario que el jugador está viendo ahora, o `null` (selector/completado).
 * - [nivel]: dificultad elegida, o `null` mientras se elige.
 * - [nivelCompletado]: `true` cuando ya no quedan escenarios en el nivel actual.
 * - [partida]: estado actual de la partida (para mostrar métricas y aplicarles consecuencias).
 * - [ultimoResultado]: veredicto de la decisión tomada sobre [escenario], o `null` si el jugador
 *   aún no ha decidido (fase de "situación + pista").
 * - [aciertos] / [rondas]: contador visible de progreso pedagógico (se reinicia al cambiar de nivel).
 * - [cargando]: `true` durante la carga inicial de la partida.
 * - [error]: mensaje a mostrar si algo falla (persistencia), o `null`.
 */
data class AtaqueEnVivoUiState(
    val escenario: EscenarioAtaque? = null,
    val nivel: Dificultad? = null,
    val nivelCompletado: Boolean = false,
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
    /**
     * Sugerencia pedagógica para automatizar con una regla el patrón que el jugador acaba de
     * repetir con acierto, o `null` si esta ronda no dispara ninguna (lo habitual). Con default
     * `null` para no romper llamadas/previews/tests que no la usan.
     */
    val sugerencia: SugerenciaRegla? = null,
)

/**
 * Sugerencia (puramente EXPLICATIVA) de crear una regla de firewall para automatizar un patrón
 * que el jugador ya domina a mano. No crea ni prellena nada: solo enseña y ofrece ir al CRUD de
 * reglas.
 */
data class SugerenciaRegla(
    val puerto: Int,
    val servicio: String,
    /** Acción a automatizar, en texto para el jugador: "Bloquear" o "Permitir". */
    val accionTexto: String,
    /** Mensaje pedagógico completo, ya adaptado al puerto/servicio/decisión reales. */
    val texto: String,
)
