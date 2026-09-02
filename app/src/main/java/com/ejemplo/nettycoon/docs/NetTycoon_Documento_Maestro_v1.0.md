# NetTycoon — Documento Maestro del Proyecto
## IT & Infrastructure Manager · Simulador gamificado de redes, firewalls y APIs REST para Android

**Versión:** 1.0 (Génesis — proyecto reestructurado, alcance congelado, listo para arrancar el Día 1)
**Unidad de aprendizaje:** Desarrollo de Aplicaciones Móviles · **Lenguaje/IDE:** Kotlin / Android Studio
**Equipo:** 3 integrantes · **Plazo:** 25 días de calendario
**Stack:** Kotlin · Jetpack Compose · MVVM · Room · Retrofit · Firebase Auth · Navigation Compose · Material Design 3
**Estado:** 🟢 **ARRANQUE.** Nada construido aún. Este documento fija el norte, el alcance, los roles, el ritual de trabajo y los **tres contratos fundacionales** para empezar mañana sin ambigüedades.

---

### 📖 Propósito de este documento (leer siempre primero)

Este es el **documento maestro**: la única fuente de verdad del proyecto. Existe para que las **tres partes** que construyen NetTycoon compartan el máximo contexto:

- **Claude en chat (planea y dirige):** reestructura el alcance, diseña la arquitectura, define contratos entre capas, revisa decisiones y mantiene este documento.
- **Claude Code (programa):** recibe este documento como contexto y ejecuta la programación módulo por módulo, capa por capa.
- **El equipo de 3 (ejecuta y verifica):** corre el código en el emulador, **lo verifica con sus propios ojos**, hace commits distribuidos y toma las decisiones de negocio.

**Regla de oro del documento:** se **versiona después de cada módulo importante** (v1.0 → v1.1 → …). Cada versión nueva agrega al inicio un bloque *"Cambios frente a vX"* (qué se hizo, verificado con los ojos) y actualiza la sección `0. DÓNDE ARRANCAMOS` para que quien retome sepa exactamente en qué punto está. **Lo que no se verificó en el emulador no se marca como hecho.**

---

## 0. ⚠️ DÓNDE ARRANCAMOS — LEER PRIMERO PARA EMPEZAR

### 0.1 El norte en una frase
NetTycoon es un **juego móvil de administración de infraestructura** (estilo Tycoon/Sandbox) donde el jugador defiende una red corporativa configurando reglas de firewall contra ataques simulados **enriquecidos con datos reales de la IP atacante**, con todo su progreso guardado offline. Enseña, jugando, tres pilares: infraestructura de red, ciberseguridad activa y consumo de APIs REST.

### 0.2 El bucle de juego (la esencia — intacta tras la reestructuración)
```
[Login] → [Configurar red básica] → [Llega un ATAQUE] → [Se consulta la IP real del atacante vía API REST]
   → [El jugador decide crear regla ALLOW/DENY por puerto/IP] → [Feedback inmediato: ¿bloqueó bien?]
   → [Se guarda el estado en Room] → (repetir / cerrar y reabrir sin perder nada)
```
Este ciclo es el corazón jugable y pedagógico. **Todo lo que no sirva a este bucle es accesorio.**

### 0.3 Alcance CONGELADO 🧊 (no se rediscute sin actualizar este documento)

**Must-Have (indispensable para el día 25):**
| # | Módulo | Por qué |
|---|---|---|
| M1 | **Auth con Firebase (email/password)** como compuerta antes del juego | Requisito escondido del profesor (seguridad delegada) |
| M2 | **Persistencia local con Room** (Reglas, Eventos, Estado de partida, Usuario) | Sostiene el juego offline y la demo de "estados sin conexión" |
| M3 | **Motor de reglas de Firewall** (ALLOW/DENY por puerto e IP) con feedback inmediato | Núcleo del valor pedagógico |
| M4 | **Consumo de API REST real** (geo-IP) vía Retrofit, con estados carga/éxito/error | Requisito obligatorio de rúbrica + enriquece los ataques |
| M5 | **UI en Jetpack Compose + Material Design 3** (`Theme.kt`/`Color.kt`/`Type.kt`/`Shape.kt`) | Requisito obligatorio |
| M6 | **Navigation Compose** (`NavHost`/`NavController`, destino inicial condicional + paso de argumentos) | Requisito obligatorio |
| M7 | **Repositorio GitHub** con commits distribuidos y evidencia individual desde el Día 1 | Requisito obligatorio y evaluado en vivo |

