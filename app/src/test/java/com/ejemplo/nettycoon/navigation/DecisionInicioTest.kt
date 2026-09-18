package com.ejemplo.nettycoon.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Cubre la tabla de verdad del gate ([decidirDestinoInicial]) y la helper de post-login
 * ([destinoPostLogin]). Es lógica pura, sin Android, testeable en JVM.
 */
class DecisionInicioTest {

    @Test
    fun `sin sesion siempre va a login aunque no haya visto onboarding`() {
        assertEquals(Rutas.Login.ruta, decidirDestinoInicial(haySesion = false, onboardingVisto = false))
    }

    @Test
    fun `sin sesion va a login aunque ya haya visto onboarding`() {
        assertEquals(Rutas.Login.ruta, decidirDestinoInicial(haySesion = false, onboardingVisto = true))
    }

    @Test
    fun `con sesion y onboarding no visto va a onboarding`() {
        assertEquals(Rutas.Onboarding.ruta, decidirDestinoInicial(haySesion = true, onboardingVisto = false))
    }

    @Test
    fun `con sesion y onboarding visto va a home`() {
        assertEquals(Rutas.Home.ruta, decidirDestinoInicial(haySesion = true, onboardingVisto = true))
    }

    @Test
    fun `post-login sin haber visto onboarding va a onboarding`() {
        assertEquals(Rutas.Onboarding.ruta, destinoPostLogin(onboardingVisto = false))
    }

    @Test
    fun `post-login habiendo visto onboarding va a home`() {
        assertEquals(Rutas.Home.ruta, destinoPostLogin(onboardingVisto = true))
    }
}
