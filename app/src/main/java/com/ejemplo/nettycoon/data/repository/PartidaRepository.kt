package com.ejemplo.nettycoon.data.repository

import com.ejemplo.nettycoon.data.local.dao.EstadoPartidaDao
import com.ejemplo.nettycoon.data.local.entity.EstadoPartida
import kotlinx.coroutines.flow.Flow

/**
 * Fuente única de datos del estado de partida (la "empresa virtual").
 *
 * Los ViewModel dependerán de este repositorio, nunca del DAO.
 */
class PartidaRepository(
    private val dao: EstadoPartidaDao,
) {
    /**
     * Devuelve la partida del usuario, creándola con valores iniciales si aún no existe.
     *
     * El `insertarSiNoExiste` es un IGNORE atómico: si ya había partida no la pisa, y el
     * `obtenerPorOwner` posterior devuelve la existente o la recién creada.
     */
    suspend fun getOrCreatePartida(uid: String): EstadoPartida {
        dao.insertarSiNoExiste(EstadoPartida(owner = uid))
        // No es null: acabamos de garantizar que existe una fila para este uid.
        return dao.obtenerPorOwner(uid)!!
    }

    /** Partida del usuario de forma reactiva (o `null` si aún no se ha creado). */
    fun observarPartida(uid: String): Flow<EstadoPartida?> = dao.observarPorOwner(uid)

    suspend fun actualizarPartida(estado: EstadoPartida) = dao.actualizar(estado)
}
