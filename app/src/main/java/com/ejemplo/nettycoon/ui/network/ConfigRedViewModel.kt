package com.ejemplo.nettycoon.ui.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ejemplo.nettycoon.data.repository.PartidaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel de la pantalla de configuración de red. Único intermediario entre
 * [ConfigRedScreen] y la capa de datos: la pantalla solo observa [estado] y llama a estos métodos.
 *
 * **No conoce Firebase ni Auth:** recibe el [uid] ya resuelto por la navegación, igual que
 * `FirewallViewModel` y `PanelViewModel`. Tampoco conoce el DAO: habla solo con
 * [PartidaRepository].
 *
 * La carga es **puntual** (no reactiva): al abrir la pantalla se rellena el formulario una vez
 * con la partida del usuario. El resto de la partida (puntaje, dinero, salud, nivel) se conserva
 * en el estado para poder guardarlo intacto: al guardar se hace `copy(...)` de esa entidad
 * cambiando SOLO los tres campos de red.
 */
class ConfigRedViewModel(
    private val uid: String,
    private val repositorio: PartidaRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(ConfigRedUiState())
    val estado: StateFlow<ConfigRedUiState> = _estado.asStateFlow()

    init {
        cargar()
    }

    /** Carga puntual: rellena el formulario con la configuración de red guardada de la partida. */
    private fun cargar() {
        viewModelScope.launch {
            try {
                val partida = repositorio.getOrCreatePartida(uid)
                _estado.update {
                    it.copy(
                        partida = partida,
                        ipRouterTexto = partida.ipRouter,
                        puertoLanTexto = partida.puertoLan.toString(),
                        puertoWanTexto = partida.puertoWan.toString(),
                        cargando = false,
                    )
                }
            } catch (e: Exception) {
                _estado.update {
                    it.copy(cargando = false, error = mensajeDeError("cargar la configuración", e))
                }
            }
        }
    }

    // --- Formulario ---
    // Cada cambio limpia el error de formulario y la señal de "guardado con éxito", para que el
    // ✓ nunca quede mostrándose sobre datos que el usuario ya modificó.

    fun onIpRouterCambiado(valor: String) {
        _estado.update { it.copy(ipRouterTexto = valor, errorFormulario = null, guardadoConExito = false) }
    }

    /** Acepta solo dígitos y como mucho 5 (65535), para que el campo no admita basura. */
    fun onPuertoLanCambiado(valor: String) {
        val filtrado = valor.filter { it.isDigit() }.take(5)
        _estado.update { it.copy(puertoLanTexto = filtrado, errorFormulario = null, guardadoConExito = false) }
    }

    fun onPuertoWanCambiado(valor: String) {
        val filtrado = valor.filter { it.isDigit() }.take(5)
        _estado.update { it.copy(puertoWanTexto = filtrado, errorFormulario = null, guardadoConExito = false) }
    }

    /**
     * Valida el formulario y, si es correcto, guarda la configuración. La validación ocurre
     * ANTES de tocar la base de datos: si falla, solo publica [ConfigRedUiState.errorFormulario]
     * y no escribe nada.
     *
     * Al guardar se parte de la partida cargada y se hace `copy(...)` cambiando SOLO los tres
     * campos de red, de modo que las métricas del juego (puntaje, dinero, salud, nivel) se
     * conservan intactas.
     */
    fun guardar() {
        val actual = _estado.value
        // Sin partida cargada aún no hay nada que preservar: no guardamos (evita pisar métricas).
        val partida = actual.partida ?: return

        when (
            val validacion = ValidadorConfigRed.validar(
                actual.ipRouterTexto,
                actual.puertoLanTexto,
                actual.puertoWanTexto,
            )
        ) {
            is ValidacionConfigRed.Invalido -> {
                _estado.update { it.copy(errorFormulario = validacion.mensaje, guardadoConExito = false) }
            }

            is ValidacionConfigRed.Valido -> {
                val actualizada = partida.copy(
                    ipRouter = validacion.ipRouter,
                    puertoLan = validacion.puertoLan,
                    puertoWan = validacion.puertoWan,
                    actualizadoEn = System.currentTimeMillis(),
                )
                viewModelScope.launch {
                    try {
                        repositorio.actualizarPartida(actualizada)
                        _estado.update {
                            it.copy(
                                partida = actualizada,
                                errorFormulario = null,
                                error = null,
                                guardadoConExito = true,
                            )
                        }
                    } catch (e: Exception) {
                        _estado.update {
                            it.copy(error = mensajeDeError("guardar la configuración", e))
                        }
                    }
                }
            }
        }
    }

    /** Descarta el error de datos actual (tras mostrarlo al usuario). */
    fun limpiarError() = _estado.update { it.copy(error = null) }

    private fun mensajeDeError(accion: String, e: Throwable): String =
        "No se pudo $accion: ${e.message ?: "error desconocido"}."
}
