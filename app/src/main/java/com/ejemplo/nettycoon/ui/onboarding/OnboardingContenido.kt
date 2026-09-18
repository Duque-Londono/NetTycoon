package com.ejemplo.nettycoon.ui.onboarding

/**
 * Una página del onboarding: título y cuerpo en español.
 *
 * Los textos se centralizan aquí (constantes) para que el líder del equipo los revise sin
 * bucear en la UI. No mezclar con lógica de presentación.
 */
data class PaginaOnboarding(
    val titulo: String,
    val texto: String,
)

/**
 * Las 4 páginas del onboarding, en orden. Contenido pedagógico de primer contacto: enmarca la
 * misión del juego antes de entrar al panel.
 */
val PAGINAS_ONBOARDING: List<PaginaOnboarding> = listOf(
    PaginaOnboarding(
        titulo = "Bienvenido a NetTycoon",
        texto = "Vas a ponerte al mando de la seguridad de una red. Tu misión: proteger tu red " +
            "de los ataques que llegan de internet, decidiendo qué dejar pasar y qué bloquear. " +
            "No necesitas saber nada de redes: aquí vas a aprender jugando.",
    ),
    PaginaOnboarding(
        titulo = "Tú decides quién entra",
        texto = "Constantemente llega tráfico a tu red: algunos son usuarios legítimos, otros " +
            "son atacantes. Verás de dónde viene cada uno y qué quiere, y tú decides: ¿lo " +
            "permites o lo bloqueas? Cada decisión te enseña algo, aciertes o falles.",
    ),
    PaginaOnboarding(
        titulo = "De aprender a automatizar",
        texto = "Al principio decides ataque por ataque, a mano, para entender cómo funciona. " +
            "Cuando reconozcas un patrón, podrás crear una REGLA para que tu firewall lo haga " +
            "solo. Así trabajan los profesionales de verdad: primero entienden, luego automatizan.",
    ),
    PaginaOnboarding(
        titulo = "¿List@ para defender tu red?",
        texto = "Elige un nivel de dificultad y empieza. Puedes ir de lo más sencillo a lo más " +
            "complejo a tu ritmo. ¡Suerte, administrador!",
    ),
)
