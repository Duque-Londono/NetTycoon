package com.ejemplo.nettycoon.ui.estadisticas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ejemplo.nettycoon.data.local.entity.EstadoPartida
import com.ejemplo.nettycoon.data.local.entity.EventoAtaque
import com.ejemplo.nettycoon.data.repository.EventoAtaqueRepository
import com.ejemplo.nettycoon.data.repository.PartidaRepository
import com.ejemplo.nettycoon.domain.firewall.MapeoFamilias
import com.ejemplo.nettycoon.domain.firewall.puntajeParaSiguienteRango
import com.ejemplo.nettycoon.domain.firewall.rangoPorPuntaje
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel de la pantalla "Mi progreso". Único intermediario entre [EstadisticasScreen] y la
 * capa de datos: la pantalla solo observa [estado].
 *
 * **No conoce Firebase ni Auth:** recibe el [uid] ya resuelto por la navegación, igual que
 * `ConfigRedViewModel`/`FirewallViewModel`. Tampoco conoce el DAO: habla solo con
 * [EventoAtaqueRepository], y el consumo del historial es **de solo lectura**.
 *
 * Observa reactivamente el historial de `EventoAtaque` del usuario y deriva en memoria las tres
 * métricas: total de ataques, tasa de acierto y familias "dominadas"/"flojas". La agrupación por
 * familia se hace en la capa de presentación (ver [MapeoFamilias]); no se persiste nada nuevo ni
 * se toca el esquema de Room.
 */
class EstadisticasViewModel(
    private val uid: String,
    private val repositorio: EventoAtaqueRepository,
    private val partidaRepo: PartidaRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadisticasUiState())
    val estado: StateFlow<EstadisticasUiState> = _estado.asStateFlow()

    init {
        observarHistorial()
    }

    /**
     * Observa a la vez el historial de ataques y la partida, y deriva de ambos el estado de la
     * pantalla.
     *
     * **Usuario nuevo (sin eventos y sin partida):** los dos Flow de Room emiten de inmediato
     * (lista vacía y null respectivamente), así que el combine produce su primera emisión sin
     * esperar a nada y la pantalla NO se queda en "cargando" para siempre. Esa emisión baja
     * cargando, deja el desempeño en cero y da rango APRENDIZ (puntaje 0).
     *
     * A propósito **no** se usa getOrCreatePartida: "Mi progreso" es una pantalla de solo lectura
     * y no debe crear filas por el hecho de visitarla. Una partida ausente se interpreta como
     * puntaje 0, que es justo lo que sería.
     */
    private fun observarHistorial() {
        viewModelScope.launch {
            combine(
                repositorio.observarEventos(uid),
                partidaRepo.observarPartida(uid),
            ) { eventos, partida ->
                // Defensa en profundidad: el DAO ya filtra por owner, pero recalcamos aquí
                // para no depender de esa garantía externa.
                calcular(eventos.filter { it.owner == uid }, partida)
            }
                .catch { e ->
                    _estado.value = EstadisticasUiState(
                        cargando = false,
                        error = "No se pudo cargar tu progreso: ${e.message ?: "error desconocido"}.",
                    )
                }
                .collect { estadoNuevo -> _estado.value = estadoNuevo }
        }
    }

    private fun calcular(
        eventos: List<EventoAtaque>,
        partida: EstadoPartida?,
    ): EstadisticasUiState {
        // El rango se deriva del puntaje y es INDEPENDIENTE del historial: se calcula primero para
        // poder devolverlo también en el caso vacío (usuario nuevo).
        val puntaje = partida?.puntaje ?: 0
        val rango = rangoPorPuntaje(puntaje)
        val faltan = puntajeParaSiguienteRango(puntaje)

        val total = eventos.size
        if (total == 0) {
            return EstadisticasUiState(
                cargando = false,
                totalAtaques = 0,
                rango = rango,
                puntaje = puntaje,
                puntosParaSiguienteRango = faltan,
            )
        }

        val aciertos = eventos.count { it.acierto }
        val tasa = porcentaje(aciertos, total)

        val familias = eventos
            .groupBy { MapeoFamilias.familiaDe(it.puertoDestino) }
            .map { (nombre, lista) ->
                val totalFam = lista.size
                val aciertosFam = lista.count { it.acierto }
                val pctFam = porcentaje(aciertosFam, totalFam)
                FamiliaResumen(
                    nombre = nombre,
                    total = totalFam,
                    aciertos = aciertosFam,
                    aciertoPct = pctFam,
                    nivel = nivelDe(pctFam),
                )
            }
            // Mayor a menor tasa de acierto; a igualdad, la de más ataques primero.
            .sortedWith(compareByDescending<FamiliaResumen> { it.aciertoPct }.thenByDescending { it.total })

        return EstadisticasUiState(
            cargando = false,
            totalAtaques = total,
            aciertos = aciertos,
            tasaAciertoPct = tasa,
            familias = familias,
            rango = rango,
            puntaje = puntaje,
            puntosParaSiguienteRango = faltan,
            error = null,
        )
    }

    private fun nivelDe(pct: Int): NivelDominio = when {
        pct >= UMBRAL_DOMINADA -> NivelDominio.DOMINADA
        pct < UMBRAL_FLOJA -> NivelDominio.FLOJA
        else -> NivelDominio.NEUTRA
    }

    /** Porcentaje entero [0, 100] con redondeo; `0` si el total es 0 (evita división por cero). */
    private fun porcentaje(parte: Int, total: Int): Int =
        if (total == 0) 0 else Math.round(parte * 100.0 / total).toInt()

    companion object {
        /** Umbral (%) a partir del cual una familia se considera "dominada". PROVISIONAL. */
        const val UMBRAL_DOMINADA = 70

        /** Umbral (%) por debajo del cual una familia se considera "floja". PROVISIONAL. */
        const val UMBRAL_FLOJA = 50
    }
}
