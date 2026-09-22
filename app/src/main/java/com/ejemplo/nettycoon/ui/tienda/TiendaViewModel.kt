package com.ejemplo.nettycoon.ui.tienda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ejemplo.nettycoon.data.local.entity.EstadoPartida
import com.ejemplo.nettycoon.data.repository.PartidaRepository
import com.ejemplo.nettycoon.data.repository.RegeneradorSalud
import com.ejemplo.nettycoon.domain.firewall.BalancePartida
import com.ejemplo.nettycoon.domain.firewall.Tienda
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel de la tienda (E3). Único intermediario entre [TiendaScreen] y la capa de datos/dominio.
 *
 * **No conoce Firebase ni Auth:** recibe el [uid] ya resuelto por la navegación (mismo patrón que
 * `PanelViewModel` / `AtaqueEnVivoViewModel`). Habla solo con repositorios, nunca con DAOs.
 *
 * **Toda la regla de negocio es pura y vive fuera:** el ViewModel solo lee la partida, se la pasa a
 * [Tienda] (precio, saldo, topes, escudo no apilable) y persiste el estado que esa función pura
 * devuelve. Aquí no se calcula ningún precio ni se decide ninguna validación a mano.
 *
 * **Regen al entrar:** aplica la regeneración por tiempo real de E2 antes de leer la partida, para
 * que la salud sobre la que se calcula el precio de la cura esté al día (si no, el jugador podría
 * pagar por puntos que el tiempo ya le había devuelto gratis).
 */
