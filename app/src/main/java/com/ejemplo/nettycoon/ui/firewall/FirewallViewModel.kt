package com.ejemplo.nettycoon.ui.firewall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ejemplo.nettycoon.data.local.entity.AccionFirewall
import com.ejemplo.nettycoon.data.local.entity.ReglaFirewall
import com.ejemplo.nettycoon.data.repository.PartidaRepository
import com.ejemplo.nettycoon.data.repository.ReglaFirewallRepository
import com.ejemplo.nettycoon.domain.firewall.cupoDeReglas
import com.ejemplo.nettycoon.domain.firewall.rangoPorPuntaje
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel de la pantalla de reglas de firewall (CRUD). Único intermediario entre
 * [FirewallScreen] y la capa de datos: la pantalla solo observa [estado] y llama a estos métodos.
 *
 * **No conoce Firebase ni Auth:** recibe el [uid] ya resuelto por la navegación, igual que
 * `PanelViewModel`. Tampoco conoce el DAO: habla solo con [ReglaFirewallRepository].
 *
 * La lista de reglas se observa de forma **reactiva** desde Room, así que crear, activar,
 * desactivar o eliminar una regla se refleja en la UI sin recargar nada a mano.
 *
 * Estas reglas son las mismas que `ProcesarAtaqueUseCase` lee en cada ronda
 * (`obtenerReglasActivas`), de modo que lo que el jugador defina aquí cambia de inmediato el
 * resultado de "Simular ataque" en el panel: el motor deja de operar solo con default-DENY.
 */
