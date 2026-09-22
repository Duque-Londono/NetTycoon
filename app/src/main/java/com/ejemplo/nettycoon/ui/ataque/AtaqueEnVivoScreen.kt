package com.ejemplo.nettycoon.ui.ataque

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ejemplo.nettycoon.data.local.entity.AccionFirewall
import com.ejemplo.nettycoon.data.local.entity.EstadoPartida
import com.ejemplo.nettycoon.domain.firewall.MapeoFamilias
import com.ejemplo.nettycoon.domain.model.CategoriaResultado
import com.ejemplo.nettycoon.ui.componentes.ContadorRegen
import com.ejemplo.nettycoon.ui.componentes.EstadoSalud
import com.ejemplo.nettycoon.ui.componentes.MedidorSalud
import com.ejemplo.nettycoon.ui.theme.Espaciado
import com.ejemplo.nettycoon.ui.theme.LocalColoresJuego
import com.ejemplo.nettycoon.ui.theme.NetTycoonTheme
import com.ejemplo.nettycoon.ui.theme.TipografiaDatosTecnicos

/**
 * Pantalla "Ataque en vivo" (FASE 1 de UX pedagógica): invierte el bucle para que el jugador
 * APRENDA. Llega un escenario explicado en lenguaje humano, el jugador DECIDE permitir/bloquear
 * y recibe feedback inmediato con una lección, acierte o falle.
 *
 * Punto de entrada con estado (conectado al ViewModel).
 */
@Composable
fun AtaqueEnVivoScreen(
    viewModel: AtaqueEnVivoViewModel,
    onIrAReglas: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    AtaqueEnVivoScreen(
        estado = estado,
        onElegirNivel = viewModel::elegirNivel,
        onCambiarNivel = viewModel::cambiarNivel,
        onReciclarNivel = viewModel::reciclarNivel,
        onPermitir = viewModel::onPermitir,
        onBloquear = viewModel::onBloquear,
        onSiguienteAtaque = viewModel::onSiguienteAtaque,
        onLimpiarError = viewModel::limpiarError,
        onAutomatizarPuerto = viewModel::automatizarPuerto,
        onAutomatizarFamilia = viewModel::automatizarFamilia,
        onLimpiarAvisoReglas = viewModel::limpiarAvisoReglas,
        onRefrescarRegen = viewModel::refrescarRegen,
        onIrAReglas = onIrAReglas,
        onVolver = onVolver,
        modifier = modifier,
    )
}

/** Versión sin estado (stateless) para previews y separación de responsabilidades. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtaqueEnVivoScreen(
    estado: AtaqueEnVivoUiState,
    onElegirNivel: (Dificultad) -> Unit,
    onCambiarNivel: () -> Unit,
    onReciclarNivel: () -> Unit,
    onPermitir: () -> Unit,
    onBloquear: () -> Unit,
    onSiguienteAtaque: () -> Unit,
    onLimpiarError: () -> Unit,
    onAutomatizarPuerto: () -> Unit,
    onAutomatizarFamilia: () -> Unit,
    onLimpiarAvisoReglas: () -> Unit,
    onRefrescarRegen: () -> Unit,
    onIrAReglas: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Ataque en vivo") },
                navigationIcon = {
                    TextButton(onClick = onVolver) { Text("Volver") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Espaciado.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Espaciado.md),
        ) {
            when {
                // Candado E2: red comprometida (salud <= 0). Bloquea SOLO esta pantalla; el
                // jugador puede volver y gestionar el resto de la app mientras la salud se
                // regenera sola con el tiempo.
                estado.comprometida -> TarjetaRedComprometida(
                    salud = estado.partida?.saludRed ?: 0,
                    anclaRegen = estado.anclaRegen,
                    onRefrescarRegen = onRefrescarRegen,
                )

                // Selector de nivel: aún no se ha elegido dificultad.
                estado.nivel == null -> SelectorNivel(onElegirNivel = onElegirNivel)

                // Nivel completado: se agotaron los escenarios del nivel.
                estado.nivelCompletado -> {
                    CabeceraNivel(nivel = estado.nivel, onCambiarNivel = onCambiarNivel)
                    TarjetaContador(aciertos = estado.aciertos, rondas = estado.rondas)
                    estado.partida?.let { TarjetaSalud(salud = it.saludRed) }
                    TarjetaNivelCompletado(
                        aciertos = estado.aciertos,
                        rondas = estado.rondas,
                        onReciclarNivel = onReciclarNivel,
                        onCambiarNivel = onCambiarNivel,
                    )
                }

                // Jugando: hay un escenario que decidir.
                estado.escenario != null -> {
                    val escenario = estado.escenario
                    // Modo "pelado" del examen final: en Imposible se apaga TODO el material
                    // educativo (servicio, situación, pista, lección, "¿Por qué?"). Solo afecta a
                    // este nivel; 1-3 quedan intactos.
                    val pelado = estado.nivel == Dificultad.IMPOSIBLE
                    CabeceraNivel(nivel = estado.nivel, onCambiarNivel = onCambiarNivel)
                    TarjetaContador(aciertos = estado.aciertos, rondas = estado.rondas)
                    // Salud de la red en contexto: el medidor anima el daño en cuanto saludRed baja
                    // (hoy solo ocurre en una BRECHA, ver diagnóstico C2). El delta se muestra tras
                    // decidir, ligado al deltaSalud REAL del veredicto.
                    estado.partida?.let {
                        TarjetaSalud(
                            salud = it.saludRed,
                            deltaSalud = estado.ultimoResultado?.deltaSalud ?: 0,
                        )
                    }

                    TarjetaSituacion(escenario = escenario, pelado = pelado)
                    if (!pelado) TarjetaPista(pista = escenario.textoPista)

                    if (estado.cargando) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    estado.error?.let { mensaje ->
                        TarjetaError(mensaje = mensaje, onLimpiarError = onLimpiarError)
                    }

                    estado.avisoReglas?.let { aviso ->
                        TarjetaAvisoReglas(mensaje = aviso, onLimpiar = onLimpiarAvisoReglas)
                    }

                    val resultado = estado.ultimoResultado
                    when {
                        // Comprobando si una regla del jugador resuelve el escenario (antes de decidir).
                        estado.evaluandoRegla && resultado == null -> IndicadorEvaluandoRegla()

                        // Aún sin decidir: se pregunta a mano.
                        resultado == null -> BotonesDecision(
                            habilitado = !estado.cargando && estado.partida != null,
                            onPermitir = onPermitir,
                            onBloquear = onBloquear,
                        )

                        // La ronda la resolvió una regla del jugador.
                        resultado.automatizada -> {
                            TarjetaAutomatizada(
                                resultado = resultado,
                                explicacion = escenario.explicacionAmpliada,
                                pelado = pelado,
                            )
                            BotonSiguiente(onSiguienteAtaque = onSiguienteAtaque)
                        }

                        // Decisión manual: veredicto (+ posible sugerencia del puente).
                        else -> {
                            TarjetaVeredicto(
                                resultado = resultado,
                                explicacion = escenario.explicacionAmpliada,
                                pelado = pelado,
                            )
                            resultado.sugerencia?.let { sugerencia ->
                                TarjetaSugerencia(
                                    sugerencia = sugerencia,
                                    onAutomatizarPuerto = onAutomatizarPuerto,
                                    onAutomatizarFamilia = onAutomatizarFamilia,
                                    onIrAReglas = onIrAReglas,
                                )
                            }
                            BotonSiguiente(onSiguienteAtaque = onSiguienteAtaque)
                        }
                    }
                }
            }
        }
    }
}

/** Nombre humano de cada nivel para mostrar al jugador. */
private fun Dificultad.etiqueta(): String = when (this) {
    Dificultad.FACIL -> "Fácil"
    Dificultad.MEDIO -> "Medio"
    Dificultad.DIFICIL -> "Difícil"
    Dificultad.IMPOSIBLE -> "Imposible"
}

