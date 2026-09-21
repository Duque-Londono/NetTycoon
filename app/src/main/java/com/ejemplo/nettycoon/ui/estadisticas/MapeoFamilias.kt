package com.ejemplo.nettycoon.ui.estadisticas

/**
 * Mapeo **puerto → familia de decisión**, usado solo por la pantalla de estadísticas para
 * agrupar el historial de ataques en categorías legibles ("dominadas" vs. "flojas").
 *
 * ⚠️ BORRADOR ESTRUCTURAL PARA VALIDACIÓN DEL EQUIPO. Es únicamente la tabla de agrupación:
 * qué puerto cae en qué familia y cómo se llama cada familia. Los nombres son provisionales y
 * los revisa el equipo antes de darlos por buenos; aquí no se redacta prosa educativa.
 *
 * Vive en la capa de presentación (`ui/estadisticas/`) A PROPÓSITO: no toca Room, ni el
 * dominio, ni el esquema de `EventoAtaque`. La "familia" NO se persiste; se deriva en lectura
 * a partir de `EventoAtaque.puertoDestino`, que es el único dato con el que se puede clasificar.
 *
 * Un puerto sin entrada en la tabla (o un evento con `puertoDestino` nulo) cae en [OTROS].
 */
object MapeoFamilias {

    /** Familia por defecto para puertos sin clasificar o eventos sin puerto conocido. */
    const val OTROS = "Otros"

    /**
     * Tabla puerto → familia. Los puertos elegidos cubren los que aparecen en el catálogo de
     * escenarios y en el generador de ataques. Provisional (ver nota de arriba).
     */
    private val PUERTO_A_FAMILIA: Map<Int, String> = mapOf(
        // Acceso remoto
        22 to "Acceso remoto",
        23 to "Acceso remoto",
        3389 to "Acceso remoto",
        // Web
        80 to "Web",
        443 to "Web",
        8080 to "Web",
        // Correo
        25 to "Correo",
        587 to "Correo",
        993 to "Correo",
        995 to "Correo",
        143 to "Correo",
        110 to "Correo",
        // Bases de datos
        3306 to "Bases de datos",
        5432 to "Bases de datos",
        1433 to "Bases de datos",
        // Transferencia de archivos
        21 to "Transferencia de archivos",
        990 to "Transferencia de archivos",
        // Infraestructura de red
        53 to "Infraestructura de red",
        123 to "Infraestructura de red",
        // Telefonía / VoIP
        5060 to "Telefonía",
    )

    /**
     * Devuelve la familia de un puerto. Un puerto nulo o no mapeado cae en [OTROS], para que
     * ningún evento quede fuera del recuento.
     */
    fun familiaDe(puerto: Int?): String =
        puerto?.let { PUERTO_A_FAMILIA[it] } ?: OTROS
}
