package com.ejemplo.nettycoon.domain.firewall

import com.ejemplo.nettycoon.data.local.entity.ReglaFirewall
import com.ejemplo.nettycoon.domain.model.ReglaEvaluable

/**
 * Traducción entidad Room → modelo de dominio para alimentar el [MotorFirewall].
 *
 * Es una función pura (sin Room/Android), fácil de testear en JVM. Mantiene la lógica de
 * dominio fuera del repositorio (que conserva su sentido CRUD).
 */

/** Convierte una [ReglaFirewall] en su [ReglaEvaluable] de dominio (descarta id/owner/creadaEn). */
fun ReglaFirewall.aEvaluable(): ReglaEvaluable =
    ReglaEvaluable(puerto = puerto, ip = ip, accion = accion)

/**
 * Convierte una lista de reglas a evaluables, quedándose solo con las **activas**.
 *
 * Como defensa extra se filtra `activa` aquí aunque la fuente ya suela traer solo activas.
 */
fun List<ReglaFirewall>.aReglasEvaluables(): List<ReglaEvaluable> =
    filter { it.activa }.map { it.aEvaluable() }
