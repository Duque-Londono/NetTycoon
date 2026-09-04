package com.ejemplo.nettycoon.data.remote

import com.ejemplo.nettycoon.data.remote.dto.IpWhoIsResponseDto
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Servicio Retrofit de la API geo-IP (ipwho.is).
 *
 * Con baseUrl `https://ipwho.is/`, el path `{ip}` resuelve a `https://ipwho.is/{ip}` (HTTPS).
 */
interface GeoIpApiService {

    /** Consulta los datos geográficos/red de una IP. */
    @GET("{ip}")
    suspend fun consultarIp(@Path("ip") ip: String): IpWhoIsResponseDto
}
