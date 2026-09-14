package com.ejemplo.nettycoon.ui.network

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ejemplo.nettycoon.data.local.NetTycoonDatabase
import com.ejemplo.nettycoon.data.repository.PartidaRepository

/**
 * Fábrica del [ConfigRedViewModel]. Única dueña del cableado de datos de esta feature, sin
 * introducir un framework de inyección de dependencias (mismo enfoque que
 * `FirewallViewModelFactory` y `PanelViewModelFactory`).
 *
 * Del [Context] obtiene el singleton [NetTycoonDatabase] y arma el repositorio; el [uid] lo
 * provee la navegación, que es quien posee la sesión.
 */
class ConfigRedViewModelFactory(
    context: Context,
    private val uid: String,
) : ViewModelProvider.Factory {

    private val repositorio = PartidaRepository(
        NetTycoonDatabase.obtenerInstancia(context.applicationContext).estadoPartidaDao(),
    )

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ConfigRedViewModel::class.java)) {
            "ViewModel desconocido: ${modelClass.name}"
        }
        return ConfigRedViewModel(uid, repositorio) as T
    }
}
