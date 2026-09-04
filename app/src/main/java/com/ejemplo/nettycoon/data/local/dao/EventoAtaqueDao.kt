package com.ejemplo.nettycoon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ejemplo.nettycoon.data.local.entity.EventoAtaque
import kotlinx.coroutines.flow.Flow

/**
 * DAO de [EventoAtaque]. Lecturas reactivas (`Flow`); escrituras `suspend`.
 */
@Dao
interface EventoAtaqueDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(evento: EventoAtaque): Long

    /** Actualiza un evento (p. ej. para rellenar país/ISP tras consultar la API geo-IP). */
    @Update
    suspend fun actualizar(evento: EventoAtaque)

    /** Historial de ataques del usuario, más recientes primero. */
    @Query("SELECT * FROM evento_ataque WHERE owner = :owner ORDER BY ocurridoEn DESC")
    fun observarPorOwner(owner: String): Flow<List<EventoAtaque>>
}
