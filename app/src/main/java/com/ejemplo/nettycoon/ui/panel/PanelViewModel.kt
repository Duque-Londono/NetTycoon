package com.ejemplo.nettycoon.ui.panel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ejemplo.nettycoon.data.repository.PartidaRepository
import com.ejemplo.nettycoon.data.repository.RegeneradorSalud
import com.ejemplo.nettycoon.domain.firewall.ProcesarAtaqueUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel del panel del juego (FASE 1). Único intermediario entre [PanelScreen] y la capa de
 * datos/dominio: la pantalla solo observa [estado] y llama a estos métodos.
 *
 * **No conoce Firebase ni Auth:** recibe el [uid] ya resuelto por la raíz de composición (que
 * posee la sesión), así que no duplica la lógica de sesión.
 *
 * **Reglas del jugador:** el caso de uso lee en cada ronda las reglas activas del `uid`
 * (`ReglaFirewallRepository.obtenerReglasActivas`), así que lo que el jugador cree en la pantalla
 * de reglas cambia de inmediato el resultado de [simularAtaque]. Si aún no hay ninguna regla
 * activa, el motor aplica la política por defecto (DENY, el estándar seguro de firewall).
 */
class PanelViewModel(
    private val uid: String,
    private val useCase: ProcesarAtaqueUseCase,
    private val partidaRepo: PartidaRepository,
    private val regenerador: RegeneradorSalud,
) : ViewModel() {

    private val _estado = MutableStateFlow(PanelUiState())
    val estado: StateFlow<PanelUiState> = _estado.asStateFlow()

    init {
        cargarPartida()
    }

    /**
     * Observa el estado de partida del usuario de forma REACTIVA, de modo que el Panel refleje al
     * instante los cambios que otras pantallas (p. ej. "Ataque en vivo") persisten en la misma fila
     * de Room.
     *
     * Antes de observar, aplica la regeneración de salud por tiempo real (E2): si corresponde,
     * `regenerador.aplicar` persiste la salud al día en Room, y como el estado visible viene del
     * Flow, la subida se refleja en la primera emisión sin lógica extra aquí. También GARANTIZA que
     * exista la fila; a propósito no publica su resultado en `_estado` para no competir con la
     * primera emisión del [PartidaRepository.observarPartida]. El estado visible viene siempre del
     * Flow, y es esa primera emisión la que baja `cargando`.
     */
    private fun cargarPartida() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                // Regenera (si toca) y asegura la fila; NO se publica (lo hace el Flow).
                regenerador.aplicar(uid)
                _estado.update { it.copy(anclaRegen = regenerador.anclaActual(uid)) }
            } catch (e: Exception) {
                _estado.update {
                    it.copy(
                        cargando = false,
                        error = "No se pudo cargar la partida: ${e.message ?: "error desconocido"}.",
                    )
                }
                return@launch
            }
            partidaRepo.observarPartida(uid)
                .catch { e ->
                    _estado.update {
                        it.copy(
                            cargando = false,
                            error = "No se pudo cargar la partida: ${e.message ?: "error desconocido"}.",
                        )
                    }
                }
                .collect { partida ->
                    _estado.update { it.copy(partida = partida, cargando = false) }
                }
        }
    }

    /**
     * Ejecuta una ronda: genera un ataque, lo enriquece con geo-IP (best-effort dentro del
     * caso de uso), lo evalúa con las reglas activas del jugador (o la política por defecto si
     * no tiene ninguna) y persiste el resultado. Modela
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

    /**
     * Reaplica la regeneración por tiempo real (E2.1): cuando el contador de la UI cruza un tramo,
     * dispara esto para que la salud suba a la vista sin salir de la pantalla. Sigue la MISMA ruta
     * que E2 (`aplicar` → persiste en Room → el `observarPartida` reactivo re-emite la salud); aquí
     * solo se actualiza además el ancla para reiniciar el contador. La vida sale siempre de la regen
     * sobre el ancla real, no del contador. Ignora llamadas concurrentes.
     */
    fun refrescarRegen() {
        if (refrescandoRegen) return
        refrescandoRegen = true
        viewModelScope.launch {
            try {
                regenerador.aplicar(uid)
                _estado.update { it.copy(anclaRegen = regenerador.anclaActual(uid)) }
            } catch (_: Exception) {
                // Silencioso: es un refresco de fondo; la salud se recalculará al reentrar.
            } finally {
                refrescandoRegen = false
            }
        }
    }

    /** Descarta el error actual (tras mostrarlo al usuario). */
    fun limpiarError() = _estado.update { it.copy(error = null) }

    /** Guard anti-reentrada para [refrescarRegen] (evita reaplicar en paralelo por ticks seguidos). */
    private var refrescandoRegen = false
}
