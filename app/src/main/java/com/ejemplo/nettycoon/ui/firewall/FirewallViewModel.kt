package com.ejemplo.nettycoon.ui.firewall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ejemplo.nettycoon.data.local.entity.AccionFirewall
import com.ejemplo.nettycoon.data.local.entity.ReglaFirewall
import com.ejemplo.nettycoon.data.repository.ReglaFirewallRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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
) : ViewModel() {

    private val _estado = MutableStateFlow(FirewallUiState())
    val estado: StateFlow<FirewallUiState> = _estado.asStateFlow()

    init {
        observarReglas()
    }

    /** Suscribe la UI a las reglas del usuario en Room (lectura reactiva, no puntual). */
    private fun observarReglas() {
        viewModelScope.launch {
            repositorio.observarReglas(uid)
                .catch { e ->
                    _estado.update {
                        it.copy(cargando = false, error = mensajeDeError("cargar tus reglas", e))
                    }
                }
                .collect { lista ->
                    _estado.update { it.copy(reglas = lista, cargando = false) }
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

    /** Activa o desactiva una regla. Una regla inactiva se conserva pero el motor la ignora. */
    fun alternarActiva(regla: ReglaFirewall) {
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

    private fun mensajeDeError(accion: String, e: Throwable): String =
        "No se pudo $accion: ${e.message ?: "error desconocido"}."
}