class TiendaViewModel(
    private val uid: String,
    private val partidaRepo: PartidaRepository,
    private val regenerador: RegeneradorSalud,
) : ViewModel() {

    private val _estado = MutableStateFlow(TiendaUiState())
    val estado: StateFlow<TiendaUiState> = _estado.asStateFlow()

    init {
        cargarPartida()
    }

    private fun cargarPartida() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                regenerador.aplicar(uid)
                publicar(partidaRepo.getOrCreatePartida(uid), cargando = false)
            } catch (e: Exception) {
                _estado.update {
                    it.copy(cargando = false, error = mensajeDeError("cargar la partida", e))
                }
            }
        }
    }

    /**
     * Intenta comprar [articulo]. Delega la decisión completa en [Tienda] y **solo persiste si la
     * compra tuvo éxito**: ante un rechazo no se escribe nada en Room, de modo que un intento
     * inválido nunca pueda cobrar ni dejar la partida a medias.
     */
    fun comprar(articulo: ArticuloTienda) {
        val partida = _estado.value.partida ?: return
        if (_estado.value.cargando) return

        val resultado = when (articulo) {
            is ArticuloTienda.Cura -> Tienda.comprarCura(partida, articulo.paquete)
            ArticuloTienda.Escudo -> Tienda.comprarEscudo(partida)
        }

        if (!resultado.exito) {
            // Rechazo: no se toca la partida, solo se explica por qué.
            _estado.update { it.copy(aviso = textoRechazo(resultado.motivo!!)) }
            return
        }

        // Publicamos de inmediato (feedback: el dinero baja y la salud sube a la vista) y
        // persistimos después, mismo criterio que "Ataque en vivo" con el veredicto.
        publicar(resultado.estado, aviso = textoExito(articulo, resultado))

        viewModelScope.launch {
            try {
                partidaRepo.actualizarPartida(resultado.estado)
            } catch (e: Exception) {
                // Volvemos a lo que hay realmente guardado: no dejamos a la vista una compra que no
                // llegó a persistirse.
                _estado.update { it.copy(error = mensajeDeError("guardar la compra", e)) }
                try {
                    publicar(partidaRepo.getOrCreatePartida(uid))
                } catch (_: Exception) {
                    // Si ni releer se puede, se queda el error ya publicado.
                }
            }
        }
    }

    /** Descarta el aviso de compra (tras mostrarlo al usuario). */
    fun limpiarAviso() = _estado.update { it.copy(aviso = null) }

    /** Descarta el error actual (tras mostrarlo al usuario). */
    fun limpiarError() = _estado.update { it.copy(error = null) }

    /** Publica una partida y recalcula con ella el escaparate (precios y disponibilidad). */
    private fun publicar(
        partida: EstadoPartida,
        cargando: Boolean? = null,
        aviso: String? = null,
    ) = _estado.update {
        it.copy(
            partida = partida,
            opciones = construirOpciones(partida),
            cargando = cargando ?: it.cargando,
            aviso = aviso ?: it.aviso,
        )
    }

    /**
     * Arma el escaparate contra la partida actual: dos paquetes de cura, "reparar al máximo" y el
     * escudo. Cada opción se pre-resuelve con las funciones puras de [Tienda], así que el precio
     * mostrado es exactamente el que se va a cobrar y el botón solo se habilita si la compra
     * realmente se puede hacer.
     */
    private fun construirOpciones(partida: EstadoPartida): List<OpcionTienda> {
        val curas = listOf(
            BalancePartida.CURA_PAQUETE_CHICO,
            BalancePartida.CURA_PAQUETE_GRANDE,
            // "Al máximo": se pide el tope entero y la propia Tienda lo recorta a lo que falte.
            BalancePartida.SALUD_MAX,
        ).map { paquete -> opcionCura(partida, paquete) }

        return curas + opcionEscudo(partida)
    }

    private fun opcionCura(partida: EstadoPartida, paquete: Int): OpcionTienda {
        val puntos = Tienda.saludCurable(partida.saludRed, paquete)
        val precio = Tienda.precioCura(partida.saludRed, paquete)
        val alMaximo = paquete >= BalancePartida.SALUD_MAX
        val titulo = if (alMaximo) "Reparar al máximo" else "Reparar +$paquete de salud"

        val razon = when {
            puntos <= 0 -> "Tu red ya está al máximo de salud."
            partida.dineroVirtual < precio -> "Te faltan \$${precio - partida.dineroVirtual}."
            else -> null
        }

        return OpcionTienda(
            articulo = ArticuloTienda.Cura(paquete),
            titulo = titulo,
            descripcion = if (puntos <= 0) {
                "No hay nada que reparar."
            } else {
                "Recupera $puntos de salud al instante, a " +
                    "\$${BalancePartida.PRECIO_CURA_POR_PUNTO} por punto. " +
                    "Esperar sigue siendo gratis."
            },
            precio = precio,
            habilitada = razon == null,
            razonDeshabilitada = razon,
        )
    }

    private fun opcionEscudo(partida: EstadoPartida): OpcionTienda {
        val precio = BalancePartida.PRECIO_ESCUDO
        val razon = when {
            partida.escudoActivo -> "Ya tienes un escudo activo: no son apilables."
            partida.saludRed <= BalancePartida.SALUD_MIN ->
                "Con la red caída no puedes jugar: repara la salud primero."
            partida.dineroVirtual < precio -> "Te faltan \$${precio - partida.dineroVirtual}."
            else -> null
        }

        return OpcionTienda(
            articulo = ArticuloTienda.Escudo,
            titulo = "Escudo (un solo uso)",
            descripcion = "Absorbe el próximo golpe de salud, sea una brecha o un falso positivo. " +
                "No te salva del coste en dinero y puntaje: el error se sigue pagando y se sigue " +
                "viendo.",
            precio = precio,
            habilitada = razon == null,
            razonDeshabilitada = razon,
        )
    }

    private fun textoExito(articulo: ArticuloTienda, resultado: Tienda.ResultadoCompra): String =
        when (articulo) {
            is ArticuloTienda.Cura ->
                "Red reparada: +${resultado.saludCurada} de salud por \$${resultado.costo}."
            ArticuloTienda.Escudo ->
                "Escudo activado por \$${resultado.costo}. Absorberá el próximo golpe de salud."
        }

    private fun textoRechazo(motivo: Tienda.MotivoRechazo): String = when (motivo) {
        Tienda.MotivoRechazo.SALDO_INSUFICIENTE -> "No te alcanza el dinero para esta compra."
        Tienda.MotivoRechazo.SALUD_COMPLETA -> "Tu red ya está al máximo de salud."
        Tienda.MotivoRechazo.ESCUDO_YA_ACTIVO -> "Ya tienes un escudo activo: no son apilables."
        Tienda.MotivoRechazo.RED_COMPROMETIDA ->
            "Con la red caída no puedes jugar: repara la salud antes de comprar un escudo."
    }

    private fun mensajeDeError(accion: String, e: Exception) =
        "No se pudo $accion: ${e.message ?: "error desconocido"}."
}
