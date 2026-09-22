package com.ejemplo.nettycoon.data.local.prefs

import android.content.Context

/**
 * Persistencia del ANCLA de regeneración de salud (E2), con **SharedPreferences** (no Room).
 *
 * Decisión de diseño (documentada a propósito):
 * - El ancla es un timestamp (epoch millis) desde el que se cuenta el tiempo real transcurrido para
 *   regenerar salud (ver [com.ejemplo.nettycoon.domain.firewall.RegenSalud]). Se guarda **por uid**
 *   para que dos cuentas en el mismo dispositivo no compartan el reloj de regen.
 * - Vive **fuera de Room** para NO tocar el esquema de la partida ni requerir migración; el campo
 *   `actualizadoEn` de `EstadoPartida` no sirve porque E1 lo reescribe en cada decisión (daño o
 *   acierto), lo que reiniciaría la regen.
 * - Se usa SharedPreferences (y no DataStore) para no añadir dependencias nuevas: basta con un
 *   `Long` por uid leído/escrito al entrar a Panel/Ataque.
 *
 * Es un wrapper delgado; la lógica pura del recálculo vive en [RegenSalud] y la aplicación
 * (leer partida → recalcular → persistir) en `RegeneradorSalud`.
 */
class PreferenciasRegen(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(NOMBRE_ARCHIVO, Context.MODE_PRIVATE)

    /**
     * Ancla guardada para el [uid], o [porDefecto] si aún no hay ninguna. Se pasa `ahora` como
     * defecto para que un usuario sin ancla previa NO regenere de golpe desde epoch 0.
     */
    fun anclaDe(uid: String, porDefecto: Long): Long =
        prefs.getLong(clave(uid), porDefecto)

    /** Guarda el ancla del [uid]. */
    fun guardarAncla(uid: String, millis: Long) {
        prefs.edit().putLong(clave(uid), millis).apply()
    }

    private fun clave(uid: String) = "$CLAVE_ANCLA_PREFIJO$uid"

    private companion object {
        const val NOMBRE_ARCHIVO = "nettycoon_regen"
        const val CLAVE_ANCLA_PREFIJO = "ancla_"
    }
}
