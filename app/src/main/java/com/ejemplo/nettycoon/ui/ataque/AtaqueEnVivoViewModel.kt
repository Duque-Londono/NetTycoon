package com.ejemplo.nettycoon.ui.ataque

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ejemplo.nettycoon.data.local.entity.AccionFirewall
import com.ejemplo.nettycoon.data.local.entity.EstadoPartida
import com.ejemplo.nettycoon.data.local.entity.EventoAtaque
import com.ejemplo.nettycoon.data.local.entity.ResultadoEvento
import com.ejemplo.nettycoon.data.repository.EventoAtaqueRepository
import com.ejemplo.nettycoon.data.repository.PartidaRepository
import com.ejemplo.nettycoon.domain.firewall.ConsecuenciasPartida
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
 * El [seleccionarSiguiente] elige el índice del próximo escenario a partir del actual (o `null` en
 * el arranque). Es inyectable para que los tests sean deterministas; por defecto es aleatorio sin
 * repetir el escenario inmediatamente anterior.
 */
class AtaqueEnVivoViewModel(
    private val uid: String,
    private val partidaRepo: PartidaRepository,
    private val eventoRepo: EventoAtaqueRepository,
    private val escenarios: List<EscenarioAtaque> = CatalogoAtaques.escenarios,
    private val seleccionarSiguiente: (actual: Int?) -> Int = { actual ->
        indiceAleatorioDistinto(escenarios.size, actual)
    },
) : ViewModel() {

    private var indiceActual: Int = seleccionarSiguiente(null)

    /**
     * Contador EN MEMORIA (de sesión, no se persiste) de cuántas veces el jugador ha ACERTADO la
     * misma decisión sobre el mismo puerto. Solo se cuentan aciertos: no queremos sugerir
     * automatizar un error. Se pierde al salir de la pantalla (el ViewModel se destruye).
     */
    private val aciertosPorPatron = mutableMapOf<Pair<Int, AccionFirewall>, Int>()

    /** Patrones para los que ya se mostró la sugerencia, para no repetirla en la sesión. */
    private val patronesYaSugeridos = mutableSetOf<Pair<Int, AccionFirewall>>()

    private val _estado = MutableStateFlow(
        AtaqueEnVivoUiState(escenario = escenarios[indiceActual]),
    )
    val estado: StateFlow<AtaqueEnVivoUiState> = _estado.asStateFlow()

    init {
        cargarPartida()
    }

    /** Carga (o crea) la partida del usuario para mostrar métricas y aplicarles consecuencias. */
    private fun cargarPartida() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val partida = partidaRepo.getOrCreatePartida(uid)
                _estado.update { it.copy(partida = partida, cargando = false) }
            } catch (e: Exception) {
                _estado.update {
                    it.copy(cargando = false, error = mensajeDeError("cargar la partida", e))
                }
            }
        }
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
        val escenario = actual.escenario

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
        _estado.update {
            it.copy(
                partida = actualizada,
                ultimoResultado = veredicto,
                aciertos = it.aciertos + if (acierto) 1 else 0,
                rondas = it.rondas + 1,
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
     * patrón (puerto + acción) y, al alcanzar [UMBRAL_SUGERENCIA] por primera vez, devuelve una
     * [SugerenciaRegla] explicativa. Devuelve `null` si la decisión fue un error (no se cuenta) o
     * si el patrón aún no llega al umbral o ya se sugirió antes en esta sesión.
     */
    private fun calcularSugerencia(
        escenario: EscenarioAtaque,
        accion: AccionFirewall,
        acierto: Boolean,
    ): SugerenciaRegla? {
        if (!acierto) return null

        val patron = escenario.puerto to accion
        val conteo = (aciertosPorPatron[patron] ?: 0) + 1
        aciertosPorPatron[patron] = conteo

        if (conteo < UMBRAL_SUGERENCIA || patron in patronesYaSugeridos) return null
        patronesYaSugeridos.add(patron)

        val bloquear = accion == AccionFirewall.DENY
        val verbo = if (bloquear) "bloqueado" else "permitido"
        val accionTexto = if (bloquear) "Bloquear" else "Permitir"
        val texto = "Has $verbo el puerto ${escenario.puerto} (${escenario.servicioNombre}) " +
            "$conteo veces y siempre acertaste. Cuando reconoces un patrón, puedes crear una " +
            "REGLA para que el firewall lo haga solo, sin que tengas que decidirlo cada vez. " +
            "Ve a 'Mis reglas' y crea una regla: puerto ${escenario.puerto}, acción $accionTexto."

        return SugerenciaRegla(
            puerto = escenario.puerto,
            servicio = escenario.servicioNombre,
            accionTexto = accionTexto,
            texto = texto,
        )
    }

    /** Trae el siguiente escenario y limpia el veredicto para volver a la fase de decisión. */
    fun onSiguienteAtaque() {
        indiceActual = seleccionarSiguiente(indiceActual)
        _estado.update {
            it.copy(escenario = escenarios[indiceActual], ultimoResultado = null)
        }
    }

    /** Descarta el error actual (tras mostrarlo al usuario). */
    fun limpiarError() = _estado.update { it.copy(error = null) }

    private fun mensajeDeError(accion: String, e: Throwable): String =
        "No se pudo $accion: ${e.message ?: "error desconocido"}."

    private companion object {
        /** Aciertos repetidos del mismo patrón (puerto + acción) que disparan la sugerencia. */
        const val UMBRAL_SUGERENCIA = 3

        fun categoriaDe(esMalicioso: Boolean, bloquear: Boolean): CategoriaResultado = when {
            esMalicioso && bloquear -> CategoriaResultado.BLOQUEO_CORRECTO
            esMalicioso && !bloquear -> CategoriaResultado.BRECHA
            !esMalicioso && bloquear -> CategoriaResultado.FALSO_POSITIVO
            else -> CategoriaResultado.PERMISO_CORRECTO
        }

        /** Índice al azar en [0, tamano) distinto de [actual] cuando hay más de un escenario. */
        fun indiceAleatorioDistinto(tamano: Int, actual: Int?): Int {
            if (tamano <= 1) return 0
            var siguiente: Int
            do {
                siguiente = (0 until tamano).random()
            } while (siguiente == actual)
            return siguiente
        }
    }
}
