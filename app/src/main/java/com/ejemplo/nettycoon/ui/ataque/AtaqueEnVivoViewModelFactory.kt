package com.ejemplo.nettycoon.ui.ataque

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ejemplo.nettycoon.data.local.NetTycoonDatabase
import com.ejemplo.nettycoon.data.repository.EventoAtaqueRepository
import com.ejemplo.nettycoon.data.repository.PartidaRepository
import com.ejemplo.nettycoon.data.repository.ReglaFirewallRepository

/**
 * Fábrica del [AtaqueEnVivoViewModel]. Única dueña del cableado de datos para esta feature, sin
 * framework de inyección de dependencias (mismo enfoque que `PanelViewModelFactory`).
 *
 * A partir del [Context] obtiene el singleton [NetTycoonDatabase] y arma los repositorios; el
 * [uid] lo provee la navegación. No necesita GeoIpRepository ni el caso de uso: los escenarios de
 * esta pantalla son un catálogo fijo, no se consultan a la API geo-IP. Incluye el
 * [ReglaFirewallRepository] para poder resolver automáticamente los escenarios que casen con una
 * regla activa del jugador (puente reglas → juego).
 */
class AtaqueEnVivoViewModelFactory(
    context: Context,
    private val uid: String,
) : ViewModelProvider.Factory {

    private val db = NetTycoonDatabase.obtenerInstancia(context.applicationContext)
    private val partidaRepo = PartidaRepository(db.estadoPartidaDao())
    private val eventoRepo = EventoAtaqueRepository(db.eventoAtaqueDao())
    private val reglaRepo = ReglaFirewallRepository(db.reglaFirewallDao())

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AtaqueEnVivoViewModel::class.java)) {
            "ViewModel desconocido: ${modelClass.name}"
        }
        return AtaqueEnVivoViewModel(uid, partidaRepo, eventoRepo, reglaRepo) as T
    }
}
