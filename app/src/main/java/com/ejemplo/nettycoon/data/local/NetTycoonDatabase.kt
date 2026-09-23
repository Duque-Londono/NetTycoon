package com.ejemplo.nettycoon.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
 *
 * **Versionado:** v1 → v2 añade `escudoActivo` a `estado_partida` (tienda de E3). La migración es
 * EXPLÍCITA ([MIGRACION_1_2]); a propósito **no** se usa `fallbackToDestructiveMigration`, que
 * borraría las partidas ya guardadas de los jugadores.
 */
@Database(
    entities = [
        ReglaFirewall::class,
        EventoAtaque::class,
        EstadoPartida::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class NetTycoonDatabase : RoomDatabase() {

    abstract fun reglaFirewallDao(): ReglaFirewallDao
    abstract fun eventoAtaqueDao(): EventoAtaqueDao
    abstract fun estadoPartidaDao(): EstadoPartidaDao

    companion object {
        private const val NOMBRE_BD = "nettycoon.db"

        /**
         * v1 → v2: columna `escudoActivo` en `estado_partida` (escudo de un uso, E3).
         *
         * Aditiva y no destructiva: `ALTER TABLE ... ADD COLUMN` con `DEFAULT 0`, de modo que las
         * partidas existentes quedan simplemente "sin escudo" y conservan puntaje, dinero, salud y
         * configuración de red. Room representa el `Boolean` de Kotlin como `INTEGER NOT NULL`
         * (0 = false), por lo que el tipo y la nulabilidad deben coincidir exactamente con los de
         * la entidad o la validación de esquema al abrir la BD fallaría.
         */
        val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE estado_partida ADD COLUMN escudoActivo INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        @Volatile
        private var INSTANCIA: NetTycoonDatabase? = null

        /** Devuelve el singleton, creándolo de forma segura ante concurrencia. */
        fun obtenerInstancia(context: Context): NetTycoonDatabase =
            INSTANCIA ?: synchronized(this) {
                INSTANCIA ?: Room.databaseBuilder(
                    context.applicationContext,
                    NetTycoonDatabase::class.java,
                    NOMBRE_BD,
                ).addMigrations(MIGRACION_1_2)
                    .build()
                    .also { INSTANCIA = it }
            }
    }
}
