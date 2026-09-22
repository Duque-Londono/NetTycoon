package com.ejemplo.nettycoon.ui.ataque

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ejemplo.nettycoon.data.local.entity.AccionFirewall
import com.ejemplo.nettycoon.data.local.entity.EstadoPartida
import com.ejemplo.nettycoon.data.local.entity.EventoAtaque
import com.ejemplo.nettycoon.data.local.entity.ReglaFirewall
import com.ejemplo.nettycoon.data.local.entity.ResultadoEvento
import com.ejemplo.nettycoon.data.repository.EventoAtaqueRepository
import com.ejemplo.nettycoon.data.repository.PartidaRepository
import com.ejemplo.nettycoon.data.repository.RegeneradorSalud
import com.ejemplo.nettycoon.data.repository.ReglaFirewallRepository
import com.ejemplo.nettycoon.domain.firewall.ConsecuenciasPartida
import com.ejemplo.nettycoon.domain.firewall.MapeoFamilias
import com.ejemplo.nettycoon.domain.firewall.MotorFirewall
import com.ejemplo.nettycoon.domain.firewall.aReglasEvaluables
import com.ejemplo.nettycoon.domain.model.Ataque
import com.ejemplo.nettycoon.domain.model.CategoriaResultado
import com.ejemplo.nettycoon.domain.model.ResultadoEvaluacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel de la pantalla "Ataque en vivo". Único intermediario entre [AtaqueEnVivoScreen] y la
 * capa de datos/dominio: la pantalla solo observa [estado] y llama a estos métodos.
 *
 * **No conoce Firebase ni Auth:** recibe el [uid] ya resuelto por la navegación (mismo patrón que
 * `PanelViewModel` / `ConfigRedViewModel`). Habla solo con repositorios, nunca con DAOs.
 *
 * **Reutiliza el dominio sin tocarlo:** construye un [ResultadoEvaluacion] a partir de la decisión
 * del jugador y delega el cálculo de puntaje/salud/dinero/nivel en [ConsecuenciasPartida.aplicar].
 * Los escenarios NO vienen del generador aleatorio, sino de un catálogo pedagógico fijo.
 *
 * **Puente automatizado (reglas → juego):** antes de pedir decisión, comprueba si alguna REGLA
 * ACTIVA del jugador aplica al escenario, reutilizando [MotorFirewall] (matching por puerto/IP,
 * precedencia DENY). Solo automatiza si una regla real casó ([ResultadoEvaluacion.reglaCoincidente]
 * `!= null`); NO deja que el default-DENY del motor auto-decida (eso mataría el juego manual). Una
 * ronda automatizada se muestra en una tarjeta aparte y NO afecta puntaje/contadores/puente, pero
 * SÍ se registra como [EventoAtaque] para un historial veraz.
 *
 * **Niveles + progresión:** el jugador elige un [Dificultad] antes de jugar; el ViewModel sirve solo
 * escenarios de ese nivel y avanza dentro de él con una PROGRESIÓN en memoria (del más simple al
 * menos obvio, según el orden del catálogo). El [seleccionarSiguiente] elige el índice del próximo
 * escenario DENTRO de la sublista del nivel, a partir del actual (o `null` al empezar el nivel) y del
 * tamaño de esa sublista. Es inyectable para que los tests sean deterministas; por defecto avanza de
 * forma secuencial. Si el índice devuelto queda fuera de rango, el nivel se marca completado.
 *
 * Toda la progresión (nivel, posición, contadores del puente) vive EN MEMORIA por sesión; no se
 * persiste en Room (igual que el contador del puente).
 */
