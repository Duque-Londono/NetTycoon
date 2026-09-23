package com.ejemplo.nettycoon.ui.estadisticas

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ejemplo.nettycoon.data.local.NetTycoonDatabase
import com.ejemplo.nettycoon.data.repository.EventoAtaqueRepository
import com.ejemplo.nettycoon.data.repository.PartidaRepository

/**
 * Fábrica del [EstadisticasViewModel]. Única dueña del cableado de datos de esta feature, sin
 * introducir un framework de inyección de dependencias (mismo enfoque que
 * `ConfigRedViewModelFactory`/`FirewallViewModelFactory`).
 *
 * Del [Context] obtiene el singleton [NetTycoonDatabase] y arma el repositorio; el [uid] lo
 * provee la navegación, que es quien posee la sesión.
 */
class EstadisticasViewModelFactory(
    context: Context,
    private val uid: String,
) : ViewModelProvider.Factory {

    private val db = NetTycoonDatabase.obtenerInstancia(context.applicationContext)
    private val repositorio = EventoAtaqueRepository(db.eventoAtaqueDao())

    // El rango (E4) se deriva del puntaje, que vive en la partida: de ahí esta segunda fuente.
    // Sigue siendo una pantalla de SOLO LECTURA (no crea ni modifica la partida).
    private val partidaRepo = PartidaRepository(db.estadoPartidaDao())

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(EstadisticasViewModel::class.java)) {
            "ViewModel desconocido: ${modelClass.name}"
        }
        return EstadisticasViewModel(uid, repositorio, partidaRepo) as T
    }
}
