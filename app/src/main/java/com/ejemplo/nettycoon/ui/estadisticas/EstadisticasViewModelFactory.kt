package com.ejemplo.nettycoon.ui.estadisticas

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ejemplo.nettycoon.data.local.NetTycoonDatabase
import com.ejemplo.nettycoon.data.repository.EventoAtaqueRepository

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

    private val repositorio = EventoAtaqueRepository(
        NetTycoonDatabase.obtenerInstancia(context.applicationContext).eventoAtaqueDao(),
    )

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(EstadisticasViewModel::class.java)) {
            "ViewModel desconocido: ${modelClass.name}"
        }
        return EstadisticasViewModel(uid, repositorio) as T
    }
}
