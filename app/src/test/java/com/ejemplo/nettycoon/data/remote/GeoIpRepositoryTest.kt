package com.ejemplo.nettycoon.data.remote

import com.ejemplo.nettycoon.data.repository.GeoIpRepository
import com.ejemplo.nettycoon.data.repository.GeoIpResultado
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Unit tests JVM de [GeoIpRepository] usando MockWebServer: ejercitan Retrofit + Gson + OkHttp
 * reales SIN pegarle a la red real. Cubre success=true, success=false, HTTP no-2xx y error de red.
 */
class GeoIpRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: GeoIpRepository

    @Before
    fun crear() {
        server = MockWebServer()
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeoIpApiService::class.java)
        repo = GeoIpRepository(api)
    }

    @After
    fun cerrar() {
        // Puede haberse apagado ya en el test de error de red; ignorar en ese caso.
        runCatching { server.shutdown() }
    }

    @Test
    fun `success true mapea pais e isp (incluido connection isp anidado)`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"country":"Colombia","connection":{"isp":"Claro"}}""",
            ),
        )

        val r = repo.consultar("8.8.8.8")

        assertTrue(r is GeoIpResultado.Exito)
        val datos = (r as GeoIpResultado.Exito).datos
        assertEquals("Colombia", datos.pais)
        assertEquals("Claro", datos.isp)
    }

    @Test
    fun `success false devuelve Error con el message de ipwho`() = runBlocking {
        // ipwho.is responde 200 aun en error logico.
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":false,"message":"Invalid IP address"}""",
            ),
        )

        val r = repo.consultar("999.999.999.999")

        assertTrue(r is GeoIpResultado.Error)
        assertEquals("Invalid IP address", (r as GeoIpResultado.Error).mensaje)
    }

    @Test
    fun `http no-2xx devuelve Error con el codigo`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))

        val r = repo.consultar("8.8.8.8")

        assertTrue(r is GeoIpResultado.Error)
        assertTrue((r as GeoIpResultado.Error).mensaje.contains("500"))
    }

    @Test
    fun `error de red devuelve Error de conexion`() = runBlocking {
        // Apagar el servidor antes de la llamada fuerza una IOException.
        server.shutdown()

        val r = repo.consultar("8.8.8.8")

        assertTrue(r is GeoIpResultado.Error)
        assertTrue((r as GeoIpResultado.Error).mensaje.contains("red", ignoreCase = true))
    }
}
