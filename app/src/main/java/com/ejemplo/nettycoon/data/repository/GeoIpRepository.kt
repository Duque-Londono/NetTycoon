package com.ejemplo.nettycoon.data.repository

import com.ejemplo.nettycoon.data.remote.GeoIpApiService
import com.ejemplo.nettycoon.data.remote.RetrofitProvider
import com.ejemplo.nettycoon.domain.model.DatosGeoIp
import retrofit2.HttpException
import java.io.IOException

/**
 * Fuente única de datos geo-IP. Envuelve al [GeoIpApiService] y traduce cada caso a un
 * [GeoIpResultado], sin filtrar el DTO de Retrofit hacia afuera.
 *
 * Casos contemplados:
 * - Excepción de red ([IOException]) → [GeoIpResultado.Error].
 * - HTTP no-2xx ([HttpException]) → Error con el código.
 * - HTTP 200 con `success=false` → Error con el `message` de ipwho.is.
 * - `success=true` → [GeoIpResultado.Exito] con [DatosGeoIp].
 */
class GeoIpRepository(
    private val api: GeoIpApiService = RetrofitProvider.geoIpApiService,
) {
    suspend fun consultar(ip: String): GeoIpResultado =
        try {
            val dto = api.consultarIp(ip)
            if (dto.success) {
                GeoIpResultado.Exito(DatosGeoIp(pais = dto.country, isp = dto.connection?.isp))
            } else {
                GeoIpResultado.Error(
                    dto.message ?: "No se pudo obtener la información de la IP.",
                )
            }
        } catch (e: HttpException) {
            GeoIpResultado.Error("El servidor respondió con un error (código ${e.code()}).", e)
        } catch (e: IOException) {
            GeoIpResultado.Error("Error de red. Revisa tu conexión e inténtalo de nuevo.", e)
        } catch (e: Exception) {
            GeoIpResultado.Error("Ocurrió un error inesperado: ${e.message ?: "desconocido"}.", e)
        }
}
