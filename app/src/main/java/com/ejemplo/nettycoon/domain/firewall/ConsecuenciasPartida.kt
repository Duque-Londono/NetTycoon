package com.ejemplo.nettycoon.domain.firewall

import com.ejemplo.nettycoon.data.local.entity.EstadoPartida
import com.ejemplo.nettycoon.domain.model.CategoriaResultado
import com.ejemplo.nettycoon.domain.model.ResultadoEvaluacion

/**
 * Aplica las consecuencias de un [ResultadoEvaluacion] sobre el [EstadoPartida].
 *
 * Función pura: recibe el estado actual y devuelve una **copia** actualizada, sin tocar
 * Room. Las cifras viven en [BalancePartida] para poder ajustarlas sin cambiar esta lógica.
 */
object ConsecuenciasPartida {

    fun aplicar(
        estado: EstadoPartida,
        resultado: ResultadoEvaluacion,
        ahora: Long = System.currentTimeMillis(),
    ): EstadoPartida {
        var deltaPuntaje = 0
        var deltaSalud = 0
        var deltaDinero = 0

        when (resultado.categoria) {
            CategoriaResultado.BLOQUEO_CORRECTO -> {
                deltaPuntaje = BalancePartida.PUNTAJE_BLOQUEO_CORRECTO
                deltaDinero = BalancePartida.DINERO_ACIERTO
            }
            CategoriaResultado.PERMISO_CORRECTO -> {
                deltaPuntaje = BalancePartida.PUNTAJE_PERMISO_CORRECTO
                deltaDinero = BalancePartida.DINERO_ACIERTO
            }
            CategoriaResultado.BRECHA -> {
                deltaSalud = BalancePartida.SALUD_BRECHA
                deltaDinero = BalancePartida.DINERO_BRECHA
            }
            CategoriaResultado.FALSO_POSITIVO -> {
                deltaPuntaje = BalancePartida.PUNTAJE_FALSO_POSITIVO
                deltaDinero = BalancePartida.DINERO_FALSO_POSITIVO
            }
        }

        val nuevoPuntaje = (estado.puntaje + deltaPuntaje).coerceAtLeast(0)
        val nuevaSalud = (estado.saludRed + deltaSalud)
            .coerceIn(BalancePartida.SALUD_MIN, BalancePartida.SALUD_MAX)
        val nuevoDinero = (estado.dineroVirtual + deltaDinero)
            .coerceAtLeast(BalancePartida.DINERO_MIN)
        val nuevoNivel =
            (BalancePartida.NIVEL_MIN + nuevoPuntaje / BalancePartida.PUNTOS_POR_NIVEL)
                .coerceAtLeast(BalancePartida.NIVEL_MIN)

        return estado.copy(
            puntaje = nuevoPuntaje,
            saludRed = nuevaSalud,
            dineroVirtual = nuevoDinero,
            nivel = nuevoNivel,
            actualizadoEn = ahora,
        )
    }
}
