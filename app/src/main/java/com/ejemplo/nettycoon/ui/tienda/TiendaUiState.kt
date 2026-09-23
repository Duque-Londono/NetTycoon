package com.ejemplo.nettycoon.ui.tienda

import com.ejemplo.nettycoon.data.local.entity.EstadoPartida

/**
 * Estado de UI de la tienda (E3).
 *
 * - [partida]: estado actual (dinero, salud, escudo) para pintar la cabecera y calcular precios.
 * - [opciones]: artículos a la venta YA resueltos contra la partida actual (precio real y si se
 *   pueden comprar o no). Los calcula el ViewModel con la lógica pura de
 *   [com.ejemplo.nettycoon.domain.firewall.Tienda]: la pantalla solo los pinta.
 * - [cargando] / [error]: estados explícitos de la carga y la persistencia.
 * - [aviso]: resultado de la última compra (salió bien o por qué no). Transitorio: la UI lo muestra
 *   y luego llama a `limpiarAviso()`. Va en su propio canal para no pisar [error], que se reserva
 *   para fallos técnicos (mismo criterio que `avisoReglas` en "Ataque en vivo").
 */
data class TiendaUiState(
    val partida: EstadoPartida? = null,
    val opciones: List<OpcionTienda> = emptyList(),
    val cargando: Boolean = true,
    val aviso: String? = null,
    val error: String? = null,
)

/** Qué se puede comprar. Es el identificador que la pantalla devuelve al ViewModel al pulsar. */
sealed interface ArticuloTienda {
    /**
     * Cura de hasta [paquete] puntos de salud. El importe real se cobra en proporción a lo que de
     * verdad se cure (nunca por encima del tope de 100), así que el paquete es un máximo.
     */
    data class Cura(val paquete: Int) : ArticuloTienda

    /** Escudo de un solo uso: absorbe el próximo golpe de salud. */
    data object Escudo : ArticuloTienda
}

/**
 * Un artículo de la tienda ya resuelto contra la partida actual: con su precio real y con el
 * motivo por el que no se puede comprar, si es el caso.
 *
 * Que venga pre-resuelto es lo que permite que la pantalla sea "tonta": no recalcula precios ni
 * decide si algo está permitido, solo muestra [precio] y habilita el botón según [habilitada].
 */
data class OpcionTienda(
    val articulo: ArticuloTienda,
    val titulo: String,
    val descripcion: String,
    /** Importe que se cobrará si se compra ahora mismo. */
    val precio: Int,
    val habilitada: Boolean,
    /** Por qué no se puede comprar (para mostrarlo bajo el artículo), o `null` si sí se puede. */
    val razonDeshabilitada: String? = null,
)
