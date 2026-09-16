package com.ejemplo.nettycoon.ui.firewall

/**
 * Un puerto de red frecuente, con su nombre humano y una explicación sencilla.
 *
 * Es un modelo de PRESENTACIÓN de la pantalla de reglas: sirve para que el jugador ELIJA un puerto
 * entendiendo qué es, en vez de adivinar un número. No es una entidad de Room ni cambia el modelo
 * de datos (la regla se sigue guardando con su puerto como `Int`).
 */
data class PuertoComun(
    val numero: Int,
    val nombre: String,
    val explicacion: String,
)

/**
 * Catálogo fijo de puertos comunes y helpers puros para asociar número ↔ servicio. Refuerza la
 * misma asociación que el jugador ya vio en "Ataque en vivo".
 *
 * SMTP aparece como dos entradas (587 y 25) porque ambos puertos se usan para envío de correo:
 * así cada elección del selector rellena un único número de puerto.
 */
object CatalogoPuertos {

    val puertos: List<PuertoComun> = listOf(
        PuertoComun(443, "HTTPS / webs seguras", "El tráfico de páginas web seguras. Muy común y normalmente legítimo."),
        PuertoComun(80, "HTTP / webs", "Páginas web sin cifrar. Común, aunque hoy se prefiere el 443."),
        PuertoComun(22, "SSH / acceso remoto", "Control remoto del servidor. Peligroso si lo abres a desconocidos."),
        PuertoComun(3389, "RDP / escritorio remoto", "Ver y usar un equipo a distancia. Blanco frecuente de ataques."),
        PuertoComun(3306, "MySQL / base de datos", "Acceso a la base de datos. Nunca debería estar abierto a internet."),
        PuertoComun(21, "FTP / transferencia de archivos", "Enviar/recibir archivos, de forma insegura. Riesgoso si es externo."),
        PuertoComun(23, "Telnet / acceso remoto antiguo", "Método viejo e inseguro: manda las claves sin cifrar."),
        PuertoComun(53, "DNS / nombres de dominio", "Traduce nombres (google.com) a direcciones. Esencial para navegar."),
        PuertoComun(587, "SMTP / envío de correo", "Envío de correos electrónicos."),
        PuertoComun(25, "SMTP / envío de correo", "Envío de correos electrónicos."),
    )

    /** El puerto común con ese número, o `null` si no está en el catálogo. */
    fun buscar(puerto: Int?): PuertoComun? =
        if (puerto == null) null else puertos.firstOrNull { it.numero == puerto }

    /** Nombre humano del servicio de un puerto, o `null` si no está en el catálogo. */
    fun nombreDe(puerto: Int?): String? = buscar(puerto)?.nombre

    /**
     * Etiqueta coherente número ↔ servicio, usada en el selector, la explicación y la lista:
     * `"22 — SSH / acceso remoto"` si el puerto está en el catálogo, o solo el número si no.
     */
    fun etiqueta(puerto: Int): String =
        nombreDe(puerto)?.let { "$puerto — $it" } ?: puerto.toString()
}