class AtaqueEnVivoViewModel(
    private val uid: String,
    private val partidaRepo: PartidaRepository,
    private val eventoRepo: EventoAtaqueRepository,
    private val reglaRepo: ReglaFirewallRepository,
    private val regenerador: RegeneradorSalud,
    private val escenarios: List<EscenarioAtaque> = CatalogoAtaques.escenarios,
    nivelInicial: Dificultad? = null,
    private val seleccionarSiguiente: (actual: Int?, total: Int) -> Int = ::progresionSecuencial,
) : ViewModel() {

    /** Escenarios del nivel actualmente en juego, en orden de progresión (vacío en el selector). */
    private var nivelEscenarios: List<EscenarioAtaque> = emptyList()

    /** Posición del escenario actual dentro de [nivelEscenarios]. */
    private var posicion: Int = 0

    /**
     * Contador EN MEMORIA (de sesión, no se persiste) de cuántas veces el jugador ha ACERTADO la
     * misma decisión sobre la misma FAMILIA de puertos (no el puerto exacto): la clave es
     * `(familia, acción)`. Contar por familia hace que la sugerencia aparezca de forma natural con
     * la progresión (p. ej. acertar DENY en MySQL, PostgreSQL y MongoDB cuenta como 3 en la familia
     * "Bases de datos"), en vez de exigir 3 aciertos sobre el MISMO puerto. Solo se cuentan aciertos:
     * no queremos sugerir automatizar un error. Se pierde al salir de la pantalla (el ViewModel se
     * destruye) y se reinicia al cambiar de nivel.
     */
    private val aciertosPorPatron = mutableMapOf<Pair<String, AccionFirewall>, Int>()

    /** Patrones `(familia, acción)` para los que ya se mostró la sugerencia, para no repetirla. */
    private val patronesYaSugeridos = mutableSetOf<Pair<String, AccionFirewall>>()

    private val _estado = MutableStateFlow(AtaqueEnVivoUiState())
    val estado: StateFlow<AtaqueEnVivoUiState> = _estado.asStateFlow()

    init {
        cargarPartida()
        nivelInicial?.let { iniciarNivel(it) }
    }

    /**
     * Carga (o crea) la partida del usuario para mostrar métricas y aplicarles consecuencias.
     *
     * Antes de leer, aplica la regeneración de salud por tiempo real (E2) para que tanto las
     * métricas como el candado usen la salud al día. Si la salud regenerada sigue en 0, la red
     * queda [AtaqueEnVivoUiState.comprometida] y la pantalla bloquea el juego.
     */
    private fun cargarPartida() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                regenerador.aplicar(uid)
                val partida = partidaRepo.getOrCreatePartida(uid)
                _estado.update {
                    it.copy(
                        partida = partida,
                        comprometida = partida.saludRed <= SALUD_COMPROMETIDA,
                        cargando = false,
                    )
                }
            } catch (e: Exception) {
                _estado.update {
                    it.copy(cargando = false, error = mensajeDeError("cargar la partida", e))
                }
            }
        }
    }

    /**
     * El jugador elige un nivel de dificultad: empieza a jugar solo con los escenarios de ese
     * nivel, desde el más simple. Reinicia los contadores y el puente para un arranque limpio.
     */
    fun elegirNivel(nivel: Dificultad) = iniciarNivel(nivel)

    /** Reinicia el nivel actual desde el primer escenario (tras completarlo o para repetirlo). */
    fun reciclarNivel() {
        _estado.value.nivel?.let { iniciarNivel(it) }
    }

    /** Vuelve al selector de nivel (sin escenario activo). */
    fun cambiarNivel() {
        nivelEscenarios = emptyList()
        _estado.update {
            it.copy(
                nivel = null,
                escenario = null,
                nivelCompletado = false,
                ultimoResultado = null,
                aciertos = 0,
                rondas = 0,
                evaluandoRegla = false,
            )
        }
    }

    /**
     * Prepara y arranca un nivel: filtra el catálogo por [nivel] (conservando el orden del catálogo
     * = de más simple a menos obvio), reinicia contadores y el puente, y carga el primer escenario.
     */
    private fun iniciarNivel(nivel: Dificultad) {
        nivelEscenarios = escenarios.filter { it.dificultad == nivel }
        aciertosPorPatron.clear()
        patronesYaSugeridos.clear()

        if (nivelEscenarios.isEmpty()) {
            // Defensa: un nivel sin escenarios se trata como completado (no ocurre con el catálogo).
            _estado.update {
                it.copy(
                    nivel = nivel, escenario = null, nivelCompletado = true,
                    ultimoResultado = null, aciertos = 0, rondas = 0,
                )
            }
            return
        }

        posicion = seleccionarSiguiente(null, nivelEscenarios.size)
        val primero = nivelEscenarios[posicion]
        _estado.update {
            it.copy(
                nivel = nivel,
                escenario = primero,
                nivelCompletado = false,
                ultimoResultado = null,
                aciertos = 0,
                rondas = 0,
                evaluandoRegla = true,
            )
        }
        evaluarAutomatizacion(primero)
    }

    /** El jugador decide dejar pasar el tráfico (ALLOW). */
    fun onPermitir() = decidir(AccionFirewall.ALLOW)

    /** El jugador decide bloquear el tráfico (DENY). */
    fun onBloquear() = decidir(AccionFirewall.DENY)

    /**
     * Aplica la decisión: calcula acierto y categoría, reutiliza [ConsecuenciasPartida] para las
     * consecuencias, persiste la partida y registra el [EventoAtaque]. Ignora la llamada si ya se
     * decidió sobre el escenario actual o si aún no hay partida cargada.
     */
    private fun decidir(accion: AccionFirewall) {
        val actual = _estado.value
        if (actual.decisionTomada || actual.cargando) return
        val partida = actual.partida ?: return
        val escenario = actual.escenario ?: return

        val bloquear = accion == AccionFirewall.DENY
        // acierto = (malicioso && bloquear) || (legítimo && permitir).
        val acierto = escenario.esMalicioso == bloquear
        val categoria = categoriaDe(escenario.esMalicioso, bloquear)
        val resultadoEvento =
            if (bloquear) ResultadoEvento.BLOQUEADO else ResultadoEvento.PERMITIDO

        val evaluacion = ResultadoEvaluacion(
            accionAplicada = accion,
            reglaCoincidente = null, // decisión manual del jugador, sin regla del CRUD.
            resultado = resultadoEvento,
            acierto = acierto,
            categoria = categoria,
        )

        val ahora = System.currentTimeMillis()
        val actualizada = ConsecuenciasPartida.aplicar(partida, evaluacion, ahora)

        val sugerencia = calcularSugerencia(escenario, accion, acierto)

        val veredicto = ResultadoDecision(
            acierto = acierto,
            categoria = categoria,
            resultadoEvento = resultadoEvento,
            leccion = if (acierto) escenario.leccionAcierto else escenario.leccionError,
            deltaPuntaje = actualizada.puntaje - partida.puntaje,
            deltaSalud = actualizada.saludRed - partida.saludRed,
            deltaDinero = actualizada.dineroVirtual - partida.dineroVirtual,
            sugerencia = sugerencia,
        )

        // Publicamos el veredicto de inmediato (feedback inmediato al jugador) y persistimos.
        // Al ser una decisión manual, dejamos de "evaluar reglas" (si la comprobación seguía en
        // curso, esta decisión gana y la corrutina de evaluación no la pisará).
        _estado.update {
            it.copy(
                partida = actualizada,
                ultimoResultado = veredicto,
                aciertos = it.aciertos + if (acierto) 1 else 0,
                rondas = it.rondas + 1,
                evaluandoRegla = false,
            )
        }

        viewModelScope.launch {
            try {
                eventoRepo.registrarEvento(
                    EventoAtaque(
                        owner = uid,
                        ipAtacante = escenario.ipAtacante,
                        puertoDestino = escenario.puerto,
                        pais = escenario.pais,
                        isp = escenario.isp,
                        resultado = resultadoEvento,
                        acierto = acierto,
                        ocurridoEn = ahora,
                    ),
                )
                partidaRepo.actualizarPartida(actualizada)
            } catch (e: Exception) {
                _estado.update {
                    it.copy(error = mensajeDeError("guardar el resultado", e))
                }
            }
        }
    }

    /**
     * Lógica del "puente" hacia las reglas: cuenta EN MEMORIA los aciertos repetidos del mismo
     * patrón (FAMILIA + acción, no puerto exacto) y, al alcanzar [UMBRAL_SUGERENCIA] por primera
     * vez, devuelve una [SugerenciaRegla] explicativa. Devuelve `null` si la decisión fue un error
     * (no se cuenta) o si el patrón aún no llega al umbral o ya se sugirió antes en esta sesión.
     */
    private fun calcularSugerencia(
        escenario: EscenarioAtaque,
        accion: AccionFirewall,
        acierto: Boolean,
    ): SugerenciaRegla? {
        if (!acierto) return null
        // Imposible apaga las ayudas: la sugerencia de automatizar ni se genera (la auto-aplicación
        // de reglas ya existentes sí sigue actuando, eso es consistencia del firewall, no una ayuda).
        if (_estado.value.nivel == Dificultad.IMPOSIBLE) return null

        val familia = MapeoFamilias.familiaDe(escenario.puerto)
        val patron = familia to accion
        val conteo = (aciertosPorPatron[patron] ?: 0) + 1
        aciertosPorPatron[patron] = conteo

        if (conteo < UMBRAL_SUGERENCIA || patron in patronesYaSugeridos) return null
        patronesYaSugeridos.add(patron)

        val bloquear = accion == AccionFirewall.DENY
        val accionTexto = if (bloquear) "Bloquear" else "Permitir"
        // COPY BORRADOR (validación del equipo): cuerpo de la tarjeta. No es prosa educativa extensa.
        val texto = "Acertaste $conteo veces en la familia \"$familia\" con la acción " +
            "\"$accionTexto\". Puedes crear reglas para que el firewall lo haga solo."

        return SugerenciaRegla(
            puerto = escenario.puerto,
            servicio = escenario.servicioNombre,
            familia = familia,
            puertosFamilia = MapeoFamilias.puertosDe(familia),
            accion = accion,
            accionTexto = accionTexto,
            texto = texto,
        )
    }

    /**
     * Opción (1) del puente — **precisa y segura**: crea UNA regla para el puerto exacto que el
     * jugador venía decidiendo, con la acción del patrón e IP comodín (`null` = cualquier IP).
     * Tras crearla, quita la tarjeta y publica un aviso. No hace nada si no hay sugerencia activa.
     */
    fun automatizarPuerto() {
        val sugerencia = _estado.value.ultimoResultado?.sugerencia ?: return
        crearReglasEnLote(listOf(sugerencia.puerto), sugerencia.accion)
    }

    /**
     * Opción (2) del puente — **cómoda pero TOSCA**: crea en lote una regla por cada puerto de la
     * familia (IP comodín, misma acción). Aplicará la acción también a tráfico futuro por esos
     * puertos, incluidas amenazas disfrazadas en la familia (la UI lo advierte). Filtra los puertos
     * que ya tienen una regla activa del jugador para no duplicar. No hace nada sin sugerencia.
     */
    fun automatizarFamilia() {
        val sugerencia = _estado.value.ultimoResultado?.sugerencia ?: return
        crearReglasEnLote(sugerencia.puertosFamilia, sugerencia.accion)
    }

    /**
     * Crea reglas activas (IP comodín) para [puertos] con [accion], filtrando los puertos que ya
     * tienen una regla ACTIVA del jugador (para no chocar/duplicar). Persiste vía [reglaRepo],
     * quita la tarjeta de sugerencia y publica un [AtaqueEnVivoUiState.avisoReglas]. El filtrado y la
     * escritura viven aquí (ViewModel → Repository), nunca en la UI.
     */
    private fun crearReglasEnLote(puertos: List<Int>, accion: AccionFirewall) {
        // Quitamos la tarjeta de inmediato (feedback: la acción se aceptó) y persistimos en segundo
        // plano; el aviso final llega al terminar la escritura.
        _estado.update {
            it.copy(ultimoResultado = it.ultimoResultado?.copy(sugerencia = null))
        }
        viewModelScope.launch {
            try {
                val yaCubiertos = reglaRepo.obtenerReglasActivas(uid).map { it.puerto }.toSet()
                val nuevos = puertos.filter { it !in yaCubiertos }
                nuevos.forEach { puerto ->
                    reglaRepo.guardarRegla(
                        ReglaFirewall(owner = uid, puerto = puerto, ip = null, accion = accion),
                    )
                }
                val accionTexto = if (accion == AccionFirewall.DENY) "Bloquear" else "Permitir"
                val aviso = when {
                    nuevos.isEmpty() -> "Ya tenías reglas activas para esos puertos; no se creó ninguna."
                    nuevos.size == 1 -> "Regla creada: $accionTexto en el puerto ${nuevos.first()}."
                    else -> "Se crearon ${nuevos.size} reglas ($accionTexto) para esos puertos."
                }
                _estado.update { it.copy(avisoReglas = aviso) }
            } catch (e: Exception) {
                _estado.update { it.copy(error = mensajeDeError("crear las reglas", e)) }
            }
        }
    }

    /** Descarta el aviso de reglas (tras mostrarlo al usuario). */
    fun limpiarAvisoReglas() = _estado.update { it.copy(avisoReglas = null) }

    /**
     * Avanza al siguiente escenario del nivel y limpia el veredicto. Si el selector devuelve un
     * índice fuera de rango (progresión agotada), marca el nivel como completado.
     */
    fun onSiguienteAtaque() {
        if (nivelEscenarios.isEmpty()) return
        // Candado E2: si el golpe recién mostrado dejó la red comprometida (salud <= 0), no se
        // carga la siguiente ronda; la pantalla muestra el bloqueo (el veredicto ya se vio).
        if ((_estado.value.partida?.saludRed ?: 1) <= SALUD_COMPROMETIDA) {
            _estado.update { it.copy(escenario = null, ultimoResultado = null, comprometida = true) }
            return
        }
        val siguiente = seleccionarSiguiente(posicion, nivelEscenarios.size)
        if (siguiente !in nivelEscenarios.indices) {
            _estado.update {
                it.copy(escenario = null, ultimoResultado = null, nivelCompletado = true)
            }
            return
        }
        posicion = siguiente
        val escenario = nivelEscenarios[siguiente]
        _estado.update {
            it.copy(escenario = escenario, ultimoResultado = null, evaluandoRegla = true)
        }
        evaluarAutomatizacion(escenario)
    }

    /**
     * Comprueba, ANTES de pedir decisión al jugador, si alguna regla activa suya resuelve el
     * [escenario]. Reutiliza [MotorFirewall] (matching + precedencia DENY) sobre las reglas activas
     * del uid. Solo automatiza si una regla real casó ([ResultadoEvaluacion.reglaCoincidente] no
     * nulo); si ninguna aplica, NO deja que el default-DENY del motor decida: baja [evaluandoRegla]
     * y se dejan los botones para decisión manual (como hoy).
     *
     * Es defensiva ante carreras: si al terminar la lectura el escenario cambió o ya hay una
     * decisión (p. ej. manual), no publica nada.
     */
    private fun evaluarAutomatizacion(escenario: EscenarioAtaque) {
        viewModelScope.launch {
            val reglas = try {
                reglaRepo.obtenerReglasActivas(uid)
            } catch (e: Exception) {
                // Si no se pueden leer las reglas, se juega a mano (no bloqueamos el juego).
                emptyList()
            }

            // Si mientras leíamos cambió el escenario o el jugador ya decidió, no hacemos nada.
            if (_estado.value.escenario != escenario || _estado.value.ultimoResultado != null) return@launch

            val evaluacion = MotorFirewall.evaluar(
                ataque = Ataque(
                    ipAtacante = escenario.ipAtacante,
                    puertoDestino = escenario.puerto,
                    esMalicioso = escenario.esMalicioso,
                ),
                reglas = reglas.aReglasEvaluables(),
            )
            val regla = evaluacion.reglaCoincidente

            // Ninguna regla del jugador casó: se decide a mano (el default-DENY del motor NO cuenta).
            if (regla == null) {
                _estado.update {
                    if (it.escenario == escenario && it.ultimoResultado == null) {
                        it.copy(evaluandoRegla = false)
                    } else {
                        it
                    }
                }
                return@launch
            }

            // Una regla real casó: la ronda se resuelve sola. NO toca puntaje/salud/dinero ni los
            // contadores/puente; el acierto/categoría los da el motor (verdad del escenario vs. acción).
            val bloquear = evaluacion.accionAplicada == AccionFirewall.DENY
            val veredicto = ResultadoDecision(
                acierto = evaluacion.acierto,
                categoria = evaluacion.categoria,
                resultadoEvento = evaluacion.resultado,
                leccion = if (evaluacion.acierto) escenario.leccionAcierto else escenario.leccionError,
                deltaPuntaje = 0,
                deltaSalud = 0,
                deltaDinero = 0,
                sugerencia = null,
                automatizadaPor = AutomatizacionRegla(
                    puerto = regla.puerto,
                    ip = regla.ip,
                    accionTexto = if (bloquear) "Bloquear" else "Permitir",
                ),
            )

            _estado.update {
                if (it.escenario == escenario && it.ultimoResultado == null) {
                    it.copy(ultimoResultado = veredicto, evaluandoRegla = false)
                } else {
                    it
                }
            }

            // Historial veraz en Room: registramos el evento resuelto por la regla, sin afectar
            // métricas (la partida NO se modifica en rondas automatizadas).
            try {
                eventoRepo.registrarEvento(
                    EventoAtaque(
                        owner = uid,
                        ipAtacante = escenario.ipAtacante,
                        puertoDestino = escenario.puerto,
                        pais = escenario.pais,
                        isp = escenario.isp,
                        resultado = evaluacion.resultado,
                        acierto = evaluacion.acierto,
                        ocurridoEn = System.currentTimeMillis(),
                    ),
                )
            } catch (e: Exception) {
                _estado.update { it.copy(error = mensajeDeError("guardar el resultado automático", e)) }
            }
        }
    }

    /** Descarta el error actual (tras mostrarlo al usuario). */
    fun limpiarError() = _estado.update { it.copy(error = null) }

    private fun mensajeDeError(accion: String, e: Throwable): String =
        "No se pudo $accion: ${e.message ?: "error desconocido"}."

    private companion object {
        /** Aciertos repetidos del mismo patrón (familia + acción) que disparan la sugerencia. */
        const val UMBRAL_SUGERENCIA = 3

        /** Salud a la que (o por debajo de la que) la red se considera comprometida (candado E2). */
        const val SALUD_COMPROMETIDA = 0

        fun categoriaDe(esMalicioso: Boolean, bloquear: Boolean): CategoriaResultado = when {
            esMalicioso && bloquear -> CategoriaResultado.BLOQUEO_CORRECTO
            esMalicioso && !bloquear -> CategoriaResultado.BRECHA
            !esMalicioso && bloquear -> CategoriaResultado.FALSO_POSITIVO
            else -> CategoriaResultado.PERMISO_CORRECTO
        }

        /**
         * Progresión secuencial por defecto: empieza en 0 (el más simple del nivel) y avanza uno a
         * uno. Devuelve [total] (fuera de rango) al pasar del último, señal para completar el nivel.
         */
        fun progresionSecuencial(actual: Int?, total: Int): Int =
            if (actual == null) 0 else minOf(actual + 1, total)
    }
}
