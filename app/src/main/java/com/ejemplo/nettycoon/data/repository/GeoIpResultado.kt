package com.ejemplo.nettycoon.data.repository

import com.ejemplo.nettycoon.domain.model.DatosGeoIp

/**
 * Resultado one-shot de una consulta geo-IP. Análogo a `AuthResultado` (mismo patrón), pero
 * propio de este dominio. No incluye `Cargando`: el estado de carga lo maneja el ViewModel
 * en la fase de integración (M4-B).
 */
sealed interface GeoIpResultado {
    data class Exito(val datos: DatosGeoIp) : GeoIpResultado
    data class Error(val mensaje: String, val causa: Throwable? = null) : GeoIpResultado
}
