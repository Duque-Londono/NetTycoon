package com.ejemplo.nettycoon.ui.firewall

import com.ejemplo.nettycoon.data.local.dao.ReglaFirewallDao
import com.ejemplo.nettycoon.data.local.entity.ReglaFirewall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake en memoria de [ReglaFirewallDao] **reactivo**: mantiene el almacén en un
 * `MutableStateFlow`, de modo que insertar, actualizar o eliminar vuelve a emitir la lista.
 *
 * Es lo que distingue a este fake del de `domain/firewall/fakes`, que devuelve `flowOf(...)`
 * (una sola emisión): aquí se prueba precisamente que la UI se actualiza sola tras cada
 * operación, así que la reactividad es el comportamiento bajo prueba.
 *
 * Se fakea el DAO —no el repositorio— para poder construir el `ReglaFirewallRepository` real y
 * ejercitarlo tal cual lo usa producción, siguiendo la decisión del proyecto de no introducir
 * interfaces de repositorio.
 */
class FakeReglaFirewallDaoReactivo(
    reglasIniciales: List<ReglaFirewall> = emptyList(),
) : ReglaFirewallDao {

    private val almacen = MutableStateFlow(reglasIniciales)
    private var siguienteId = (reglasIniciales.maxOfOrNull { it.id } ?: 0L) + 1L

    /** Si es distinto de `null`, toda escritura falla con esta excepción (para probar errores). */
    var errorAlEscribir: Exception? = null

    val reglas: List<ReglaFirewall> get() = almacen.value

    override suspend fun insertar(regla: ReglaFirewall): Long {
        errorAlEscribir?.let { throw it }
        val id = siguienteId++
        almacen.value = almacen.value + regla.copy(id = id)
        return id
    }

    override suspend fun actualizar(regla: ReglaFirewall) {
        errorAlEscribir?.let { throw it }
        almacen.value = almacen.value.map { if (it.id == regla.id) regla else it }
    }

    override suspend fun eliminar(regla: ReglaFirewall) {
        errorAlEscribir?.let { throw it }
        almacen.value = almacen.value.filterNot { it.id == regla.id }
    }

    override fun observarPorOwner(owner: String): Flow<List<ReglaFirewall>> =
        almacen.map { lista -> lista.filter { it.owner == owner } }

    override fun observarActivasPorOwner(owner: String): Flow<List<ReglaFirewall>> =
        almacen.map { lista -> lista.filter { it.owner == owner && it.activa } }

    override suspend fun obtenerPorId(id: Long): ReglaFirewall? =
        almacen.value.firstOrNull { it.id == id }

    override suspend fun obtenerActivasPorOwner(owner: String): List<ReglaFirewall> =
        almacen.value.filter { it.owner == owner && it.activa }
}
