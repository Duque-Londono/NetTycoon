package com.ejemplo.nettycoon.domain.model

/**
 * Datos geo-IP de dominio: lo que el juego necesita de una IP, ya desacoplado del DTO de
 * Retrofit (misma regla que aplicamos con Firebase/Room: no filtrar tipos de terceros).
 */
data class DatosGeoIp(
    val pais: String?,
    val isp: String?,
)
