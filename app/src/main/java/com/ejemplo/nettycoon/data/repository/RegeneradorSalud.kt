package com.ejemplo.nettycoon.data.repository

import com.ejemplo.nettycoon.domain.firewall.RegenSalud

/**
 * Aplica la regeneración de salud (E2) al ENTRAR a una pantalla que muestra la partida (Panel /
 * Ataque en vivo): lee la partida, recalcula la salud con [RegenSalud] a partir del ancla guardada
 * y el tiempo real, y —si cambió— persiste la salud nueva en Room y el ancla nueva en prefs.
 *
 * **Convivencia con el daño de E1:** el ancla vive en prefs, **independiente de las escrituras de
 * Room** de `ConsecuenciasPartida`; E1 no toca el ancla, así que la regen sigue contando desde el
 * último paso consumido. La regen corre una sola vez por entrada (en el `init` del ViewModel),
 * mientras que el daño ocurre durante el juego, por lo que no hay lectura-modificación-escritura
 * concurrente sobre la misma fila.
 *
 * Las dependencias de tiempo y de persistencia del ancla se inyectan como funciones (mismo estilo
 * que `AtaqueEnVivoViewModel.seleccionarSiguiente`) para que sea determinista y testeable en JVM
 * con fakes, sin acoplarse a Android.
 */
class RegeneradorSalud(
    private val partidaRepo: PartidaRepository,
    private val leerAncla: (uid: String, porDefecto: Long) -> Long,
    private val guardarAncla: (uid: String, millis: Long) -> Unit,
    private val reloj: () -> Long = System::currentTimeMillis,
) {
    /**
     * Recalcula y persiste la regeneración para el [uid]. Garantiza la existencia de la fila
     * (vía `getOrCreatePartida`) antes de leerla.
     */
    suspend fun aplicar(uid: String) {
        val partida = partidaRepo.getOrCreatePartida(uid)
        val ahora = reloj()
        val ancla = leerAncla(uid, ahora)
        val resultado = RegenSalud.calcular(partida.saludRed, ancla, ahora)
        if (resultado.saludNueva != partida.saludRed) {
            partidaRepo.actualizarPartida(partida.copy(saludRed = resultado.saludNueva))
        }
        guardarAncla(uid, resultado.anclaNueva)
    }

    /**
     * Ancla vigente para el [uid] (para derivar el contador de la próxima recuperación, E2.1). Si
     * aún no hay ninguna guardada, devuelve el instante actual (tramo recién empezado). Es solo
     * lectura: no modifica el estado ni el modelo de regeneración.
     */
    fun anclaActual(uid: String): Long = leerAncla(uid, reloj())
}
