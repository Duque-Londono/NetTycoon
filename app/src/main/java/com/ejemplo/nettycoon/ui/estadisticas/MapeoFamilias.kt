package com.ejemplo.nettycoon.ui.estadisticas

/**
 * Mapeo **puerto → familia de decisión**, usado solo por la pantalla de estadísticas para
 * agrupar el historial de ataques en categorías legibles ("dominadas" vs. "flojas").
 *
 * ⚠️ BORRADOR ESTRUCTURAL PARA VALIDACIÓN DEL EQUIPO. Es únicamente la tabla de agrupación:
 * qué puerto cae en qué familia, cómo se llama cada familia y su descripción de una línea. Los
 * puertos son asignaciones IANA/estándar (conocimiento IT); los NOMBRES de familia y las
 * DESCRIPCIONES son provisionales y los revisa el equipo. Aquí no se redacta prosa educativa.
 *
 * Vive en la capa de presentación (`ui/estadisticas/`) A PROPÓSITO: no toca Room, ni el
 * dominio, ni el esquema de `EventoAtaque`. La "familia" NO se persiste; se deriva en lectura
 * a partir de `EventoAtaque.puertoDestino`, que es el único dato con el que se puede clasificar.
 *
 * Un puerto sin entrada en la tabla (o un evento con `puertoDestino` nulo) cae en [OTROS].
 */
object MapeoFamilias {

    // --- Nombres de familia (borrador para validación del equipo) ---
    const val ACCESO_REMOTO = "Acceso remoto"
    const val WEB = "Web"
    const val BASES_DE_DATOS = "Bases de datos"
    const val CORREO = "Correo"
    const val ARCHIVOS = "Transferencia y compartición de archivos"
    const val DNS_RED = "DNS y servicios de red"
    const val DIRECTORIO_AUTENTICACION = "Directorio y autenticación"
    const val GESTION_MONITOREO = "Gestión y monitoreo"
    const val TELEFONIA = "Telefonía (VoIP)"
    const val SOSPECHOSOS = "Sospechosos / no estándar"

    /** Familia por defecto para puertos sin clasificar o eventos sin puerto conocido. */
    const val OTROS = "Otros"

    /**
     * Tabla puerto → familia. Los puertos son asignaciones estándar; cubren los que aparecen en
     * el catálogo de escenarios y en el generador de ataques, más los bien conocidos habituales.
     * Provisional en cuanto a nombres/agrupación (ver nota de arriba).
     */
    private val PUERTO_A_FAMILIA: Map<Int, String> = mapOf(
        // Acceso remoto
        22 to ACCESO_REMOTO,    // SSH
        23 to ACCESO_REMOTO,    // Telnet
        3389 to ACCESO_REMOTO,  // RDP
        5900 to ACCESO_REMOTO,  // VNC
        // Web
        80 to WEB,              // HTTP
        443 to WEB,             // HTTPS
        8080 to WEB,            // HTTP alternativo
        8443 to WEB,            // HTTPS alternativo
        // Bases de datos
        1433 to BASES_DE_DATOS, // Microsoft SQL Server
        1521 to BASES_DE_DATOS, // Oracle
        3306 to BASES_DE_DATOS, // MySQL/MariaDB
        5432 to BASES_DE_DATOS, // PostgreSQL
        6379 to BASES_DE_DATOS, // Redis
        27017 to BASES_DE_DATOS, // MongoDB
        // Correo
        25 to CORREO,           // SMTP
        465 to CORREO,          // SMTPS
        587 to CORREO,          // SMTP (envío autenticado)
        110 to CORREO,          // POP3
        995 to CORREO,          // POP3S
        143 to CORREO,          // IMAP
        993 to CORREO,          // IMAPS
        // Transferencia y compartición de archivos
        20 to ARCHIVOS,         // FTP (datos)
        21 to ARCHIVOS,         // FTP (control)
        69 to ARCHIVOS,         // TFTP
        989 to ARCHIVOS,        // FTPS (datos)
        990 to ARCHIVOS,        // FTPS (control)
        445 to ARCHIVOS,        // SMB
        2049 to ARCHIVOS,       // NFS
        // DNS y servicios de red
        53 to DNS_RED,          // DNS
        67 to DNS_RED,          // DHCP (servidor)
        68 to DNS_RED,          // DHCP (cliente)
        123 to DNS_RED,         // NTP
        // Directorio y autenticación
        88 to DIRECTORIO_AUTENTICACION,   // Kerberos
        389 to DIRECTORIO_AUTENTICACION,  // LDAP
        636 to DIRECTORIO_AUTENTICACION,  // LDAPS
        1812 to DIRECTORIO_AUTENTICACION, // RADIUS (autenticación)
        1813 to DIRECTORIO_AUTENTICACION, // RADIUS (accounting)
        // Gestión y monitoreo
        161 to GESTION_MONITOREO, // SNMP
        162 to GESTION_MONITOREO, // SNMP (traps)
        5985 to GESTION_MONITOREO, // WinRM (HTTP)
        5986 to GESTION_MONITOREO, // WinRM (HTTPS)
        // Telefonía (VoIP)
        5060 to TELEFONIA,      // SIP
        5061 to TELEFONIA,      // SIP (TLS)
        // Sospechosos / no estándar: sin servicio legítimo estándar; banderas rojas típicas de
        // backdoors/malware. Ángulo defensivo/educativo, no una asignación IANA.
        4444 to SOSPECHOSOS,    // usado por defecto por varias herramientas de intrusión
        31337 to SOSPECHOSOS,   // "elite"; histórico de backdoors (p. ej. Back Orifice)
    )

    /**
     * Descripción de UNA línea por familia, para mostrar bajo cada sección de la pantalla.
     *
     * ⚠️ PLACEHOLDER para validación del equipo: texto provisional, sin prosa educativa extensa.
     * [OTROS] no lleva descripción (es el cajón de sastre y no siempre se muestra como sección).
     */
    val DESCRIPCIONES: Map<String, String> = mapOf(
        ACCESO_REMOTO to "Entrar y controlar equipos a distancia.",
        WEB to "Navegación y servicios web.",
        BASES_DE_DATOS to "Servidores donde se guardan los datos.",
        CORREO to "Envío y recepción de correo electrónico.",
        ARCHIVOS to "Compartir y transferir archivos.",
        DNS_RED to "Servicios que hacen funcionar la red (nombres, direcciones, hora).",
        DIRECTORIO_AUTENTICACION to "Identidad y control de acceso de usuarios.",
        GESTION_MONITOREO to "Administración y vigilancia de equipos.",
        TELEFONIA to "Llamadas de voz sobre internet.",
        SOSPECHOSOS to "Puertos sin uso legítimo habitual; suelen ser banderas rojas.",
    )

    /**
     * Devuelve la familia de un puerto. Un puerto nulo o no mapeado cae en [OTROS], para que
     * ningún evento quede fuera del recuento.
     */
    fun familiaDe(puerto: Int?): String =
        puerto?.let { PUERTO_A_FAMILIA[it] } ?: OTROS

    /** Descripción de una línea de la familia, o `null` si no tiene (p. ej. [OTROS]). */
    fun descripcionDe(familia: String): String? = DESCRIPCIONES[familia]
}
