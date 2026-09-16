package com.ejemplo.nettycoon.ui.firewall

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pruebas JVM del [CatalogoPuertos]: mapeo puerto → nombre de servicio y formato de etiqueta.
 * Lógica pura, sin Android ni corrutinas.
 */
class CatalogoPuertosTest {

    @Test
    fun `nombreDe devuelve el servicio de puertos del catalogo`() {
        assertEquals("SSH / acceso remoto", CatalogoPuertos.nombreDe(22))
        assertEquals("HTTPS / webs seguras", CatalogoPuertos.nombreDe(443))
        assertEquals("MySQL / base de datos", CatalogoPuertos.nombreDe(3306))
        // SMTP está por partida doble (587 y 25), ambos con el mismo nombre.
        assertEquals("SMTP / envío de correo", CatalogoPuertos.nombreDe(587))
        assertEquals("SMTP / envío de correo", CatalogoPuertos.nombreDe(25))
    }

    @Test
    fun `nombreDe devuelve null para puertos fuera del catalogo o nulos`() {
        assertNull(CatalogoPuertos.nombreDe(9999))
        assertNull(CatalogoPuertos.nombreDe(null))
    }

    @Test
    fun `buscar encuentra el puerto comun por numero`() {
        val ssh = CatalogoPuertos.buscar(22)
        assertEquals(22, ssh?.numero)
        assertEquals("SSH / acceso remoto", ssh?.nombre)
        assertNull(CatalogoPuertos.buscar(9999))
    }

    @Test
    fun `etiqueta usa puerto y nombre cuando existe`() {
        assertEquals("22 — SSH / acceso remoto", CatalogoPuertos.etiqueta(22))
        assertEquals("443 — HTTPS / webs seguras", CatalogoPuertos.etiqueta(443))
    }

    @Test
    fun `etiqueta usa solo el numero cuando el puerto no esta en el catalogo`() {
        assertEquals("9999", CatalogoPuertos.etiqueta(9999))
    }
}
