package com.ejemplo.nettycoon.ui.firewall

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ejemplo.nettycoon.data.local.NetTycoonDatabase
import com.ejemplo.nettycoon.data.repository.ReglaFirewallRepository

/**
 * Fábrica del [FirewallViewModel]. Única dueña del cableado de datos de esta feature, sin
 * introducir un framework de inyección de dependencias (mismo enfoque que
 * `PanelViewModelFactory` y `AuthViewModelFactory`).
 *
 * Del [Context] obtiene el singleton [NetTycoonDatabase] y arma el repositorio; el [uid] lo
 * provee la navegación, que es quien posee la sesión.
 */
class FirewallViewModelFactory(
    context: Context,
    private val uid: String,
) : ViewModelProvider.Factory {

    private val repositorio = ReglaFirewallRepository(
        NetTycoonDatabase.obtenerInstancia(context.applicationContext).reglaFirewallDao(),
    )

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(FirewallViewModel::class.java)) {
            "ViewModel desconocido: ${modelClass.name}"
        }
        return FirewallViewModel(uid, repositorio) as T
    }
}
