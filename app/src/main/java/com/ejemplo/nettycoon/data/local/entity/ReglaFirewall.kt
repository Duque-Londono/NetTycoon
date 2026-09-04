package com.ejemplo.nettycoon.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Regla de firewall definida por el jugador: qué hacer (ALLOW/DENY) con el tráfico
 * dirigido a un [puerto] y, opcionalmente, a una [ip] (o patrón de IP).
 *
 * Pertenece a un usuario concreto vía [owner] (el uid de Firebase Auth).
 */
@Entity(tableName = "regla_firewall")
data class ReglaFirewall(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** uid de Firebase del dueño de la regla. */
    @ColumnInfo(index = true)
    val owner: String,

    /** Puerto objetivo de la regla. */
    val puerto: Int,

    /** IP o patrón de IP objetivo. `null` significa "cualquier IP". */
    val ip: String? = null,

    /** Acción a aplicar sobre el tráfico que coincide. */
    val accion: AccionFirewall,

    /** Si la regla está activa (se evalúa) o no. */
    val activa: Boolean = true,

    /** Marca de tiempo de creación (epoch millis). */
    val creadaEn: Long = System.currentTimeMillis(),
)
