package com.ejemplo.nettycoon.ui.ataque

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ejemplo.nettycoon.data.local.entity.AccionFirewall
import com.ejemplo.nettycoon.data.local.entity.EstadoPartida
import com.ejemplo.nettycoon.domain.firewall.MapeoFamilias
import com.ejemplo.nettycoon.domain.model.CategoriaResultado
import com.ejemplo.nettycoon.ui.theme.NetTycoonTheme

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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                // Selector de nivel: aún no se ha elegido dificultad.
                estado.nivel == null -> SelectorNivel(onElegirNivel = onElegirNivel)

                // Nivel completado: se agotaron los escenarios del nivel.
                estado.nivelCompletado -> {
                    CabeceraNivel(nivel = estado.nivel, onCambiarNivel = onCambiarNivel)
                    TarjetaContador(aciertos = estado.aciertos, rondas = estado.rondas)
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
                    CabeceraNivel(nivel = estado.nivel, onCambiarNivel = onCambiarNivel)
                    TarjetaContador(aciertos = estado.aciertos, rondas = estado.rondas)

                    TarjetaSituacion(escenario = escenario)
                    TarjetaPista(pista = escenario.textoPista)

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
                            )
                            BotonSiguiente(onSiguienteAtaque = onSiguienteAtaque)
                        }

                        // Decisión manual: veredicto (+ posible sugerencia del puente).
                        else -> {
                            TarjetaVeredicto(
                                resultado = resultado,
                                explicacion = escenario.explicacionAmpliada,
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
}

/**
 * Selector inicial: el jugador elige el nivel. Cada opción trae una línea que explica, en lenguaje
 * de novato, qué tipo de casos verá, para que la elección sea informada y no a ciegas.
 */
@Composable
private fun SelectorNivel(onElegirNivel: (Dificultad) -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                titulo = "Fácil",
                descripcion = "Casos claros para empezar: distingue lo normal de lo peligroso.",
                onClick = { onElegirNivel(Dificultad.FACIL) },
            )
            OpcionNivel(
                titulo = "Medio",
                descripcion = "Servicios sensibles y protocolos viejos: ya toca pensar un poco.",
                onClick = { onElegirNivel(Dificultad.MEDIO) },
            )
            OpcionNivel(
                titulo = "Difícil",
                descripcion = "Casos con trampa: lo que parece sospechoso puede ser legítimo (y al revés).",
                onClick = { onElegirNivel(Dificultad.DIFICIL) },
            )
        }
    }
}

@Composable
private fun OpcionNivel(
    titulo: String,
    descripcion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(descripcion, style = MaterialTheme.typography.bodySmall)
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
        Text(
            "Nivel: ${nivel.etiqueta()}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                horizontalArrangement = Arrangement.spacedBy(16.dp),
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
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Tu progreso", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Aciertos: $aciertos / $rondas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TarjetaSituacion(escenario: EscenarioAtaque, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Situación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(escenario.textoSituacion, style = MaterialTheme.typography.bodyLarge)
            FilaDato("Servicio", escenario.servicioNombre)
            FilaDato("Puerto", escenario.puerto.toString())
            FilaDato("IP origen", escenario.ipAtacante)
            FilaDato("País", escenario.pais)
            FilaDato("ISP", escenario.isp)
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("💡 Pista", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(pista, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * Dos botones grandes de igual peso visual. No se colorea "Bloquear" como acción peligrosa: ambas
 * decisiones pueden ser la correcta según el caso, y sesgar el color enseñaría lo contrario.
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
        horizontalArrangement = Arrangement.spacedBy(16.dp),
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
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
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
 */
@Composable
private fun TarjetaVeredicto(
    resultado: ResultadoDecision,
    explicacion: String,
    modifier: Modifier = Modifier,
) {
    val colores = if (resultado.acierto) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
    Card(modifier = modifier.fillMaxWidth(), colors = colores) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                if (resultado.acierto) "✅ ¡Bien hecho!" else "❌ Cuidado",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(resultado.leccion, style = MaterialTheme.typography.bodyLarge)
            Text(
                "Efecto en tu red",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            FilaDato("Puntaje", conSigno(resultado.deltaPuntaje))
            FilaDato("Salud de la red", conSigno(resultado.deltaSalud))
            FilaDato("Dinero virtual", conSigno(resultado.deltaDinero))

            SeccionPorQue(explicacion = explicacion)
        }
    }
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
            FilaDato("Puerto", automatizacion.puerto.toString())
            FilaDato("IP", automatizacion.ip ?: "Cualquiera")
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
            Text(resultado.leccion, style = MaterialTheme.typography.bodyLarge)
            Text(
                "Las rondas automáticas no suman puntaje: el puntaje premia decidir a mano.",
                style = MaterialTheme.typography.bodySmall,
            )

            SeccionPorQue(explicacion = explicacion)
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // COPY BORRADOR (validación del equipo): título de la tarjeta.
            Text(
                "🎓 Ya dominas esta familia: ¿la automatizas?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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

/** Formatea un delta con signo explícito: "+15", "-20", "0". */
private fun conSigno(valor: Int): String = when {
    valor > 0 -> "+$valor"
    else -> valor.toString()
}

// --- Previews ---

private val escenarioDemoMalicioso = CatalogoAtaques.escenarios[1] // SSH sospechoso.
private val escenarioDemoLegitimo = CatalogoAtaques.escenarios[0] // HTTPS legítimo.

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
            onLimpiarAvisoReglas = {}, onIrAReglas = {}, onVolver = {},
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
            onLimpiarAvisoReglas = {}, onIrAReglas = {}, onVolver = {},
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
            onLimpiarAvisoReglas = {}, onIrAReglas = {}, onVolver = {},
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
            onLimpiarAvisoReglas = {}, onIrAReglas = {}, onVolver = {},
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
            onLimpiarAvisoReglas = {}, onIrAReglas = {}, onVolver = {},
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
            onLimpiarAvisoReglas = {}, onIrAReglas = {}, onVolver = {},
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
            onLimpiarAvisoReglas = {}, onIrAReglas = {}, onVolver = {},
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
            onLimpiarAvisoReglas = {}, onIrAReglas = {}, onVolver = {},
        )
    }
}
