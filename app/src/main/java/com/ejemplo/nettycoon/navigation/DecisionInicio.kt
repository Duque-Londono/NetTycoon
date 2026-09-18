package com.ejemplo.nettycoon.navigation

/**
 * Lógica **pura** de decisión del destino inicial del grafo (el "gate").
 *
 * Se extrae aquí, sin dependencias de Android ni de Compose, para poder tocar el gate de
 * navegación con una tabla de verdad cubierta por pruebas JVM, sin arriesgar el gate de auth.
 *
 * Reglas:
 * - Sin sesión → siempre [Rutas.Login] (el gate de auth manda; el onboarding nunca se muestra
 *   antes de identificarse).
 * - Con sesión y onboarding **no** visto → [Rutas.Onboarding] (primer contacto).
 * - Con sesión y onboarding ya visto → [Rutas.Home] (comportamiento de siempre).
 */
fun decidirDestinoInicial(haySesion: Boolean, onboardingVisto: Boolean): String = when {
    !haySesion -> Rutas.Login.ruta
    !onboardingVisto -> Rutas.Onboarding.ruta
    else -> Rutas.Home.ruta
}

/**
 * Destino tras un login/registro/bypass **exitoso** (ya hay sesión, por definición de estas vías).
 *
 * Las TRES vías de entrada al juego usan esta misma helper para que el comportamiento del
 * onboarding sea idéntico por cualquiera de ellas y no existan flujos divergentes. Es un caso
 * particular de [decidirDestinoInicial] con `haySesion = true`.
 */
fun destinoPostLogin(onboardingVisto: Boolean): String =
    decidirDestinoInicial(haySesion = true, onboardingVisto = onboardingVisto)