/**
 * Estilo visual de un chip de nivel. El nivel es META-INFO, no una acción: por eso NO usa los roles
 * de las decisiones (primary/cian = Permitir, secondary/índigo = Bloquear). La progresión se
 * transmite por INTENSIDAD dentro de una única familia neutra (violeta/tertiary → outline severo):
 * Fácil el más tenue, subiendo hasta Imposible como chip severo perfilado (sin rojo).
 */
private data class EstiloChipNivel(
    val contenedor: Color,
    val contenido: Color,
    val borde: BorderStroke?,
)

@Composable
private fun estiloChipNivel(nivel: Dificultad): EstiloChipNivel {
    val esquema = MaterialTheme.colorScheme
    return when (nivel) {
        Dificultad.FACIL -> EstiloChipNivel(
            contenedor = esquema.tertiaryContainer.copy(alpha = 0.35f),
            contenido = esquema.onSurfaceVariant,
            borde = null,
        )
        Dificultad.MEDIO -> EstiloChipNivel(
            contenedor = esquema.tertiaryContainer.copy(alpha = 0.6f),
            contenido = esquema.onTertiaryContainer,
            borde = null,
        )
        Dificultad.DIFICIL -> EstiloChipNivel(
            contenedor = esquema.tertiaryContainer,
            contenido = esquema.onTertiaryContainer,
            borde = BorderStroke(1.dp, esquema.outlineVariant),
        )
        // Examen final: chip severo, perfilado y sin relleno (el más intenso, nunca rojo).
        Dificultad.IMPOSIBLE -> EstiloChipNivel(
            contenedor = Color.Transparent,
            contenido = esquema.onSurface,
            borde = BorderStroke(1.5.dp, esquema.outline),
        )
    }
}

/** Chip reutilizable con la etiqueta del nivel; acento por intensidad (ver [estiloChipNivel]). */
@Composable
private fun ChipNivel(nivel: Dificultad, modifier: Modifier = Modifier) {
    val estilo = estiloChipNivel(nivel)
    Surface(
        modifier = modifier,
        color = estilo.contenedor,
        contentColor = estilo.contenido,
        shape = MaterialTheme.shapes.small,
        border = estilo.borde,
    ) {
        Text(
            text = nivel.etiqueta(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = Espaciado.sm, vertical = Espaciado.xs),
        )
    }
}

/** Chip neutro para meta-info textual (p. ej. la familia del patrón), fuera de los roles de acción. */
@Composable
private fun ChipInfo(texto: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = Espaciado.sm, vertical = Espaciado.xs),
        )
    }
}

/**
 * Candado E2: la red está COMPROMETIDA (salud <= 0). Bloquea el juego en esta pantalla con un
 * mensaje claro; el resto de la app sigue navegable y la salud se regenera sola con el tiempo.
 * Reutiliza el color `danger` y la etiqueta de [EstadoSalud.COMPROMETIDA] (coherencia visual).
 */
