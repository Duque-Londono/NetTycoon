package com.ejemplo.nettycoon.data.repository

import com.ejemplo.nettycoon.data.local.dao.EventoAtaqueDao
import com.ejemplo.nettycoon.data.local.entity.EventoAtaque
import kotlinx.coroutines.flow.Flow

/**
 * Fuente única de datos del log de ataques.
 *
 * Los ViewModel dependerán de este repositorio, nunca del DAO.
 */
class EventoAtaqueRepository(
    private val dao: EventoAtaqueDao,
) {
    /** Registra un ataque y devuelve su id generado. */
    suspend fun registrarEvento(evento: EventoAtaque): Long = dao.insertar(evento)

    /**
     * Actualiza un evento ya registrado (p. ej. para rellenar país/ISP una vez que la
     * API geo-IP respondió).
     */
    suspend fun actualizarEvento(evento: EventoAtaque) = dao.actualizar(evento)

    /** Historial de ataques del usuario. */
    fun observarEventos(owner: String): Flow<List<EventoAtaque>> =
        dao.observarPorOwner(owner)
}