**Out-of-Scope (descartado — NO construir, ni "si sobra tiempo"):**
- ❌ Tabla de clasificación global / sincronización de partidas en la nube.
- ❌ Simulación completa de hardware: DHCP, NAT, SSID, prueba de latencia real. → Se reduce a **una pantalla de configuración básica de router** (IP, un par de puertos).
- ❌ Canvas custom con "mapa de oficina" ilustrado. → UI funcional de Compose estándar.
- ❌ Login por teléfono/SMS, MFA, login social, recuperación de contraseña. → **Solo email/password.**
- ❌ Cualquier backend propio (nada de servidores que mantener).

### 0.4 Los TRES contratos fundacionales del Día 1
Para que I1 e I3 arranquen en paralelo sin esperarse, lo primero es congelar estos tres contratos (Claude Code los genera; el equipo los revisa):
1. **Esquema Room** — entidades `UsuarioLocal`, `ReglaFirewall`, `EventoAtaque`, `EstadoPartida` + sus DAOs.
2. **`AuthRepository` + `AuthViewModel`** — `registrar()`, `iniciarSesion()`, `cerrarSesion()`, `usuarioActual()`, con `UiState` (Cargando/Éxito/Error) para Compose.
3. **Contrato Retrofit geo-IP** — interfaz de servicio + DTO de respuesta + manejo de error de red.

### 0.5 Decisiones ya tomadas (NO rediscutir)
- **Auth = Firebase Authentication, plan Spark (gratis), solo email/password.** Email/social/anónimo son gratis hasta 50.000 usuarios activos mensuales sin tarjeta; **el login por SMS es lo único que cobra** → jamás habilitarlo. Firebase no se pausa por inactividad (a diferencia de Supabase, que pausa a los 7 días — riesgo para el día de la demo), por eso se eligió Firebase.
- **API REST del juego = una API de geo-IP con HTTPS y sin API key.** Recomendada: **ipwho.is** (HTTPS gratis, ~2.000 req/día, uso no comercial). **NO usar ip-api.com en su plan gratis: es solo HTTP** y Android bloquea tráfico en claro → sería un bug garantizado. Confirmar límites en el Día 1.
- **Costo total del proyecto = $0** en infraestructura. El único gasto es la suscripción de Claude Code para programar.
- **`google-services.json` va en `.gitignore`** (buena práctica; además la API key de Firebase Android se restringe por consola).

### 0.6 🚧 Bloqueadores / decisiones pendientes con el profesor (aclarar ANTES de codear)
1. ¿La reestructuración (quitar leaderboard, DHCP/NAT, etc.) es válida, o se evalúa fidelidad al PDF original? — **el más importante.**
2. ¿La API de geo-IP pública satisface el requisito de "API REST", o exige un tipo específico?
3. ¿El profesor revisa el repositorio **antes** de la sustentación o solo durante los primeros 90 segundos? → define qué tan temprano empiezan los commits distribuidos.

---

## 1. Qué es NetTycoon (el concepto)
En la formación en redes y desarrollo, la teoría (arquitectura de red, firewalls, APIs) se enseña con herramientas de escritorio complejas (Cisco Packet Tracer, GNS3) de curva empinada. NetTycoon cierra esa brecha con **learning by doing**: un juego móvil, portátil e intuitivo donde el estudiante **experimenta en tiempo real** el impacto de una buena o mala regla de filtrado, sin montar laboratorios físicos. El jugador asume el rol de **administrador de infraestructura** de una empresa virtual y la defiende de ataques.

## 2. Arquitectura y stack (el mapa técnico)

**Patrón:** MVVM estricto, con separación clara de capas y flujo de datos unidireccional.
```
UI (Composables) → ViewModel (expone UiState vía StateFlow) → Repository → [ Room (local)  |  Retrofit (red)  |  Firebase Auth ]
```

