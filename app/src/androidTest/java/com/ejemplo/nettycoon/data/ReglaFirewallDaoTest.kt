package com.ejemplo.nettycoon.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ejemplo.nettycoon.data.local.NetTycoonDatabase
import com.ejemplo.nettycoon.data.local.dao.ReglaFirewallDao
import com.ejemplo.nettycoon.data.local.entity.AccionFirewall
import com.ejemplo.nettycoon.data.local.entity.ReglaFirewall
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba instrumentada mínima sobre una base Room IN-MEMORY.
 *
 * Su objetivo real es verificar en TIEMPO DE EJECUCIÓN que la implementación del DAO
 * generada por KSP (con el setup de AGP 9 / Kotlin integrado) funciona: inserta una
 * [ReglaFirewall] y la lee de vuelta.
 */
@RunWith(AndroidJUnit4::class)
class ReglaFirewallDaoTest {

    private lateinit var db: NetTycoonDatabase
    private lateinit var dao: ReglaFirewallDao

    @Before
    fun crearBd() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NetTycoonDatabase::class.java,
        ).build()
        dao = db.reglaFirewallDao()
    }

    @After
    fun cerrarBd() {
        db.close()
    }

    @Test
    fun insertarYLeerRegla() = runBlocking {
        val regla = ReglaFirewall(
            owner = "uid-de-prueba",
            puerto = 443,
            ip = "203.0.113.5",
            accion = AccionFirewall.DENY,
        )

        val id = dao.insertar(regla)
        val leida = dao.obtenerPorId(id)

        assertNotNull("El DAO generado por KSP debe devolver la regla insertada", leida)
        assertEquals("uid-de-prueba", leida!!.owner)
        assertEquals(443, leida.puerto)
        assertEquals("203.0.113.5", leida.ip)
        assertEquals(AccionFirewall.DENY, leida.accion)
    }
}
