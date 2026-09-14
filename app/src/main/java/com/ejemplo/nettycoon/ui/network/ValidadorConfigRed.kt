package com.ejemplo.nettycoon.ui.network

/**
 * Resultado de validar el formulario de configuración de red.
 *
 * [Valido] entrega ya los datos convertidos al tipo que espera la entidad `EstadoPartida`
 * (IP como `String` no vacío, puertos como `Int`), de modo que el ViewModel no tenga que
 * volver a parsear nada.
 */
sealed interface ValidacionConfigRed {
    data class Valido(
        val ipRouter: String,
        val puertoLan: Int,
        val puertoWan: Int,
    ) : ValidacionConfigRed

    data class Invalido(val mensaje: String) : ValidacionConfigRed
}

/**
 * Validación del formulario de configuración de red. Lógica pura (sin Android ni corrutinas),
 * para poder probarla en la JVM y para que el ViewModel se limite a orquestar. Sigue el mismo
 * enfoque que `ValidadorRegla` de la feature de firewall.
 *
 * Reglas:
 * - **IP del router:** obligatoria (a diferencia de la IP de una regla, aquí NO hay comodín: es
 *   la dirección concreta del router del jugador) y con formato IPv4 válido.
 * - **Puerto LAN / puerto WAN:** obligatorios, numéricos, dentro de [PUERTO_MIN]..[PUERTO_MAX].
 *
 * Se valida en orden (IP → LAN → WAN) y se devuelve el primer error encontrado, de forma que el
 * mensaje que ve el jugador siempre apunta al primer campo que debe corregir.
 */
object ValidadorConfigRed {

    const val PUERTO_MIN = 1
    const val PUERTO_MAX = 65535

    fun validar(
        ipRouterTexto: String,
        puertoLanTexto: String,
        puertoWanTexto: String,
    ): ValidacionConfigRed {
        val ipLimpia = ipRouterTexto.trim()
        if (ipLimpia.isEmpty()) {
            return ValidacionConfigRed.Invalido("Escribe la IP del router.")
        }
        if (!esIpv4Valida(ipLimpia)) {
            return ValidacionConfigRed.Invalido(
                "La IP del router no tiene un formato válido (ej. 192.168.0.1).",
            )
        }

        val puertoLan = when (val r = validarPuerto(puertoLanTexto, "LAN")) {
            is ResultadoPuerto.Error -> return ValidacionConfigRed.Invalido(r.mensaje)
            is ResultadoPuerto.Ok -> r.puerto
        }
        val puertoWan = when (val r = validarPuerto(puertoWanTexto, "WAN")) {
            is ResultadoPuerto.Error -> return ValidacionConfigRed.Invalido(r.mensaje)
            is ResultadoPuerto.Ok -> r.puerto
        }

        return ValidacionConfigRed.Valido(
            ipRouter = ipLimpia,
            puertoLan = puertoLan,
            puertoWan = puertoWan,
        )
    }

    private sealed interface ResultadoPuerto {
        data class Ok(val puerto: Int) : ResultadoPuerto
        data class Error(val mensaje: String) : ResultadoPuerto
    }

    /** Valida un puerto y arma los mensajes de error nombrando el campo ([etiqueta]: "LAN"/"WAN"). */
    private fun validarPuerto(texto: String, etiqueta: String): ResultadoPuerto {
        val limpio = texto.trim()
        if (limpio.isEmpty()) {
            return ResultadoPuerto.Error("Escribe el puerto $etiqueta.")
        }
        val puerto = limpio.toIntOrNull()
            ?: return ResultadoPuerto.Error("El puerto $etiqueta debe ser un número.")
        if (puerto !in PUERTO_MIN..PUERTO_MAX) {
            return ResultadoPuerto.Error(
                "El puerto $etiqueta debe estar entre $PUERTO_MIN y $PUERTO_MAX.",
            )
        }
        return ResultadoPuerto.Ok(puerto)
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