@Composable
private fun TarjetaRedComprometida(
    salud: Int,
    anclaRegen: Long?,
    onRefrescarRegen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = LocalColoresJuego.current
    // Etiqueta reutilizada del estado presentacional de salud ya existente.
    val etiqueta = when (EstadoSalud.COMPROMETIDA) {
        EstadoSalud.SEGURA -> "Segura"
        EstadoSalud.EN_RIESGO -> "En riesgo"
        EstadoSalud.COMPROMETIDA -> "Comprometida"
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colores.danger),
    ) {
        Column(
            modifier = Modifier.padding(Espaciado.md),
            verticalArrangement = Arrangement.spacedBy(Espaciado.sm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Espaciado.sm),
            ) {
                Icon(
                    imageVector = Icons.Filled.Cancel,
                    contentDescription = null,
                    tint = colores.onDanger,
                )
                Text(
                    "Red $etiqueta",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colores.onDanger,
                )
            }
            // COPY BORRADOR (validación del equipo).
            Text(
                "Tu red está comprometida. Se recupera sola con el tiempo. Mientras tanto no " +
                    "puedes jugar ataques, pero sí revisar tus reglas, la configuración de red y tu progreso.",
                style = MaterialTheme.typography.bodyMedium,
                color = colores.onDanger,
            )
            // Cuenta atrás hasta poder volver a jugar (el próximo +5 saca la salud de 0). Al
            // cumplirse, reaplica la regen y la pantalla se desbloquea sola.
            if (anclaRegen != null) {
                ContadorRegen(
                    salud = salud,
                    ancla = anclaRegen,
                    onPasoPendiente = onRefrescarRegen,
                    prefijo = "Jugable en",
                    color = colores.onDanger,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

/**
 * Selector inicial: el jugador elige el nivel. Cada opción trae una línea que explica, en lenguaje
 * de novato, qué tipo de casos verá, para que la elección sea informada y no a ciegas.
 */
@Composable
private fun SelectorNivel(onElegirNivel: (Dificultad) -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Espaciado.md),
            verticalArrangement = Arrangement.spacedBy(Espaciado.sm + Espaciado.xs),
        ) {
            Text(
                "Elige tu nivel",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Empieza por el nivel que quieras. Dentro de cada uno, los casos van de lo más " +
                    "claro a lo más engañoso a medida que avanzas.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OpcionNivel(
                nivel = Dificultad.FACIL,
                descripcion = "Casos claros para empezar: distingue lo normal de lo peligroso.",
                onClick = { onElegirNivel(Dificultad.FACIL) },
            )
            OpcionNivel(
                nivel = Dificultad.MEDIO,
                descripcion = "Servicios sensibles y protocolos viejos: ya toca pensar un poco.",
                onClick = { onElegirNivel(Dificultad.MEDIO) },
            )
            OpcionNivel(
                nivel = Dificultad.DIFICIL,
                descripcion = "Casos con trampa: lo que parece sospechoso puede ser legítimo (y al revés).",
                onClick = { onElegirNivel(Dificultad.DIFICIL) },
            )
            // COPY BORRADOR (validación del equipo): descripción + advertencia del nivel Imposible.
            OpcionNivel(
                nivel = Dificultad.IMPOSIBLE,
                descripcion = "Sin ayudas: solo puerto e IP/país/ISP en crudo. Decides a ciegas.",
                onClick = { onElegirNivel(Dificultad.IMPOSIBLE) },
            )
            Text(
                "⚠️ Examen final: sin servicio, sin pista, sin explicación.",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * Tarjeta de opción de nivel. El acento va en el [ChipNivel] (meta-info por intensidad), no en el
 * relleno de la tarjeta: así el color del nivel nunca compite con el cian/índigo de las acciones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpcionNivel(
    nivel: Dificultad,
    descripcion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Espaciado.md),
            verticalArrangement = Arrangement.spacedBy(Espaciado.sm),
        ) {
            ChipNivel(nivel = nivel)
            Text(descripcion, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Cabecera con el nivel actual y un botón para volver al selector. */
@Composable
private fun CabeceraNivel(
    nivel: Dificultad,
    onCambiarNivel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Espaciado.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Nivel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            ChipNivel(nivel = nivel)
        }
        TextButton(onClick = onCambiarNivel) { Text("Cambiar nivel") }
    }
}

/** Aviso al agotar el nivel: felicita y ofrece repetir o cambiar de nivel (no en silencio). */
@Composable
private fun TarjetaNivelCompletado(
    aciertos: Int,
    rondas: Int,
    onReciclarNivel: () -> Unit,
    onCambiarNivel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Espaciado.md),
            verticalArrangement = Arrangement.spacedBy(Espaciado.sm),
        ) {
            Text(
                "🎉 ¡Completaste este nivel!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Terminaste todos los casos del nivel con $aciertos aciertos de $rondas.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Espaciado.md),
            ) {
                Button(onClick = onReciclarNivel, modifier = Modifier.weight(1f)) {
                    Text("Repetir nivel")
                }
                Button(
                    onClick = onCambiarNivel,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                ) {
                    Text("Cambiar nivel")
                }
            }
        }
    }
}

@Composable
private fun TarjetaContador(aciertos: Int, rondas: Int, modifier: Modifier = Modifier) {
    // El número sube animado (no salta) cuando cambian aciertos/rondas tras una decisión.
    val aciertosAnimados by animateIntAsState(targetValue = aciertos, label = "aciertos")
    val rondasAnimadas by animateIntAsState(targetValue = rondas, label = "rondas")
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Espaciado.md),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Tu progreso", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Aciertos: $aciertosAnimados / $rondasAnimadas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * Salud de la red en contexto durante "Ataque en vivo". Reutiliza [MedidorSalud] (fase B), que ya
 * anima el llenado/color cuando [salud] cambia — así el jugador ve el "golpe" al recibir daño.
 *
 * [deltaSalud] es el efecto de la ÚLTIMA decisión (0 si no aplica). Cuando es distinto de cero se
 * muestra un badge "-X / +X" con fade-in, en el color semántico ([LocalColoresJuego]). Va ligado al
 * delta REAL del veredicto: si un fallo no toca la salud (falso positivo) o es un acierto, no
 * aparece — es lo esperado dado el estado actual del dominio.
 */
@Composable
private fun TarjetaSalud(
    salud: Int,
    modifier: Modifier = Modifier,
    deltaSalud: Int = 0,
) {
    val colores = LocalColoresJuego.current
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Espaciado.md),
            verticalArrangement = Arrangement.spacedBy(Espaciado.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Salud de la red",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                AnimatedVisibility(visible = deltaSalud != 0) {
                    val positivo = deltaSalud > 0
                    Surface(
                        color = if (positivo) colores.success else colores.danger,
                        contentColor = if (positivo) colores.onSuccess else colores.onDanger,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            text = conSigno(deltaSalud),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                horizontal = Espaciado.sm,
                                vertical = Espaciado.xs,
                            ),
                        )
                    }
                }
            }
            MedidorSalud(salud = salud)
        }
    }
}

