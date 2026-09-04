package com.ejemplo.nettycoon.data.local.entity

/**
 * Acción que aplica una [ReglaFirewall] sobre el tráfico que coincide con ella.
 *
 * Se persiste como `String` (su [name]) mediante un TypeConverter, de modo que
 * reordenar o añadir valores en el futuro no corrompe los datos guardados.
 */
enum class AccionFirewall {
    /** Permite el paso del tráfico. */
    ALLOW,

    /** Bloquea el tráfico. */
    DENY,
}