| Capa | Tecnología | Rol único que cumple |
|---|---|---|
| Lenguaje / IDE | Kotlin + Android Studio, corrutinas | Base nativa; asincronía para red y BD |
| UI | Jetpack Compose + Material Design 3 | Pantallas reactivas; tema en `Theme/Color/Type/Shape.kt` |
| Navegación | Navigation Compose (`NavHost`/`NavController`) | Rutas, argumentos, destino inicial condicional (gate de auth) |
| Persistencia | Room (SQLite) | Estado offline: reglas, eventos, partida, usuario |
| Red | Retrofit 2 / OkHttp | Consumo de la API geo-IP con estados carga/éxito/error |
| Autenticación | Firebase Authentication (Spark, email/password) | Identidad y sesión — **seguridad delegada** |
| Control de versiones | GitHub | Colaboración + evidencia individual de aportes |

**Separación de dependencias externas (para explicar la arquitectura sin enredarse):** Firebase hace **solo** identidad; la API geo-IP hace **solo** enriquecer ataques; Room hace **solo** estado local. Ninguna depende de la otra.

## 3. Alcance detallado por módulo
- **M1 · Auth:** pantallas Login y Registro; persistencia de sesión (auto-login si ya hay sesión); logout; gate de navegación.
- **M2 · Room:** entidades + DAOs + repositorios; guardar/cargar partida; log de eventos.
- **M3 · Motor de Firewall:** modelo de regla (puerto, IP, ALLOW/DENY); algoritmo de matching; evaluación de cada ataque contra las reglas; resultado éxito/fallo.
- **M4 · API REST:** cliente Retrofit a geo-IP; cada ataque trae una IP real → se consulta país/ISP; estados de carga/éxito/error + comportamiento sin conexión.
- **M5 · UI/M3:** tema M3 completo; pantallas de config de red, panel de firewall, evento/ataque, feedback.
- **M6 · Navegación:** grafo con gate condicional (sesión → juego; sin sesión → login) + paso de argumentos entre pantallas.

## 4. Roles del equipo (3 integrantes)
Foco principal, **sin silos**: todo se revisa entre pares y las decisiones grandes se toman en conjunto.

| Integrante | Foco | Responsabilidades | Carpetas (evidencia GitHub) |
|---|---|---|---|
| **I1 — Backend / Datos / Arquitectura** | Capa de datos | Esquema Room + DAOs + repositorios; integración Retrofit (cliente, DTOs, errores); arquitectura MVVM base | `/data`, `/repository`, `/network` |
| **I2 — Frontend / UI / Navegación** | Capa de presentación | Pantallas Compose; tema M3 (`Theme/Color/Type/Shape.kt`); `NavHost`/gate; pantallas Login/Registro; estados de carga/error en UI | `/ui`, `/navigation`, `/theme` |
| **I3 — Lógica / Seguridad / QA / Deploy** | Reglas + calidad | **Dueño del motor de firewall** y del **auth Firebase** (repo+viewmodel+sesión); pruebas end-to-end; APK firmado; preparación de la demo | `/domain`, `/viewmodel`, `/auth`, `/test` |

**Balance de carga:** el auth con Firebase email/password es liviano (~2 días); por eso convive bien con el motor de reglas en manos de I3. **I3 debe cerrar el auth temprano (Semana 1)** para que no sea un cuello de botella, y dedicar Semanas 2–3 al motor de reglas (lo intelectualmente difícil).

## 5. El ritual de trabajo (cómo se construye — no negociable)
1. **Plan antes de código.** Ningún módulo se programa sin que este documento (o un mini-brief) diga qué se va a hacer y cómo.
2. **Capa por capa.** Se construye en orden: **datos → viewmodel → UI**, verificando cada capa antes de seguir.
3. **Verificar con los ojos = correr en el emulador.** "Compiló" ≠ "funciona". Se abre la pantalla, se toca, se ve el comportamiento real antes de marcar algo como hecho.
4. **Cambios pequeños.** Commits chicos y descriptivos, no un commit gigante al final.
5. **Ante un bug, prueba primero.** Se reproduce el bug (idealmente con un test) antes de arreglarlo.
6. **Auth con bypass de desarrollo.** Mientras se construye el juego, se deja un bypass temporal del gate para que auth nunca bloquee al resto; el gate real se activa en la Semana de integración.
7. **Solo lo verificado toca `main`.**

