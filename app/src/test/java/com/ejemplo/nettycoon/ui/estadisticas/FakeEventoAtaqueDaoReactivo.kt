package com.ejemplo.nettycoon.ui.estadisticas

import com.ejemplo.nettycoon.data.local.dao.EventoAtaqueDao
import com.ejemplo.nettycoon.data.local.entity.EventoAtaque
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake en memoria de [EventoAtaqueDao] **reactivo**: mantiene el almacén en un `MutableStateFlow`,
 * de modo que insertar o actualizar vuelve a emitir la lista y la UI observa el cambio.
 *
 * Se fakea el DAO —no el repositorio— para poder construir el `EventoAtaqueRepository` real y
 * ejercitarlo tal cual lo usa producción, siguiendo la decisión del proyecto de no introducir
 * interfaces de repositorio (mismo enfoque que `FakeReglaFirewallDaoReactivo`).
 */
class FakeEventoAtaqueDaoReactivo(
    eventosIniciales: List<EventoAtaque> = emptyList(),
) : EventoAtaqueDao {

    private val almacen = MutableStateFlow(eventosIniciales)
    private var siguienteId = (eventosIniciales.maxOfOrNull { it.id } ?: 0L) + 1L

    /** Si es distinto de `null`, `observarPorOwner` emite un Flow que falla (para probar errores). */
    var errorAlObservar: Exception? = null

    override suspend fun insertar(evento: EventoAtaque): Long {
        val id = siguienteId++
        almacen.value = almacen.value + evento.copy(id = id)
        return id
    }

    override suspend fun actualizar(evento: EventoAtaque) {
        almacen.value = almacen.value.map { if (it.id == evento.id) evento else it }
    }

    override fun observarPorOwner(owner: String): Flow<List<EventoAtaque>> =
        almacen.map { lista ->
            errorAlObservar?.let { throw it }
            lista.filter { it.owner == owner }.sortedByDescending { it.ocurridoEn }
        }
}