class FirewallViewModel(
    private val uid: String,
    private val repositorio: ReglaFirewallRepository,
    private val partidaRepo: PartidaRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(FirewallUiState())
    val estado: StateFlow<FirewallUiState> = _estado.asStateFlow()

    init {
        observarReglas()
    }

    /**
     * Suscribe la UI a las reglas del usuario y a su partida (lecturas reactivas, no puntuales).
     *
     * La partida entra porque el CUPO de reglas activas (E5) se deriva del rango, que a su vez se
     * deriva del puntaje: así, en cuanto el jugador sube o baja de rango, el cupo de esta pantalla
     * se actualiza solo. Es la misma vía que se usó en "Mi progreso", y esta pantalla tampoco crea
     * la partida (nada de getOrCreatePartida): una partida ausente se lee como puntaje 0.
     *
     * Los dos Flow de Room emiten de inmediato (lista vacia y null), asi que el combine produce su
     * primera emisión sin esperar a nada y la pantalla no se queda cargando para siempre.
     */
    private fun observarReglas() {
        viewModelScope.launch {
            combine(
                repositorio.observarReglas(uid),
                partidaRepo.observarPartida(uid),
            ) { lista, partida ->
                val rango = rangoPorPuntaje(partida?.puntaje ?: 0)
                Triple(lista, rango, cupoDeReglas(rango))
            }
                .catch { e ->
                    _estado.update {
                        it.copy(cargando = false, error = mensajeDeError("cargar tus reglas", e))
                    }
                }
                .collect { (lista, rango, cupo) ->
                    _estado.update {
                        it.copy(reglas = lista, rango = rango, cupo = cupo, cargando = false)
                    }
                }
        }
    }

    // --- Formulario ---

    /** Acepta solo dígitos y como mucho 5 (65535), para que el campo no admita basura. */
    fun onPuertoCambiado(valor: String) {
        val filtrado = valor.filter { it.isDigit() }.take(5)
        _estado.update { it.copy(puertoTexto = filtrado, errorFormulario = null) }
    }

    fun onIpCambiada(valor: String) {
        _estado.update { it.copy(ipTexto = valor, errorFormulario = null) }
    }

    fun onAccionCambiada(accion: AccionFirewall) {
        _estado.update { it.copy(accion = accion, errorFormulario = null) }
    }

    // --- Operaciones CRUD ---

    /**
     * Valida el formulario y, si es correcto, guarda la regla. Al terminar limpia el formulario
     * (la lista se actualiza sola por la observación reactiva). Si la validación falla, no toca
     * la base de datos y solo publica [FirewallUiState.errorFormulario].
     */
    fun crearRegla() {
        val actual = _estado.value

        // Compuerta del CUPO (E5): se comprueba ANTES de validar el formulario para que el mensaje
        // que ve el jugador sea el del cupo y no un error de puerto. No se escribe nada en Room.
        if (!actual.puedeCrear) {
            _estado.update { it.copy(errorFormulario = mensajeCupo(actual)) }
            return
        }

        when (val validacion = ValidadorRegla.validar(actual.puertoTexto, actual.ipTexto)) {
            is ValidacionRegla.Invalido -> {
                _estado.update { it.copy(errorFormulario = validacion.mensaje) }
            }

            is ValidacionRegla.Valido -> {
                val nueva = ReglaFirewall(
                    owner = uid,
                    puerto = validacion.puerto,
                    ip = validacion.ip,
                    accion = actual.accion,
                )
                viewModelScope.launch {
                    try {
                        repositorio.guardarRegla(nueva)
                        _estado.update {
                            it.copy(puertoTexto = "", ipTexto = "", errorFormulario = null)
                        }
                    } catch (e: Exception) {
                        _estado.update { it.copy(error = mensajeDeError("guardar la regla", e)) }
                    }
                }
            }
        }
    }

    /**
     * Activa o desactiva una regla. Una regla inactiva se conserva pero el motor la ignora.
     *
     * **Compuerta del cupo (E5), solo al ACTIVAR.** Cierra un agujero real: con cupo 3 se podrían
     * crear 3 reglas, desactivar una, crear otra (3 activas / 4 creadas) y después reactivar la
     * guardada, quedando 4 activas. Bloquear la reactivación en el tope mantiene el invariante.
     *
     * **DESACTIVAR nunca se bloquea**: es justamente la salida cuando se está sobre el cupo.
     */
    fun alternarActiva(regla: ReglaFirewall) {
        val actual = _estado.value
        val vaAActivar = !regla.activa
        if (vaAActivar && !actual.puedeCrear) {
            _estado.update { it.copy(error = mensajeCupo(actual, activando = true)) }
            return
        }
        viewModelScope.launch {
            try {
                repositorio.actualizarRegla(regla.copy(activa = !regla.activa))
            } catch (e: Exception) {
                _estado.update { it.copy(error = mensajeDeError("actualizar la regla", e)) }
            }
        }
    }

    fun eliminarRegla(regla: ReglaFirewall) {
        viewModelScope.launch {
            try {
                repositorio.eliminarRegla(regla)
            } catch (e: Exception) {
                _estado.update { it.copy(error = mensajeDeError("eliminar la regla", e)) }
            }
        }
    }

    /** Descarta el error de datos actual (tras mostrarlo al usuario). */
    fun limpiarError() = _estado.update { it.copy(error = null) }

    /**
     * Mensaje del cupo, redactado para que NO se lea como un error ni como un castigo: explica por
     * qué no se puede y cómo salir. Distingue los dos casos:
     *
     * - **En el tope:** el jugador llegó a su límite; la salida es subir de rango o liberar cupo.
     * - **Sobre el cupo:** bajó de rango (un falso positivo resta puntaje) y se quedó con más
     *   reglas activas de las que ahora le corresponden. No se le ha tocado ni una sola regla; solo
     *   no puede crear ni activar más hasta desactivar alguna.
     */
    private fun mensajeCupo(estado: FirewallUiState, activando: Boolean = false): String {
        val verbo = if (activando) "activar" else "crear"
        return if (estado.sobreCupo) {
            "Bajaste de rango, así que tu cupo ahora es de ${estado.cupo} reglas activas y tienes " +
                "${estado.reglasActivas}. No te hemos quitado ninguna: siguen todas ahí. Para " +
                "$verbo una, desactiva antes otra con su interruptor."
        } else {
            "Alcanzaste tu cupo de ${estado.cupo} reglas activas. Desactiva una para $verbo otra, " +
                "o sube de rango para automatizar más."
        }
    }

    private fun mensajeDeError(accion: String, e: Throwable): String =
        "No se pudo $accion: ${e.message ?: "error desconocido"}."
}