/**
 * Tarjeta de situación. En modo [pelado] (nivel Imposible) se apaga el material educativo: sin
 * texto de situación ni fila "Servicio", solo el contexto EN CRUDO (puerto + IP + país + ISP), sin
 * interpretar ni etiquetar. En los niveles 1-3 se muestra completa como siempre.
 */
@Composable
private fun TarjetaSituacion(
    escenario: EscenarioAtaque,
    pelado: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Espaciado.md),
            verticalArrangement = Arrangement.spacedBy(Espaciado.sm),
        ) {
            if (pelado) {
                // COPY BORRADOR (validación del equipo): encabezado de la ronda pelada.
                Text(
                    "Contexto en crudo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Text("Situación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(escenario.textoSituacion, style = MaterialTheme.typography.bodyLarge)
                FilaDato("Servicio", escenario.servicioNombre)
            }
            // Datos técnicos en monoespaciado (se sienten "de consola"). Aplica también en pelado.
            FilaDatoTecnico("Puerto", escenario.puerto.toString())
            FilaDatoTecnico("IP origen", escenario.ipAtacante)
            FilaDatoTecnico("País", escenario.pais)
            FilaDatoTecnico("ISP", escenario.isp)
        }
    }
}

@Composable
private fun TarjetaPista(pista: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Espaciado.md),
            verticalArrangement = Arrangement.spacedBy(Espaciado.xs + 2.dp),
        ) {
            Text("💡 Pista", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(pista, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * Dos botones grandes de igual peso visual. Guarda pedagógica del color: Permitir = primary (cian)
 * y Bloquear = secondary (índigo); NEUTROS y distintos, nunca verde/rojo. Ambas decisiones pueden
 * ser la correcta según el caso, así que sesgar el color (verde=bien, rojo=mal) enseñaría lo
 * contrario. El verde/rojo (success/danger) queda reservado al RESULTADO y a la salud (fase C2).
 */
@Composable
private fun BotonesDecision(
    habilitado: Boolean,
    onPermitir: () -> Unit,
    onBloquear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Espaciado.md),
    ) {
        Button(
            onClick = onPermitir,
            enabled = habilitado,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text("Permitir", style = MaterialTheme.typography.titleMedium)
        }
        Button(
            onClick = onBloquear,
            enabled = habilitado,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
        ) {
            Text("Bloquear", style = MaterialTheme.typography.titleMedium)
        }
    }
}

/**
 * Veredicto. El tratamiento visual depende SOLO de [ResultadoDecision.acierto]: un permiso
 * correcto se celebra igual que un bloqueo correcto (dejar pasar tráfico legítimo es tan valioso
 * como frenar un ataque).
 *
 * Fase C2 — la "vida" de la tarjeta:
 * - Aparece con fade + scale-in ([AnimatedVisibility] con estado disparado al montarse).
 * - Icono de resultado (check/cross) con un pequeño scale-in de entrada.
 * - AQUÍ SÍ entran los colores de RESULTADO: acierto = success (verde) / fallo = danger (rojo) de
 *   [LocalColoresJuego]. Es el único lugar (junto con la salud) donde va verde/rojo; las acciones
 *   Permitir/Bloquear siguen en cian/índigo (la guarda pedagógica queda intacta).
 */
@Composable
private fun TarjetaVeredicto(
    resultado: ResultadoDecision,
    explicacion: String,
    pelado: Boolean,
    modifier: Modifier = Modifier,
) {
    val colores = LocalColoresJuego.current
    val contenedor = if (resultado.acierto) colores.success else colores.danger
    val contenido = if (resultado.acierto) colores.onSuccess else colores.onDanger

    // Aparición: el estado arranca en false y pasa a true al primer frame → dispara fade + scale-in.
    val estadoAparicion = remember { MutableTransitionState(false) }.apply { targetState = true }

    AnimatedVisibility(
        visibleState = estadoAparicion,
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = contenedor,
                contentColor = contenido,
            ),
            // Flash sutil: borde en el color semántico para reforzar el resultado sin parpadeos.
            border = BorderStroke(1.dp, contenido.copy(alpha = 0.35f)),
        ) {
            Column(
                modifier = Modifier.padding(Espaciado.md),
                verticalArrangement = Arrangement.spacedBy(Espaciado.sm),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Espaciado.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconoResultado(acierto = resultado.acierto)
                    Text(
                        if (resultado.acierto) "¡Bien hecho!" else "Cuidado",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                // En Imposible se conserva el MARCADOR (acierto + efecto en la red) pero se apaga el
                // material educativo: sin lección ni "¿Por qué?".
                if (!pelado) Text(resultado.leccion, style = MaterialTheme.typography.bodyLarge)

                // E3: si el escudo cubrió el golpe, se dice APARTE del veredicto y DESPUÉS de la
                // lección, nunca en su lugar. El mensaje de arriba ("Cuidado" + rojo + lección)
                // sigue diciendo que el jugador falló; este solo añade que sobrevivió esta vez.
                if (resultado.escudoAbsorbio) AvisoEscudoAbsorbio()

                Text(
                    "Efecto en tu red",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                FilaDato("Puntaje", conSigno(resultado.deltaPuntaje))
                FilaDato(
                    "Salud de la red",
                    if (resultado.escudoAbsorbio) {
                        "Sin cambio (escudo)"
                    } else {
                        conSigno(resultado.deltaSalud)
                    },
                )
                FilaDato("Dinero virtual", conSigno(resultado.deltaDinero))

                if (!pelado) SeccionPorQue(explicacion = explicacion)
            }
        }
    }
}

/**
 * Aviso de que el escudo comprado en la tienda (E3) absorbió el golpe de salud.
 *
 * Va DENTRO de la tarjeta de veredicto y por eso hereda su color: si el jugador falló, sigue
 * leyéndose sobre el rojo de error. Es deliberado — son DOS mensajes distintos y ninguno anula al
 * otro: el veredicto dice que **falló** (icono, "Cuidado", color y lección intactos) y este bloque
 * dice que **esta vez no lo pagó con salud**. Nunca debe poder leerse como un acierto, así que el
 * texto nombra el error explícitamente y recuerda que el escudo ya se gastó.
 *
 * Se muestra también en el nivel Imposible (no es material educativo que ese nivel apague, sino un
 * hecho sobre el estado de la partida, igual que la sección "Efecto en tu red").
 */
@Composable
private fun AvisoEscudoAbsorbio(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, LocalContentColor.current.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier.padding(Espaciado.sm),
            verticalArrangement = Arrangement.spacedBy(Espaciado.xs),
        ) {
            Text(
                "🛡️ Escudo absorbió el golpe",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "La decisión siguió siendo equivocada y el coste en dinero y puntaje se aplica " +
                    "igual; lo único que cambia es que tu salud no bajó. El escudo era de un solo " +
                    "uso: ya se gastó.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * Icono de resultado del veredicto: check (acierto) / cross (fallo). Hace un breve scale-in de
 * entrada (rebote leve) para dar vida al veredicto. Hereda el color del contenido de la tarjeta.
 */
@Composable
private fun IconoResultado(acierto: Boolean, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val escala by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        label = "escalaIconoResultado",
    )
    Icon(
        imageVector = if (acierto) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
        contentDescription = if (acierto) "Acierto" else "Fallo",
        modifier = modifier
            .size(28.dp)
            .scale(escala),
    )
}

/**
 * Sección "¿Por qué?": profundidad OPCIONAL y no intrusiva. Colapsada por defecto; al pulsar
 * despliega in-place (con [AnimatedVisibility], sin diálogo ni nueva pantalla) la explicación
 * ampliada del concepto de ciberseguridad. Si [explicacion] está vacía, no se muestra nada (p. ej.
 * escenarios de test/preview sin explicación redactada).
 *
 * El estado expandido es de composición (UI local): se recuerda con clave [explicacion] para
 * COLAPSARSE automáticamente al cambiar de escenario, sin necesidad de tocar el ViewModel.
 */
@Composable
private fun SeccionPorQue(explicacion: String, modifier: Modifier = Modifier) {
    if (explicacion.isBlank()) return

    var expandido by remember(explicacion) { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        TextButton(
            onClick = { expandido = !expandido },
            modifier = Modifier.align(Alignment.Start),
        ) {
            Text(if (expandido) "▾ ¿Por qué?" else "▸ ¿Por qué?")
        }
        AnimatedVisibility(visible = expandido) {
            Text(
                text = explicacion,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Indicador breve mientras se comprueba si una regla del jugador resuelve el escenario. */
@Composable
private fun IndicadorEvaluandoRegla(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator()
        Text("Comprobando tus reglas…", style = MaterialTheme.typography.bodyMedium)
    }
}

/** Botón para avanzar al siguiente escenario (común a rondas manuales y automatizadas). */
@Composable
private fun BotonSiguiente(onSiguienteAtaque: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onSiguienteAtaque, modifier = modifier.fillMaxWidth()) {
        Text("Siguiente ataque")
    }
}

/**
 * Tarjeta "🤖 Automatizado por tu regla": la ronda la resolvió una regla activa del jugador, no una
 * decisión manual. Es intencionadamente DISTINTA del veredicto manual (encabezado y tono propios,
 * sin sección "Efecto en tu red" porque no afecta métricas) para que el jugador NOTE que su regla
 * trabajó por él. Muestra qué regla actuó, cómo se resolvió el tráfico y si acertó o abrió brecha.
 */
@Composable
private fun TarjetaAutomatizada(
    resultado: ResultadoDecision,
    explicacion: String,
    pelado: Boolean,
    modifier: Modifier = Modifier,
) {
    val automatizacion = resultado.automatizadaPor ?: return
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Espaciado.md),
            verticalArrangement = Arrangement.spacedBy(Espaciado.sm),
        ) {
            Text(
                "🤖 Automatizado por tu regla",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Tu firewall decidió solo con una regla que creaste: no tuviste que intervenir.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Text(
                "La regla que actuó",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            FilaDatoTecnico("Puerto", automatizacion.puerto.toString())
            FilaDatoTecnico("IP", automatizacion.ip ?: "Cualquiera")
            FilaDato("Acción", automatizacion.accionTexto)
            FilaDato(
                "Resultado",
                if (resultado.resultadoEvento == com.ejemplo.nettycoon.data.local.entity.ResultadoEvento.BLOQUEADO) {
                    "Bloqueado"
                } else {
                    "Permitido"
                },
            )

            // Estado claro y visible: acierto vs. brecha/falso positivo (aunque no afecte métricas).
            Text(
                if (resultado.acierto) "✅ Tu regla acertó" else "❌ Tu regla falló",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            // En Imposible se apaga la lección y el "¿Por qué?"; el resto (qué regla actuó y el
            // resultado) se conserva para que el jugador vea que su regla trabajó.
            if (!pelado) Text(resultado.leccion, style = MaterialTheme.typography.bodyLarge)
            Text(
                "Las rondas automáticas no suman puntaje: el puntaje premia decidir a mano.",
                style = MaterialTheme.typography.bodySmall,
            )

            if (!pelado) SeccionPorQue(explicacion = explicacion)
        }
    }
}

/**
 * Tarjeta del puente hacia las reglas. Tono distinto del veredicto (que celebra o corrige) y de la
 * pista (que informa antes de decidir): usa el color terciario para sentirse como un
 * aprendizaje/mejora, no como un error.
 *
 * Ofrece dos caminos ACCIONABLES ("Precisión vs. comodidad"):
 * - **Solo el puerto**: crea una regla precisa y segura para el puerto exacto.
 * - **Toda la familia**: crea en lote las reglas de la familia; cómodo pero tosco, con advertencia
 *   explícita de que se aplicará también a tráfico futuro (incluidas amenazas disfrazadas).
 *
 * Los COPYS son BORRADOR para validación del equipo (no prosa educativa extensa).
 */
@Composable
private fun TarjetaSugerencia(
    sugerencia: SugerenciaRegla,
    onAutomatizarPuerto: () -> Unit,
    onAutomatizarFamilia: () -> Unit,
    onIrAReglas: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Espaciado.md),
            verticalArrangement = Arrangement.spacedBy(Espaciado.sm + Espaciado.xs),
        ) {
            // COPY BORRADOR (validación del equipo): título de la tarjeta.
            Text(
                "🎓 Ya dominas esta familia: ¿la automatizas?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            // Chip neutro con la familia del patrón (meta-info, no una acción).
            ChipInfo(texto = sugerencia.familia)
            Text(sugerencia.texto, style = MaterialTheme.typography.bodyLarge)

            // Opción (1): precisa y segura → solo el puerto exacto.
            Button(
                onClick = onAutomatizarPuerto,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // COPY BORRADOR: botón opción "solo el puerto".
                Text("Automatizar solo el puerto ${sugerencia.puerto} (${sugerencia.servicio})")
            }

            // Opción (2): cómoda pero tosca → familia entera, con advertencia explícita.
            val listaPuertos = sugerencia.puertosFamilia.joinToString(", ")
            Text(
                // COPY BORRADOR: advertencia de la opción "familia entera".
                "⚠️ Cómodo pero tosco: aplicará \"${sugerencia.accionTexto}\" a TODO el tráfico " +
                    "futuro de estos puertos ($listaPuertos), incluidas amenazas disfrazadas dentro " +
                    "de la familia. Una regla amplia también puede dejar pasar lo malo.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onAutomatizarFamilia,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
            ) {
                // COPY BORRADOR: botón opción "familia entera".
                Text("Automatizar toda la familia ${sugerencia.familia}")
            }

            TextButton(onClick = onIrAReglas, modifier = Modifier.align(Alignment.End)) {
                Text("Ver mis reglas")
            }
        }
    }
}

@Composable
private fun TarjetaError(
    mensaje: String,
    onLimpiarError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Espaciado.md),
            verticalArrangement = Arrangement.spacedBy(Espaciado.sm),
        ) {
            Text(mensaje, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onLimpiarError, modifier = Modifier.align(Alignment.End)) {
                Text("Entendido")
            }
        }
    }
}

