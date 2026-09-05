package com.ejemplo.nettycoon.ui.panel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ejemplo.nettycoon.data.local.NetTycoonDatabase
import com.ejemplo.nettycoon.data.repository.EventoAtaqueRepository
import com.ejemplo.nettycoon.data.repository.GeoIpRepository
import com.ejemplo.nettycoon.data.repository.PartidaRepository
import com.ejemplo.nettycoon.data.repository.ReglaFirewallRepository
import com.ejemplo.nettycoon.domain.firewall.ProcesarAtaqueUseCase

/**
 * Fábrica del [PanelViewModel]. Única dueña del cableado de la capa de datos para esta feature,
 * sin introducir un framework de inyección de dependencias (mismo enfoque que
 * `AuthViewModelFactory`).
 *
 * A partir del [Context] obtiene el singleton [NetTycoonDatabase] y arma los repositorios y el
 * [ProcesarAtaqueUseCase]; el [uid] lo provee la raíz de composición, que posee la sesión.
 */
class PanelViewModelFactory(
    context: Context,
    private val uid: String,
) : ViewModelProvider.Factory {

    private val db = NetTycoonDatabase.obtenerInstancia(context.applicationContext)
    private val partidaRepo = PartidaRepository(db.estadoPartidaDao())
    private val useCase = ProcesarAtaqueUseCase(
        reglaRepo = ReglaFirewallRepository(db.reglaFirewallDao()),
        eventoRepo = EventoAtaqueRepository(db.eventoAtaqueDao()),
        partidaRepo = partidaRepo,
        geoIpRepo = GeoIpRepository(),
    )

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(PanelViewModel::class.java)) {
            "ViewModel desconocido: ${modelClass.name}"
        }
        return PanelViewModel(uid, useCase, partidaRepo) as T
    }
}
