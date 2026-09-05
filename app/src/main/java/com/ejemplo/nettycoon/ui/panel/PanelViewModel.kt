package com.ejemplo.nettycoon.ui.panel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ejemplo.nettycoon.data.repository.PartidaRepository
import com.ejemplo.nettycoon.domain.firewall.ProcesarAtaqueUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel del panel del juego (FASE 1). Único intermediario entre [PanelScreen] y la capa de
 * datos/dominio: la pantalla solo observa [estado] y llama a estos métodos.
 *
 * **No conoce Firebase ni Auth:** recibe el [uid] ya resuelto por la raíz de composición (que
 * posee la sesión), así que no duplica la lógica de sesión.
 *
 * **Política por defecto (default-DENY):** en esta fase no hay UI para crear reglas de firewall,
 * de modo que el motor evalúa cada ataque con la política por defecto (bloquear lo no permitido).
 * Es intencional; el CRUD de reglas llega en una fase posterior.
 */
class PanelViewModel(
    private val uid: String,
    private val useCase: ProcesarAtaqueUseCase,
    private val partidaRepo: PartidaRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(PanelUiState())
    val estado: StateFlow<PanelUiState> = _estado.asStateFlow()

    init {
        cargarPartida()
    }

    /** Carga (o crea) el estado de partida inicial del usuario. */
    private fun cargarPartida() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val partida = partidaRepo.getOrCreatePartida(uid)
                _estado.update { it.copy(partida = partida, cargando = false) }
            } catch (e: Exception) {
                _estado.update {
                    it.copy(
                        cargando = false,
                        error = "No se pudo cargar la partida: ${e.message ?: "error desconocido"}.",
                    )
                }
            }
        }
    }

    /**
     * Ejecuta una ronda: genera un ataque, lo enriquece con geo-IP (best-effort dentro del
     * caso de uso), lo evalúa con la política por defecto y persiste el resultado. Modela
     * Cargando → Éxito/Error. Si ya hay una ronda en curso, no hace nada.
     */
    fun simularAtaque() {
        if (_estado.value.cargando) return
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val ronda = useCase.ejecutarRonda(uid)
                _estado.update {
                    it.copy(
                        ultimaRonda = ronda,
                        partida = ronda.estadoPartida,
                        cargando = false,
                    )
                }
            } catch (e: Exception) {
                _estado.update {
                    it.copy(
                        cargando = false,
                        error = "No se pudo simular el ataque: ${e.message ?: "error desconocido"}.",
                    )
                }
            }
        }
    }

    /** Descarta el error actual (tras mostrarlo al usuario). */
    fun limpiarError() = _estado.update { it.copy(error = null) }
}