/**
 * Aviso breve tras aceptar una sugerencia del puente (cuántas reglas se crearon, o que ya estaban
 * cubiertas). No es un error: usa el color primario, no el de error.
 */
@Composable
private fun TarjetaAvisoReglas(
    mensaje: String,
    onLimpiar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Espaciado.md),
            verticalArrangement = Arrangement.spacedBy(Espaciado.sm),
        ) {
            Text(mensaje, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onLimpiar, modifier = Modifier.align(Alignment.End)) {
                Text("Entendido")
            }
        }
    }
}

@Composable
private fun FilaDato(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium)
        Text(valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

/**
 * Variante de [FilaDato] para DATOS TÉCNICOS (puerto, IP, país, ISP): el valor va en monoespaciado
 * ([TipografiaDatosTecnicos]) para que se lea "de consola". La etiqueta se mantiene en la tipografía
 * normal del tema.
 */
@Composable
private fun FilaDatoTecnico(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium)
        Text(valor, style = TipografiaDatosTecnicos)
    }
}

/** Formatea un delta con signo explícito: "+15", "-20", "0". */
private fun conSigno(valor: Int): String = when {
    valor > 0 -> "+$valor"
    else -> valor.toString()
}

// --- Previews ---

private val escenarioDemoMalicioso = CatalogoAtaques.escenarios[1] // SSH sospechoso.
private val escenarioDemoLegitimo = CatalogoAtaques.escenarios[0] // HTTPS legítimo.
private val escenarioDemoImposible =
    CatalogoAtaques.escenarios.first { it.dificultad == Dificultad.IMPOSIBLE }

