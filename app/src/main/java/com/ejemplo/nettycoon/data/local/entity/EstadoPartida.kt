package com.ejemplo.nettycoon.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Estado de la partida (la "empresa virtual") de un usuario. Hay una sola partida por
 * usuario: por eso el [owner] (uid de Firebase) es la clave primaria.
 *
 * Incluye las métricas del juego y una configuración de red mínima (IP del router y
 * un par de puertos), como campos directos: es 1:1 con la partida y no tiene ciclo de
 * vida propio, así que una tabla aparte solo añadiría un JOIN sin beneficio.
 */
@Entity(tableName = "estado_partida")
data class EstadoPartida(
    /** uid de Firebase del dueño de la partida (clave primaria: una partida por usuario). */
    @PrimaryKey
    val owner: String,

    // --- Métricas del juego ---
    val puntaje: Int = 0,
    val dineroVirtual: Int = 1000,

    /** Salud/integridad de la red (0–100). */
    val saludRed: Int = 100,
    val nivel: Int = 1,

    // --- Configuración de red básica (mínima; sin DHCP/NAT/SSID) ---
    val ipRouter: String = "192.168.0.1",
    val puertoLan: Int = 80,
    val puertoWan: Int = 443,

    /**
     * Seguro de UN SOLO USO comprado en la tienda (E3): si está activo, el PRÓXIMO golpe de salud
     * (brecha o falso positivo) se absorbe y el escudo se consume.
     *
     * Vive aquí, en Room, y no en prefs, **a propósito**: es estado de dominio de la partida y tiene
     * que moverse de forma ATÓMICA con la salud, en la misma escritura de
     * `ConsecuenciasPartida.aplicar` que aplica el daño. (El ancla de regeneración de E2 sí vive en
     * prefs, pero por una razón que aquí no aplica: ver `PreferenciasRegen`.)
     *
     * No es apilable: solo hay "tiene escudo" o "no tiene". Introducirlo obligó a la migración
     * 1→2 de [com.ejemplo.nettycoon.data.local.NetTycoonDatabase].
     */
    val escudoActivo: Boolean = false,

    /** Marca de tiempo de la última actualización (epoch millis). */
    val actualizadoEn: Long = System.currentTimeMillis(),
)
