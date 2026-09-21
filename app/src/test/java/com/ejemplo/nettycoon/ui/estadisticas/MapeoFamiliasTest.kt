package com.ejemplo.nettycoon.ui.estadisticas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas del mapeo puerto → familia (capa de presentación). No necesita corrutinas ni Room:
 * es una tabla pura. Fija los puertos estándar de cada familia y el fallback a "Otros".
 */
class MapeoFamiliasTest {

    @Test
    fun `acceso remoto agrupa ssh telnet rdp y vnc`() {
        listOf(22, 23, 3389, 5900).forEach { puerto ->
            assertEquals(MapeoFamilias.ACCESO_REMOTO, MapeoFamilias.familiaDe(puerto))
        }
    }

    @Test
    fun `web agrupa http https y alternativos`() {
        listOf(80, 443, 8080, 8443).forEach { puerto ->
            assertEquals(MapeoFamilias.WEB, MapeoFamilias.familiaDe(puerto))
        }
    }

    @Test
    fun `bases de datos agrupa los motores conocidos`() {
        listOf(1433, 1521, 3306, 5432, 6379, 27017).forEach { puerto ->
            assertEquals(MapeoFamilias.BASES_DE_DATOS, MapeoFamilias.familiaDe(puerto))
        }
    }

    @Test
    fun `correo clasifica el puerto 25`() {
        assertEquals(MapeoFamilias.CORREO, MapeoFamilias.familiaDe(25))
        // y el resto de la familia de correo
        listOf(465, 587, 110, 995, 143, 993).forEach { puerto ->
            assertEquals(MapeoFamilias.CORREO, MapeoFamilias.familiaDe(puerto))
        }
    }

    @Test
    fun `transferencia de archivos clasifica el puerto 21`() {
        assertEquals(MapeoFamilias.ARCHIVOS, MapeoFamilias.familiaDe(21))
        listOf(20, 69, 989, 990, 445, 2049).forEach { puerto ->
            assertEquals(MapeoFamilias.ARCHIVOS, MapeoFamilias.familiaDe(puerto))
        }
    }

    @Test
    fun `dns y servicios de red clasifica el puerto 53`() {
        assertEquals(MapeoFamilias.DNS_RED, MapeoFamilias.familiaDe(53))
        listOf(67, 68, 123).forEach { puerto ->
            assertEquals(MapeoFamilias.DNS_RED, MapeoFamilias.familiaDe(puerto))
        }
    }

    @Test
    fun `directorio y autenticacion clasifica el puerto 389`() {
        assertEquals(MapeoFamilias.DIRECTORIO_AUTENTICACION, MapeoFamilias.familiaDe(389))
        listOf(88, 636, 1812, 1813).forEach { puerto ->
            assertEquals(MapeoFamilias.DIRECTORIO_AUTENTICACION, MapeoFamilias.familiaDe(puerto))
        }
    }

    @Test
    fun `gestion y monitoreo clasifica el puerto 161`() {
        assertEquals(MapeoFamilias.GESTION_MONITOREO, MapeoFamilias.familiaDe(161))
        listOf(162, 5985, 5986).forEach { puerto ->
            assertEquals(MapeoFamilias.GESTION_MONITOREO, MapeoFamilias.familiaDe(puerto))
        }
    }

    @Test
    fun `telefonia clasifica el puerto 5060`() {
        assertEquals(MapeoFamilias.TELEFONIA, MapeoFamilias.familiaDe(5060))
        assertEquals(MapeoFamilias.TELEFONIA, MapeoFamilias.familiaDe(5061))
    }

    @Test
    fun `sospechosos agrupa 4444 y 31337`() {
        assertEquals(MapeoFamilias.SOSPECHOSOS, MapeoFamilias.familiaDe(4444))
        assertEquals(MapeoFamilias.SOSPECHOSOS, MapeoFamilias.familiaDe(31337))
    }

    @Test
    fun `puerto sin mapa y puerto nulo caen en Otros`() {
        assertEquals(MapeoFamilias.OTROS, MapeoFamilias.familiaDe(9999))
        assertEquals(MapeoFamilias.OTROS, MapeoFamilias.familiaDe(null))
    }

    @Test
    fun `cada familia mapeada tiene descripcion de una linea y Otros no`() {
        val familiasMapeadas = listOf(
            MapeoFamilias.ACCESO_REMOTO, MapeoFamilias.WEB, MapeoFamilias.BASES_DE_DATOS,
            MapeoFamilias.CORREO, MapeoFamilias.ARCHIVOS, MapeoFamilias.DNS_RED,
            MapeoFamilias.DIRECTORIO_AUTENTICACION, MapeoFamilias.GESTION_MONITOREO,
            MapeoFamilias.TELEFONIA, MapeoFamilias.SOSPECHOSOS,
        )
        familiasMapeadas.forEach { familia ->
            assertNotNull("Falta descripción para $familia", MapeoFamilias.descripcionDe(familia))
            assertTrue(MapeoFamilias.descripcionDe(familia)!!.isNotBlank())
        }
        assertNull(MapeoFamilias.descripcionDe(MapeoFamilias.OTROS))
    }
}
