package com.ejemplo.nettycoon.ui.firewall

/**
 * Resultado de validar el formulario de creación de una regla.
 *
 * [Valido] entrega ya los datos convertidos al tipo que espera la entidad `ReglaFirewall`
 * (puerto como `Int`, IP como `String?` donde `null` significa "cualquier IP"), de modo que el
 * ViewModel no tenga que volver a parsear nada.
 */
sealed interface ValidacionRegla {
    data class Valido(val puerto: Int, val ip: String?) : ValidacionRegla
    data class Invalido(val mensaje: String) : ValidacionRegla
}

/**
 * Validación del formulario de reglas de firewall. Lógica pura (sin Android ni corrutinas),
 * para poder probarla en la JVM y para que el ViewModel se limite a orquestar.
 *
 * Reglas:
 * - **Puerto:** obligatorio, numérico, dentro de [PUERTO_MIN]..[PUERTO_MAX].
 * - **IP:** opcional. Vacía (o solo espacios) significa **cualquier IP** y se guarda como `null`,
 *   que es el comodín que ya entiende `MotorFirewall`. Si se escribe, debe ser IPv4 válida.
 */
object ValidadorRegla {

    const val PUERTO_MIN = 1
    const val PUERTO_MAX = 65535

    fun validar(puertoTexto: String, ipTexto: String): ValidacionRegla {
        val puertoLimpio = puertoTexto.trim()
        if (puertoLimpio.isEmpty()) {
            return ValidacionRegla.Invalido("Escribe un puerto.")
        }
        val puerto = puertoLimpio.toIntOrNull()
            ?: return ValidacionRegla.Invalido("El puerto debe ser un número.")
        if (puerto !in PUERTO_MIN..PUERTO_MAX) {
            return ValidacionRegla.Invalido("El puerto debe estar entre $PUERTO_MIN y $PUERTO_MAX.")
        }

        val ipLimpia = ipTexto.trim()
        if (ipLimpia.isEmpty()) {
            // Sin IP = comodín: la regla aplica a cualquier origen.
            return ValidacionRegla.Valido(puerto = puerto, ip = null)
        }
        if (!esIpv4Valida(ipLimpia)) {
            return ValidacionRegla.Invalido("La IP no tiene un formato válido (ej. 203.0.113.9).")
        }
        return ValidacionRegla.Valido(puerto = puerto, ip = ipLimpia)
    }

    /** IPv4 en notación decimal punteada: cuatro octetos de 1 a 3 dígitos, cada uno 0..255. */
    private fun esIpv4Valida(ip: String): Boolean {
        val octetos = ip.split(".")
        if (octetos.size != 4) return false
        return octetos.all { octeto ->
            octeto.isNotEmpty() &&
                octeto.length <= 3 &&
                octeto.all { it.isDigit() } &&
                octeto.toInt() in 0..255
        }
    }
}