@Preview(showBackground = true, name = "Sin decidir")
@Composable
private fun AtaqueEnVivoSinDecidirPreview() {
    NetTycoonTheme {
        AtaqueEnVivoScreen(
            estado = AtaqueEnVivoUiState(
                escenario = escenarioDemoMalicioso,
                nivel = Dificultad.MEDIO,
                partida = EstadoPartida(owner = "demo"),
                cargando = false,
                aciertos = 2,
                rondas = 3,
            ),
            onElegirNivel = {}, onCambiarNivel = {}, onReciclarNivel = {},
            onPermitir = {}, onBloquear = {}, onSiguienteAtaque = {},
            onLimpiarError = {}, onAutomatizarPuerto = {}, onAutomatizarFamilia = {},
            onLimpiarAvisoReglas = {}, onRefrescarRegen = {}, onIrAReglas = {}, onVolver = {},
        )
    }
}

@Preview(showBackground = true, name = "Acierto al permitir")
@Composable
private fun AtaqueEnVivoAciertoPermitirPreview() {
    NetTycoonTheme {
        AtaqueEnVivoScreen(
            estado = AtaqueEnVivoUiState(
                escenario = escenarioDemoLegitimo,
                nivel = Dificultad.FACIL,
                partida = EstadoPartida(owner = "demo"),
                cargando = false,
                aciertos = 3,
                rondas = 3,
                ultimoResultado = ResultadoDecision(
                    acierto = true,
                    categoria = CategoriaResultado.PERMISO_CORRECTO,
                    resultadoEvento = com.ejemplo.nettycoon.data.local.entity.ResultadoEvento.PERMITIDO,
                    leccion = escenarioDemoLegitimo.leccionAcierto,
                    deltaPuntaje = 10,
                    deltaSalud = 0,
                    deltaDinero = 50,
                ),
            ),
            onElegirNivel = {}, onCambiarNivel = {}, onReciclarNivel = {},
            onPermitir = {}, onBloquear = {}, onSiguienteAtaque = {},
            onLimpiarError = {}, onAutomatizarPuerto = {}, onAutomatizarFamilia = {},
            onLimpiarAvisoReglas = {}, onRefrescarRegen = {}, onIrAReglas = {}, onVolver = {},
        )
    }
}

