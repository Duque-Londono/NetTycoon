package com.ejemplo.nettycoon.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Registro (log) de un ataque simulado que llegó a la red del jugador.
 *
 * Los campos [pais] e [isp] son nullables porque los rellenará más adelante la API
 * de geo-IP (ipwho.is); al registrarse el evento pueden aún no conocerse.
 *
 * Pertenece a un usuario concreto vía [owner] (el uid de Firebase Auth).
 */
@Entity(tableName = "evento_ataque")
data class EventoAtaque(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** uid de Firebase del dueño del evento. */
    @ColumnInfo(index = true)
    val owner: String,

    /** IP del atacante. */
    val ipAtacante: String,

    /** Puerto atacado (si se conoce). */
    val puertoDestino: Int? = null,

    /** País de la IP atacante; lo rellena la API geo-IP. */
    val pais: String? = null,

    /** ISP de la IP atacante; lo rellena la API geo-IP. */
    val isp: String? = null,

    /** Cómo se resolvió el tráfico: bloqueado o permitido. */
    val resultado: ResultadoEvento,

    /** Si la decisión del jugador ante este ataque fue correcta. */
    val acierto: Boolean,

    /** Marca de tiempo del ataque (epoch millis). */
    val ocurridoEn: Long = System.currentTimeMillis(),
)
