package com.ejemplo.nettycoon.ui.ataque

import com.ejemplo.nettycoon.data.local.entity.AccionFirewall
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
 * - [evaluandoRegla]: `true` mientras se comprueba (antes de mostrar los botones) si alguna regla
 *   activa del jugador aplica al escenario actual. Muy breve (lectura local de Room); durante este
 *   lapso la UI muestra un indicador "Comprobando tus reglas…" en vez de los botones de decisión.
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
    val evaluandoRegla: Boolean = false,
    /**
     * `true` si la red está COMPROMETIDA (salud <= 0): el candado E2 bloquea SOLO esta pantalla
     * (no se pueden jugar ataques) hasta que la salud vuelva a ser > 0 por la regeneración por
     * tiempo real. El resto de la app sigue navegable. Se evalúa al entrar y al pedir el siguiente
     * ataque (tras mostrar el veredicto del golpe final).
     */
    val comprometida: Boolean = false,
    /**
     * Ancla de regeneración vigente (epoch millis), para derivar en la UI el contador "Jugable
     * en …" cuando la red está [comprometida] (E2.1). `null` hasta resolver la carga inicial.
     */
    val anclaRegen: Long? = null,
    val error: String? = null,
    /**
     * Aviso breve tras aceptar una sugerencia del puente (cuántas reglas se crearon, o que ya
     * estaban cubiertas), o `null`. Es transitorio: la UI lo muestra y luego llama a
     * `limpiarAvisoReglas()`. No es un error; va en su propio canal para no pisar [error].
     */
    val avisoReglas: String? = null,
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
    /**
     * Si la ronda fue resuelta AUTOMÁTICAMENTE por una regla activa del jugador (en vez de a mano),
     * describe qué regla actuó; `null` si fue una decisión manual (lo habitual). Con default `null`
     * para no romper llamadas/previews/tests que no la usan.
     *
     * En rondas automatizadas los `delta*` van en 0: NO afectan puntaje/salud/dinero ni cuentan como
     * acierto manual (el puntaje premia decidir a mano). El acierto/categoría sí son reales (los
     * calcula el motor comparando la acción de la regla con la verdad del escenario) para poder
     * mostrar honestamente si la regla acertó o abrió una brecha.
     */
    val automatizadaPor: AutomatizacionRegla? = null,
) {
    /** `true` si esta ronda la resolvió una regla del jugador en vez de una decisión manual. */
    val automatizada: Boolean get() = automatizadaPor != null
}

/**
 * Datos de la regla activa que resolvió una ronda automáticamente, para mostrarlos en la tarjeta
 * "🤖 Automatizado por tu regla". Es solo presentación (no es la entidad de Room).
 */
data class AutomatizacionRegla(
    /** Puerto de la regla que casó con el escenario. */
    val puerto: Int,
    /** IP de la regla, o `null` si la regla aplica a cualquier IP (comodín). */
    val ip: String?,
    /** Acción de la regla, en texto para el jugador: "Permitir" o "Bloquear". */
    val accionTexto: String,
)

/**
 * Sugerencia ACCIONABLE de automatizar con reglas de firewall un patrón (familia + acción) que el
 * jugador ya domina a mano. Ofrece dos caminos (ver `AtaqueEnVivoViewModel.automatizarPuerto` y
 * `automatizarFamilia`):
 *
 * 1. **Solo el puerto** [puerto]: crea una regla precisa y segura, la más conservadora.
 * 2. **La familia entera** [familia]: crea en lote una regla por cada puerto de [puertosFamilia];
 *    es cómoda pero TOSCA, porque aplicará [accion] también a tráfico futuro por esos puertos,
 *    incluidas amenazas disfrazadas dentro de la familia (de ahí la advertencia en la UI).
 *
 * Lleva la [accion] cruda ([AccionFirewall]) para poder crear las reglas, además del texto para
 * mostrarla al jugador.
 */
data class SugerenciaRegla(
    val puerto: Int,
    val servicio: String,
    /** Familia de decisión del patrón (de `MapeoFamilias`), p. ej. "Bases de datos". */
    val familia: String,
    /** Puertos que componen la familia (de `MapeoFamilias.puertosDe`), para el lote de la opción 2. */
    val puertosFamilia: List<Int>,
    /** Acción a automatizar (cruda), la misma que el jugador venía decidiendo con acierto. */
    val accion: AccionFirewall,
    /** Acción a automatizar, en texto para el jugador: "Bloquear" o "Permitir". */
    val accionTexto: String,
    /** Mensaje pedagógico completo, ya adaptado a la familia/puerto/decisión reales. */
    val texto: String,
)
