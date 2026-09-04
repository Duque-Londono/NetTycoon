package com.ejemplo.nettycoon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ejemplo.nettycoon.data.local.entity.EstadoPartida
import kotlinx.coroutines.flow.Flow

/**
 * DAO de [EstadoPartida] (una partida por usuario). Lecturas reactivas (`Flow`);
 * escrituras `suspend`.
 */
@Dao
interface EstadoPartidaDao {

    /**
     * Inserta la partida solo si el usuario aún no tiene una (IGNORE ante conflicto de
     * clave). Base del `getOrCreatePartida` del repositorio.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarSiNoExiste(estado: EstadoPartida)

    @Update
    suspend fun actualizar(estado: EstadoPartida)

    /** Consulta puntual de la partida del usuario. */
    @Query("SELECT * FROM estado_partida WHERE owner = :owner")
    suspend fun obtenerPorOwner(owner: String): EstadoPartida?

    /** Partida del usuario de forma reactiva. */
    @Query("SELECT * FROM estado_partida WHERE owner = :owner")
    fun observarPorOwner(owner: String): Flow<EstadoPartida?>
}