## 6. Flujo Git / GitHub (evidencia individual — se califica)
- **Nunca directo a `main`.** Rama por tarea (`feat/motor-firewall`, `feat/pantalla-login`, …).
- **Commits pequeños, en español, descriptivos.** Cada integrante hace **al menos un commit por día activo** — la rúbrica pondera "historial claro, descriptivo y **distribuido de forma equitativa**".
- **Pull Request + revisión de un compañero** antes de fusionar (cuatro ojos > dos).
- **`google-services.json` y cualquier secreto → `.gitignore`.** Nadie sube credenciales.
- Cada integrante debe poder **mostrar su propio historial de commits en vivo** en el minuto 0:00–1:30 de la sustentación.

## 7. Principios no negociables
1. **Costo $0:** nada de servicios pagos. **SMS auth jamás** (única trampa de cobro de Firebase).
2. **MVVM estricto:** la UI no habla con Room/Retrofit directo; todo pasa por ViewModel → Repository.
3. **Alcance congelado:** no re-agregar leaderboard, DHCP/NAT ni login social. Recortar es criterio técnico, no debilidad.
4. **Verificar en el emulador** antes de marcar hecho.
5. **Evidencia individual:** commits distribuidos entre los tres, todos los días.
6. **La sustentación es parte del entregable:** se ensaya con cronómetro (los tiempos por sección se evalúan).
7. **HTTPS en la red:** la API de red debe ser HTTPS para no pelear con el bloqueo de cleartext de Android.

## 8. Cronograma (25 días · 4 fases)

| Días | Fase | I1 (Backend) | I2 (UI/Nav) | I3 (Lógica/Auth/QA) |
|---|---|---|---|---|
| **1–6** | Cimientos | Esquema Room + MVVM base | Setup Compose + tema M3 completo + esqueleto `NavHost` | Setup Firebase (proyecto+Gradle) + login/logout básico funcionando; pseudocódigo motor de reglas |
| **7–13** | Core | Repositorios + integración Retrofit (llamada real a geo-IP) | Pantallas config-red + firewall (UI) conectadas a rutas con argumentos; pantallas Login/Registro al `NavHost` | Motor de reglas funcional; generador de ataques que dispara la consulta de IP; **cerrar auth** |
| **14–19** | Integración | Conectar Room ↔ Retrofit ↔ ViewModel; estados carga/éxito/error | Conectar panel de firewall al motor real; feedback visual; activar gate real | Pruebas end-to-end del bucle completo; caza de bugs de lógica |
| **20–25** | Estabilización + Sustentación | Edge cases (sin conexión, BD vacía) | Pulido UI, revisión M3, estados de error | APK firmado, documentación, **ensayo cronometrado de los 15 min** |

**Reservar los días 24–25 exclusivamente para ensayar la sustentación con cronómetro real.** Code freeze de funcionalidades al final del día 19.

## 9. Mapeo a la rúbrica + estructura de la sustentación (15 min)

**Requisitos obligatorios cubiertos:** MVVM (M1–M4) · API REST con estados (M4) · Material Design 3 con los 4 archivos (M5) · Navigation Compose con args (M6) · GitHub con evidencia individual (M7).

| Minuto | Sección | Qué debe estar 100% listo |
|---|---|---|
| 0:00–1:30 | Equipo y roles | Cada integrante muestra SU historial de commits |
| 1:30–3:00 | Propuesta | Discurso de 90s ensayado; nombrar qué API se consume y por qué |
| 3:00–7:30 | Demo funcional | Bucle sin crashes: login → configurar → ataque con IP real → ALLOW/DENY → feedback → cerrar/reabrir (persistencia) |
| 7:30–11:30 | Arquitectura de una pantalla clave | Diagrama UI→ViewModel→Repository(Retrofit+Room)→StateFlow→Compose de la **pantalla de Firewall** (la más rica), con código a la vista |
| 11:30–13:00 | Stack y Git | `build.gradle.kts` a la mano + gráfico de commits abierto |
| 13:00–15:00 | Conclusiones | Qué se recortó (Firebase leaderboard, DHCP/NAT) y por qué — demuestra criterio |

