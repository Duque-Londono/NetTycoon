package com.ejemplo.nettycoon.data.repository

import com.ejemplo.nettycoon.data.local.dao.ReglaFirewallDao
import com.ejemplo.nettycoon.data.local.entity.ReglaFirewall
import kotlinx.coroutines.flow.Flow

/**
 * Fuente única de datos de reglas de firewall para las capas superiores.
 *
 * Expone directamente la entidad [ReglaFirewall] (es un tipo nuestro, no de terceros).
 * Los ViewModel dependerán de este repositorio, nunca del DAO.
 */
class ReglaFirewallRepository(
    private val dao: ReglaFirewallDao,
) {
    /** Todas las reglas del usuario. */
    fun observarReglas(owner: String): Flow<List<ReglaFirewall>> =
        dao.observarPorOwner(owner)

    /** Solo las reglas activas del usuario. */
    fun observarReglasActivas(owner: String): Flow<List<ReglaFirewall>> =
        dao.observarActivasPorOwner(owner)

    /** Crea/guarda una regla y devuelve su id generado. */
    suspend fun guardarRegla(regla: ReglaFirewall): Long = dao.insertar(regla)

    suspend fun actualizarRegla(regla: ReglaFirewall) = dao.actualizar(regla)

    suspend fun eliminarRegla(regla: ReglaFirewall) = dao.eliminar(regla)

    suspend fun obtenerRegla(id: Long): ReglaFirewall? = dao.obtenerPorId(id)
}
