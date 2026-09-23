package com.ejemplo.nettycoon.ui.firewall

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ejemplo.nettycoon.data.local.NetTycoonDatabase
import com.ejemplo.nettycoon.data.repository.PartidaRepository
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

    private val db = NetTycoonDatabase.obtenerInstancia(context.applicationContext)
    private val repositorio = ReglaFirewallRepository(db.reglaFirewallDao())

    // El cupo de reglas activas (E5) se deriva del rango, y el rango del puntaje de la partida:
    // de ahí esta segunda fuente. La pantalla no crea ni modifica la partida, solo la lee.
    private val partidaRepo = PartidaRepository(db.estadoPartidaDao())

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(FirewallViewModel::class.java)) {
            "ViewModel desconocido: ${modelClass.name}"
        }
        return FirewallViewModel(uid, repositorio, partidaRepo) as T
    }
}
