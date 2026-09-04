package com.ejemplo.nettycoon.domain.firewall

import com.ejemplo.nettycoon.domain.model.Ataque
import kotlin.random.Random

/**
 * Genera [Ataque]s sintéticos para el bucle del juego. Lógica pura, sin red ni Android.
 *
 * El [random] es inyectable: con `Random(semilla)` la generación es determinista y testeable.
 *
 * **Punto de extensión (M4):** por ahora la `ipAtacante` se genera localmente (IPv4 aleatoria).
 * Cuando se integre la API geo-IP (ipwho.is) por Retrofit, esa capa enriquecerá el evento con
 * país/ISP a partir de la IP; el generador seguirá produciendo el esqueleto del ataque y no
 * hará llamadas de red (se mantiene puro y offline).
 */
class GeneradorAtaques(
    private val random: Random = Random.Default,
    private val puertosComunes: List<Int> = PUERTOS_POR_DEFECTO,
    private val probabilidadMalicioso: Double = PROBABILIDAD_MALICIOSO,
) {
    fun generar(): Ataque = Ataque(
        ipAtacante = generarIpv4(),
        puertoDestino = puertosComunes[random.nextInt(puertosComunes.size)],
        esMalicioso = random.nextDouble() < probabilidadMalicioso,
    )

    private fun generarIpv4(): String =
        (0 until 4).joinToString(".") { random.nextInt(0, 256).toString() }

    companion object {
        /** Puertos habituales de servicios (objetivos plausibles de ataque). */
        val PUERTOS_POR_DEFECTO = listOf(22, 25, 53, 80, 110, 143, 443, 3389, 8080)

        /** Proporción de ataques maliciosos (0.0–1.0). */
        const val PROBABILIDAD_MALICIOSO = 0.5
    }
}