@Preview(showBackground = true, name = "Fallo (brecha)")
@Composable
private fun AtaqueEnVivoFalloPreview() {
    NetTycoonTheme {
        AtaqueEnVivoScreen(
            estado = AtaqueEnVivoUiState(
                escenario = escenarioDemoMalicioso,
                nivel = Dificultad.MEDIO,
                partida = EstadoPartida(owner = "demo"),
                cargando = false,
                aciertos = 1,
                rondas = 2,
                ultimoResultado = ResultadoDecision(
                    acierto = false,
                    categoria = CategoriaResultado.BRECHA,
                    resultadoEvento = com.ejemplo.nettycoon.data.local.entity.ResultadoEvento.PERMITIDO,
                    leccion = escenarioDemoMalicioso.leccionError,
                    deltaPuntaje = 0,
                    deltaSalud = -20,
                    deltaDinero = -100,
                ),
            ),
            onElegirNivel = {}, onCambiarNivel = {}, onReciclarNivel = {},
            onPermitir = {}, onBloquear = {}, onSiguienteAtaque = {},
            onLimpiarError = {}, onAutomatizarPuerto = {}, onAutomatizarFamilia = {},
            onLimpiarAvisoReglas = {}, onRefrescarRegen = {}, onIrAReglas = {}, onVolver = {},
        )
    }
}

@Preview(showBackground = true, name = "Acierto con sugerencia")
@Composable
private fun AtaqueEnVivoConSugerenciaPreview() {
    NetTycoonTheme {
        AtaqueEnVivoScreen(
            estado = AtaqueEnVivoUiState(
                escenario = escenarioDemoMalicioso,
                nivel = Dificultad.MEDIO,
                partida = EstadoPartida(owner = "demo"),
                cargando = false,
                aciertos = 3,
                rondas = 3,
                ultimoResultado = ResultadoDecision(
                    acierto = true,
                    categoria = CategoriaResultado.BLOQUEO_CORRECTO,
                    resultadoEvento = com.ejemplo.nettycoon.data.local.entity.ResultadoEvento.BLOQUEADO,
                    leccion = escenarioDemoMalicioso.leccionAcierto,
                    deltaPuntaje = 15,
                    deltaSalud = 0,
                    deltaDinero = 50,
                    sugerencia = SugerenciaRegla(
                        puerto = escenarioDemoMalicioso.puerto,
                        servicio = escenarioDemoMalicioso.servicioNombre,
                        familia = MapeoFamilias.familiaDe(escenarioDemoMalicioso.puerto),
                        puertosFamilia = MapeoFamilias.puertosDe(
                            MapeoFamilias.familiaDe(escenarioDemoMalicioso.puerto),
                        ),
                        accion = AccionFirewall.DENY,
                        accionTexto = "Bloquear",
                        texto = "Acertaste 3 veces en la familia \"" +
                            MapeoFamilias.familiaDe(escenarioDemoMalicioso.puerto) +
                            "\" con la acción \"Bloquear\". Puedes crear reglas para que el " +
                            "firewall lo haga solo.",
                    ),
                ),
            ),
            onElegirNivel = {}, onCambiarNivel = {}, onReciclarNivel = {},
            onPermitir = {}, onBloquear = {}, onSiguienteAtaque = {},
            onLimpiarError = {}, onAutomatizarPuerto = {}, onAutomatizarFamilia = {},
            onLimpiarAvisoReglas = {}, onRefrescarRegen = {}, onIrAReglas = {}, onVolver = {},
        )
    }
}

@Preview(showBackground = true, name = "Automatizado por regla (acierto)")
@Composable
private fun AtaqueEnVivoAutomatizadoPreview() {
    NetTycoonTheme {
        AtaqueEnVivoScreen(
            estado = AtaqueEnVivoUiState(
                escenario = escenarioDemoMalicioso,
                nivel = Dificultad.MEDIO,
                partida = EstadoPartida(owner = "demo"),
                cargando = false,
                aciertos = 2,
                rondas = 4,
                ultimoResultado = ResultadoDecision(
                    acierto = true,
                    categoria = CategoriaResultado.BLOQUEO_CORRECTO,
                    resultadoEvento = com.ejemplo.nettycoon.data.local.entity.ResultadoEvento.BLOQUEADO,
                    leccion = escenarioDemoMalicioso.leccionAcierto,
                    deltaPuntaje = 0,
                    deltaSalud = 0,
                    deltaDinero = 0,
                    automatizadaPor = AutomatizacionRegla(
                        puerto = escenarioDemoMalicioso.puerto,
                        ip = null,
                        accionTexto = "Bloquear",
                    ),
                ),
            ),
            onElegirNivel = {}, onCambiarNivel = {}, onReciclarNivel = {},
            onPermitir = {}, onBloquear = {}, onSiguienteAtaque = {},
            onLimpiarError = {}, onAutomatizarPuerto = {}, onAutomatizarFamilia = {},
            onLimpiarAvisoReglas = {}, onRefrescarRegen = {}, onIrAReglas = {}, onVolver = {},
        )
    }
}

@Preview(showBackground = true, name = "Automatizado por regla (brecha)")
@Composable
private fun AtaqueEnVivoAutomatizadoBrechaPreview() {
    NetTycoonTheme {
        AtaqueEnVivoScreen(
            estado = AtaqueEnVivoUiState(
                escenario = escenarioDemoMalicioso,
                nivel = Dificultad.MEDIO,
                partida = EstadoPartida(owner = "demo"),
                cargando = false,
                aciertos = 2,
                rondas = 4,
                ultimoResultado = ResultadoDecision(
                    acierto = false,
                    categoria = CategoriaResultado.BRECHA,
                    resultadoEvento = com.ejemplo.nettycoon.data.local.entity.ResultadoEvento.PERMITIDO,
                    leccion = escenarioDemoMalicioso.leccionError,
                    deltaPuntaje = 0,
                    deltaSalud = 0,
                    deltaDinero = 0,
                    automatizadaPor = AutomatizacionRegla(
                        puerto = escenarioDemoMalicioso.puerto,
                        ip = null,
                        accionTexto = "Permitir",
                    ),
                ),
            ),
            onElegirNivel = {}, onCambiarNivel = {}, onReciclarNivel = {},
            onPermitir = {}, onBloquear = {}, onSiguienteAtaque = {},
            onLimpiarError = {}, onAutomatizarPuerto = {}, onAutomatizarFamilia = {},
            onLimpiarAvisoReglas = {}, onRefrescarRegen = {}, onIrAReglas = {}, onVolver = {},
        )
    }
}

