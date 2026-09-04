package com.ejemplo.nettycoon.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ejemplo.nettycoon.data.local.entity.ReglaFirewall
import kotlinx.coroutines.flow.Flow

/**
 * DAO de [ReglaFirewall]. Lecturas reactivas (`Flow`); escrituras `suspend`.
 */
@Dao
interface ReglaFirewallDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(regla: ReglaFirewall): Long

    @Update
    suspend fun actualizar(regla: ReglaFirewall)

    @Delete
    suspend fun eliminar(regla: ReglaFirewall)

    /** Todas las reglas del usuario, más recientes primero. */
    @Query("SELECT * FROM regla_firewall WHERE owner = :owner ORDER BY creadaEn DESC")
    fun observarPorOwner(owner: String): Flow<List<ReglaFirewall>>

    /** Solo las reglas activas del usuario (las que el motor debe evaluar). */
    @Query("SELECT * FROM regla_firewall WHERE owner = :owner AND activa = 1 ORDER BY creadaEn DESC")
    fun observarActivasPorOwner(owner: String): Flow<List<ReglaFirewall>>

    /** Consulta puntual por id (usada, entre otros, por la prueba instrumentada). */
    @Query("SELECT * FROM regla_firewall WHERE id = :id")
    suspend fun obtenerPorId(id: Long): ReglaFirewall?

    /** Reglas activas del usuario en una lectura puntual (no reactiva), para el motor. */
    @Query("SELECT * FROM regla_firewall WHERE owner = :owner AND activa = 1 ORDER BY creadaEn DESC")
    suspend fun obtenerActivasPorOwner(owner: String): List<ReglaFirewall>
}
