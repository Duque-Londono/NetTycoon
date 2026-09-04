package com.ejemplo.nettycoon.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ejemplo.nettycoon.data.local.dao.EstadoPartidaDao
import com.ejemplo.nettycoon.data.local.dao.EventoAtaqueDao
import com.ejemplo.nettycoon.data.local.dao.ReglaFirewallDao
import com.ejemplo.nettycoon.data.local.entity.EstadoPartida
import com.ejemplo.nettycoon.data.local.entity.EventoAtaque
import com.ejemplo.nettycoon.data.local.entity.ReglaFirewall

/**
 * Base de datos Room de NetTycoon (estado local offline).
 *
 * Se expone como singleton vía [obtenerInstancia]: una sola instancia por proceso,
 * construida sobre el `applicationContext`. No se introduce ningún framework de DI: los
 * repositorios reciben sus DAOs desde esta instancia, y los futuros ViewModel reciben
 * los repositorios.
 */
@Database(
    entities = [
        ReglaFirewall::class,
        EventoAtaque::class,
        EstadoPartida::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class NetTycoonDatabase : RoomDatabase() {

    abstract fun reglaFirewallDao(): ReglaFirewallDao
    abstract fun eventoAtaqueDao(): EventoAtaqueDao
    abstract fun estadoPartidaDao(): EstadoPartidaDao

    companion object {
        private const val NOMBRE_BD = "nettycoon.db"

        @Volatile
        private var INSTANCIA: NetTycoonDatabase? = null

        /** Devuelve el singleton, creándolo de forma segura ante concurrencia. */
        fun obtenerInstancia(context: Context): NetTycoonDatabase =
            INSTANCIA ?: synchronized(this) {
                INSTANCIA ?: Room.databaseBuilder(
                    context.applicationContext,
                    NetTycoonDatabase::class.java,
                    NOMBRE_BD,
                ).build().also { INSTANCIA = it }
            }
    }
}