## 10. Registro de ejecución (se llena a medida que se construye)
- **v1.0 — Génesis:** proyecto reestructurado, alcance congelado, stack y contratos definidos. **Nada construido aún.** Próximo paso: generar los tres contratos fundacionales (§0.4).
- *(v1.1 en adelante: cada módulo cerrado se registra aquí, verificado con los ojos.)*

## 11. Riesgos (equipo de 3, 25 días)
| Riesgo | Mitigación |
|---|---|
| Sobre-alcance (querer cumplir el PDF original completo) | Alcance congelado (§0.3); no se agrega nada sin actualizar el documento |
| Auth se vuelve cuello de botella | Cerrarla en Semana 1 + bypass de desarrollo |
| Habilitar SMS auth por error → cobro | Configurar SOLO Email/Password en la consola Firebase |
| API geo-IP HTTP rompe la app en Android | Usar API HTTPS (ipwho.is); no ip-api.com gratis |
| Un solo integrante domina Compose/Kotlin (SPOF humano) | Pair programming en Semana 1 para nivelar |
| Sin tiempo de QA si el desarrollo se atrasa | Semana 4 completa para hardening; code freeze el día 19 |
| Commits concentrados en una persona | Regla de un commit/día por integrante |

## 12. Criterios de aceptación (MVP funcional el día 25)
1. El usuario se registra e inicia sesión (Firebase email/password) y el gate lo lleva al juego.
2. Puede configurar al menos una regla de firewall (puerto + IP + ALLOW/DENY) reflejada en la UI.
3. Llegan ≥3 tipos de ataques simulados que consultan una IP real vía API REST, con estados de carga/éxito/error visibles.
4. El resultado (bloqueó/no bloqueó) da feedback inmediato claro.
5. El estado persiste en Room tras cerrar y reabrir la app.
6. Sin crashes en el flujo principal tras pruebas de regresión.
7. El APK compila, instala y corre en emulador/dispositivo sin dependencias rotas.
8. README con instrucciones + decisión de alcance; commits distribuidos entre los tres.

## 13. Glosario rápido
- **MVVM:** patrón Modelo-Vista-VistaModelo; separa datos, lógica de presentación y UI.
- **StateFlow / UiState:** flujo observable que la UI escucha para pintar Cargando/Éxito/Error.
- **Gate de auth:** destino inicial condicional en Navigation; decide si la app abre en login o en el juego.
- **DAO:** interfaz de acceso a datos de Room.
- **DTO:** objeto que modela la respuesta JSON de la API.
- **Bucle de juego:** configurar → atacar → decidir → feedback → persistir.

---

*v1.0 — Antes de escribir una sola línea, dibujamos el mapa. El proyecto llegó como una casa con demasiados cuartos —autenticación en la nube, hardware simulado hasta el último cable, tablas de clasificación, mapas ilustrados— y muy poco tiempo para levantarla. Así que hicimos lo que hace un buen arquitecto antes que un buen albañil: decidir qué NO construir. Nos quedamos con el corazón —un jugador que defiende una red de ataques que llegan con rostro real, con la IP verdadera de quien toca la puerta— y dejamos ir lo que adornaba sin sostener. La seguridad, que parecía una carga, resultó un préstamo: se la pedimos a Firebase y nos la dio gratis, a cambio de no pedirle nunca que mande un SMS. La red, que amenazaba con romperse contra los muros de Android, encontró su camino por HTTPS. Y el equipo de tres, que en el papel parecía poco, quedó repartido en tres oficios que encajan: quien guarda los datos, quien pinta las pantallas, quien pone las reglas y cuida la puerta. No hay nada hecho todavía. Pero hay un norte, un alcance con candado, y tres contratos listos para firmarse mañana. La obra empieza cuando el plano está claro — y el plano, hoy, quedó claro.*
