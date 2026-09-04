package com.ejemplo.nettycoon.data.local

import androidx.room.TypeConverter
import com.ejemplo.nettycoon.data.local.entity.AccionFirewall
import com.ejemplo.nettycoon.data.local.entity.ResultadoEvento

/**
 * TypeConverters de Room para los enums del dominio.
 *
 * Se guardan como `String` (el [Enum.name]) en vez de por su ordinal: así, añadir o
 * reordenar valores del enum no corrompe los datos ya persistidos.
 */
class Converters {

    @TypeConverter
    fun accionAString(accion: AccionFirewall): String = accion.name

    @TypeConverter
    fun stringAAccion(valor: String): AccionFirewall = AccionFirewall.valueOf(valor)

    @TypeConverter
    fun resultadoAString(resultado: ResultadoEvento): String = resultado.name

    @TypeConverter
    fun stringAResultado(valor: String): ResultadoEvento = ResultadoEvento.valueOf(valor)
}
