package com.ejemplo.nettycoon.ui.tienda

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ejemplo.nettycoon.data.local.NetTycoonDatabase
import com.ejemplo.nettycoon.data.local.prefs.PreferenciasRegen
import com.ejemplo.nettycoon.data.repository.PartidaRepository
import com.ejemplo.nettycoon.data.repository.RegeneradorSalud

/**
 * Fábrica del [TiendaViewModel]. Única dueña del cableado de datos de esta feature, sin introducir
 * un framework de inyección de dependencias (mismo enfoque que `PanelViewModelFactory`).
 *
 * La tienda solo necesita la partida y el regenerador (para que la salud esté al día antes de
 * poner precio a una cura): no toca reglas, eventos ni la API geo-IP.
 */
class TiendaViewModelFactory(
    context: Context,
    private val uid: String,
) : ViewModelProvider.Factory {

    private val db = NetTycoonDatabase.obtenerInstancia(context.applicationContext)
    private val partidaRepo = PartidaRepository(db.estadoPartidaDao())
    private val prefsRegen = PreferenciasRegen(context.applicationContext)
    private val regenerador = RegeneradorSalud(
        partidaRepo = partidaRepo,
        leerAncla = prefsRegen::anclaDe,
        guardarAncla = prefsRegen::guardarAncla,
    )

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TiendaViewModel::class.java)) {
            "ViewModel desconocido: ${modelClass.name}"
        }
        return TiendaViewModel(uid, partidaRepo, regenerador) as T
    }
}
