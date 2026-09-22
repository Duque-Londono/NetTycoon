package com.ejemplo.nettycoon.domain.firewall

import com.ejemplo.nettycoon.data.local.entity.EstadoPartida

/**
 * Tienda del juego (E3): en qué GASTAR el [EstadoPartida.dineroVirtual].
 *
 * Hasta E2 el dinero solo entraba (+ por acierto) y salía como castigo (- por brecha/falso
 * positivo), sin ninguna decisión del jugador de por medio. La tienda le da dos usos (*sinks*):
 *
 * 1. **Curar** ([comprarCura]): restaurar salud pagando.
 * 2. **Escudo** ([comprarEscudo]): un seguro de UN SOLO USO que absorbe el próximo golpe de salud.
 *
 * **Anti-softlock:** curar es un ATAJO opcional, nunca la única salida. El piso sigue siendo la
 * regeneración gratuita por tiempo real de E2 ([RegenSalud]), así que un jugador con 0 de salud y
 * 0 de dinero se recupera igual esperando. Esta clase no toca esa ruta.
 *
 * Todo aquí son **funciones puras** sobre [EstadoPartida] (mismo patrón que [RegenSalud] y
 * `EstadoSalud`): sin Android, sin Room, sin corrutinas. Persistir el estado devuelto es
 * responsabilidad del ViewModel, que es quien habla con el repositorio.
 */
object Tienda {

    /** Por qué se rechazó una compra. Sirve para el mensaje que se le muestra al jugador. */
    enum class MotivoRechazo {
        /** No alcanza el dinero virtual para el precio pedido. */
        SALDO_INSUFICIENTE,

        /** La salud ya está al máximo: no hay nada que curar. */
        SALUD_COMPLETA,

        /** Ya hay un escudo activo: no son apilables. */
        ESCUDO_YA_ACTIVO,

        /**
         * La red está comprometida (salud 0) y se intentó comprar un escudo. Con el candado de E2
         * activo no se puede jugar, así que un escudo ahí sería dinero muerto: primero hay que
         * curar (o esperar a la regeneración gratuita).
         */
        RED_COMPROMETIDA,
    }

    /**
     * Resultado de un intento de compra.
     *
     * En caso de rechazo, [estado] es el estado **sin modificar** que se pasó: quien llama puede
     * persistirlo o descartarlo sin riesgo de cobrar de más.
     */
    data class ResultadoCompra(
        val estado: EstadoPartida,
        /** Importe realmente cobrado (0 si se rechazó). */
        val costo: Int = 0,
        /** Puntos de salud realmente curados (0 si se rechazó o si la compra fue un escudo). */
        val saludCurada: Int = 0,
        /** Motivo del rechazo, o `null` si la compra se realizó. */
        val motivo: MotivoRechazo? = null,
    ) {
        /** `true` si la compra se realizó y [estado] ya trae el cambio aplicado. */
        val exito: Boolean get() = motivo == null
    }

    /**
     * Puntos de salud que se curarían realmente con un [paquete] dado la [saludActual]: nunca más
     * de los que faltan para el tope. Es lo que hace que el tope de 100 no obligue a prohibir la
     * compra ni a cobrar de más.
     */
    fun saludCurable(saludActual: Int, paquete: Int): Int =
        (BalancePartida.SALUD_MAX - saludActual).coerceIn(0, paquete.coerceAtLeast(0))

    /**
     * Precio de curar un [paquete] partiendo de [saludActual]. Se cobra **proporcional a lo que de
     * verdad se cura**: con 90 de salud, el paquete de +25 solo cura 10 y cuesta 60, no 150.
     */
    fun precioCura(saludActual: Int, paquete: Int): Int =
        saludCurable(saludActual, paquete) * BalancePartida.PRECIO_CURA_POR_PUNTO

    /**
     * Compra de cura: sube la salud hasta [BalancePartida.SALUD_MAX] como tope y descuenta el
     * precio proporcional.
     *
     * Se permite **aunque la red esté comprometida** (salud 0): es precisamente el atajo de pago
     * para salir del candado de E2 sin esperar.
     */
    fun comprarCura(estado: EstadoPartida, paquete: Int): ResultadoCompra {
        val curable = saludCurable(estado.saludRed, paquete)
        if (curable <= 0) return ResultadoCompra(estado, motivo = MotivoRechazo.SALUD_COMPLETA)

        val costo = curable * BalancePartida.PRECIO_CURA_POR_PUNTO
        if (estado.dineroVirtual < costo) {
            return ResultadoCompra(estado, motivo = MotivoRechazo.SALDO_INSUFICIENTE)
        }

        return ResultadoCompra(
            estado = estado.copy(
                saludRed = estado.saludRed + curable,
                dineroVirtual = estado.dineroVirtual - costo,
            ),
            costo = costo,
            saludCurada = curable,
        )
    }

    /**
     * Compra del escudo de un uso. Reglas:
     * - **No apilable:** con un escudo ya activo se rechaza.
     * - **No con la red comprometida** (salud 0): ver [MotivoRechazo.RED_COMPROMETIDA].
     *
     * El escudo se consume más tarde, en [ConsecuenciasPartida.aplicar], y solo absorbe el golpe de
     * SALUD: el castigo de dinero y puntaje del error sigue aplicándose íntegro.
     */
    fun comprarEscudo(estado: EstadoPartida): ResultadoCompra {
        if (estado.escudoActivo) {
            return ResultadoCompra(estado, motivo = MotivoRechazo.ESCUDO_YA_ACTIVO)
        }
        if (estado.saludRed <= BalancePartida.SALUD_MIN) {
            return ResultadoCompra(estado, motivo = MotivoRechazo.RED_COMPROMETIDA)
        }
        val costo = BalancePartida.PRECIO_ESCUDO
        if (estado.dineroVirtual < costo) {
            return ResultadoCompra(estado, motivo = MotivoRechazo.SALDO_INSUFICIENTE)
        }

        return ResultadoCompra(
            estado = estado.copy(
                escudoActivo = true,
                dineroVirtual = estado.dineroVirtual - costo,
            ),
            costo = costo,
        )
    }
}