@Preview(showBackground = true, name = "Selector de nivel")
@Composable
private fun AtaqueEnVivoSelectorPreview() {
    NetTycoonTheme {
        AtaqueEnVivoScreen(
            estado = AtaqueEnVivoUiState(cargando = false),
            onElegirNivel = {}, onCambiarNivel = {}, onReciclarNivel = {},
            onPermitir = {}, onBloquear = {}, onSiguienteAtaque = {},
            onLimpiarError = {}, onAutomatizarPuerto = {}, onAutomatizarFamilia = {},
            onLimpiarAvisoReglas = {}, onRefrescarRegen = {}, onIrAReglas = {}, onVolver = {},
        )
    }
}

@Preview(showBackground = true, name = "Nivel completado")
@Composable
private fun AtaqueEnVivoNivelCompletadoPreview() {
    NetTycoonTheme {
        AtaqueEnVivoScreen(
            estado = AtaqueEnVivoUiState(
                nivel = Dificultad.FACIL,
                nivelCompletado = true,
                partida = EstadoPartida(owner = "demo"),
                cargando = false,
                aciertos = 7,
                rondas = 9,
            ),
            onElegirNivel = {}, onCambiarNivel = {}, onReciclarNivel = {},
            onPermitir = {}, onBloquear = {}, onSiguienteAtaque = {},
            onLimpiarError = {}, onAutomatizarPuerto = {}, onAutomatizarFamilia = {},
            onLimpiarAvisoReglas = {}, onRefrescarRegen = {}, onIrAReglas = {}, onVolver = {},
        )
    }
}

@Preview(showBackground = true, name = "Imposible · pelado sin decidir")
@Composable
private fun AtaqueEnVivoImposibleSinDecidirPreview() {
    NetTycoonTheme {
        AtaqueEnVivoScreen(
            estado = AtaqueEnVivoUiState(
                escenario = escenarioDemoImposible,
                nivel = Dificultad.IMPOSIBLE,
                partida = EstadoPartida(owner = "demo"),
                cargando = false,
                aciertos = 4,
                rondas = 5,
            ),
            onElegirNivel = {}, onCambiarNivel = {}, onReciclarNivel = {},
            onPermitir = {}, onBloquear = {}, onSiguienteAtaque = {},
            onLimpiarError = {}, onAutomatizarPuerto = {}, onAutomatizarFamilia = {},
            onLimpiarAvisoReglas = {}, onRefrescarRegen = {}, onIrAReglas = {}, onVolver = {},
        )
    }
}

@Preview(showBackground = true, name = "Imposible · pelado acierto")
@Composable
private fun AtaqueEnVivoImposibleAciertoPreview() {
    NetTycoonTheme {
        AtaqueEnVivoScreen(
            estado = AtaqueEnVivoUiState(
                escenario = escenarioDemoImposible,
                nivel = Dificultad.IMPOSIBLE,
                partida = EstadoPartida(owner = "demo"),
                cargando = false,
                aciertos = 5,
                rondas = 5,
                ultimoResultado = ResultadoDecision(
                    acierto = true,
                    categoria = CategoriaResultado.BLOQUEO_CORRECTO,
                    resultadoEvento = com.ejemplo.nettycoon.data.local.entity.ResultadoEvento.BLOQUEADO,
                    leccion = escenarioDemoImposible.leccionAcierto, // oculta en pelado
                    deltaPuntaje = 15,
                    deltaSalud = 0,
                    deltaDinero = 50,
                ),
            ),
            onElegirNivel = {}, onCambiarNivel = {}, onReciclarNivel = {},
            onPermitir = {}, onBloquear = {}, onSiguienteAtaque = {},
            onLimpiarError = {}, onAutomatizarPuerto = {}, onAutomatizarFamilia = {},
            onLimpiarAvisoReglas = {}, onRefrescarRegen = {}, onIrAReglas = {}, onVolver = {},
        )
    }
}

@Preview(showBackground = true, name = "Imposible · pelado fallo")
@Composable
private fun AtaqueEnVivoImposibleFalloPreview() {
    NetTycoonTheme {
        AtaqueEnVivoScreen(
            estado = AtaqueEnVivoUiState(
                escenario = escenarioDemoImposible,
                nivel = Dificultad.IMPOSIBLE,
                partida = EstadoPartida(owner = "demo"),
                cargando = false,
                aciertos = 4,
                rondas = 5,
                ultimoResultado = ResultadoDecision(
                    acierto = false,
                    categoria = CategoriaResultado.BRECHA,
                    resultadoEvento = com.ejemplo.nettycoon.data.local.entity.ResultadoEvento.PERMITIDO,
                    leccion = escenarioDemoImposible.leccionError, // oculta en pelado
                    deltaPuntaje = 0,
                    deltaSalud = -20,
                    deltaDinero = -100,
                ),
            ),
            onElegirNivel = {}, onCambiarNivel = {}, onReciclarNivel = {},
            onPermitir = {}, onBloquear = {}, onSiguienteAtaque = {},
            onLimpiarError = {}, onAutomatizarPuerto = {}, onAutomatizarFamilia = {},
            onLimpiarAvisoReglas = {}, onRefrescarRegen = {}, onIrAReglas = {}, onVolver = {},
        )
    }
}
