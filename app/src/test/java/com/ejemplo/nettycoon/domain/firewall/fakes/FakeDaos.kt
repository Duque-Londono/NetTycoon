package com.ejemplo.nettycoon.domain.firewall.fakes

import com.ejemplo.nettycoon.data.local.dao.EstadoPartidaDao
import com.ejemplo.nettycoon.data.local.dao.EventoAtaqueDao
import com.ejemplo.nettycoon.data.local.dao.ReglaFirewallDao
import com.ejemplo.nettycoon.data.local.entity.EstadoPartida
import com.ejemplo.nettycoon.data.local.entity.EventoAtaque
import com.ejemplo.nettycoon.data.local.entity.ReglaFirewall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Fakes en memoria de los DAOs de Room (que son interfaces). Permiten construir los
 * repositorios reales para probar el caso de uso en JVM sin tocar la API pública de M2 ni
 * necesitar un emulador.
 *
 * Solo se implementa el comportamiento que el caso de uso ejercita; los métodos reactivos
 * (`Flow`) devuelven algo trivial porque no se usan aquí.
 */

class FakeReglaFirewallDao(reglas: List<ReglaFirewall> = emptyList()) : ReglaFirewallDao {
    private val almacen = reglas.toMutableList()
    private var siguienteId = 1L

    override suspend fun insertar(regla: ReglaFirewall): Long {
        val id = siguienteId++
        almacen.add(regla.copy(id = id))
        return id
    }

    override suspend fun actualizar(regla: ReglaFirewall) {
        val i = almacen.indexOfFirst { it.id == regla.id }
        if (i >= 0) almacen[i] = regla
    }

    override suspend fun eliminar(regla: ReglaFirewall) {
        almacen.removeAll { it.id == regla.id }
    }

    override fun observarPorOwner(owner: String): Flow<List<ReglaFirewall>> =
        flowOf(almacen.filter { it.owner == owner })

    override fun observarActivasPorOwner(owner: String): Flow<List<ReglaFirewall>> =
        flowOf(almacen.filter { it.owner == owner && it.activa })

    override suspend fun obtenerPorId(id: Long): ReglaFirewall? =
        almacen.firstOrNull { it.id == id }

    override suspend fun obtenerActivasPorOwner(owner: String): List<ReglaFirewall> =
        almacen.filter { it.owner == owner && it.activa }
}

class FakeEventoAtaqueDao : EventoAtaqueDao {
    val insertados = mutableListOf<EventoAtaque>()
    private var siguienteId = 1L

    override suspend fun insertar(evento: EventoAtaque): Long {
        val id = siguienteId++
        insertados.add(evento.copy(id = id))
        return id
    }

    override suspend fun actualizar(evento: EventoAtaque) {
        val i = insertados.indexOfFirst { it.id == evento.id }
        if (i >= 0) insertados[i] = evento
    }

    override fun observarPorOwner(owner: String): Flow<List<EventoAtaque>> =
        flowOf(insertados.filter { it.owner == owner })
}

class FakeEstadoPartidaDao : EstadoPartidaDao {
    val almacen = mutableMapOf<String, EstadoPartida>()

    override suspend fun insertarSiNoExiste(estado: EstadoPartida) {
        // Semántica IGNORE: no pisa si ya existe.
        almacen.putIfAbsent(estado.owner, estado)
    }

    override suspend fun actualizar(estado: EstadoPartida) {
        almacen[estado.owner] = estado
    }

    override suspend fun obtenerPorOwner(owner: String): EstadoPartida? = almacen[owner]

    override fun observarPorOwner(owner: String): Flow<EstadoPartida?> =
        flowOf(almacen[owner])
}
