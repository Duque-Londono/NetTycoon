# NetTycoon

**Juego móvil Android (Kotlin) para aprender ciberseguridad desde cero.** El jugador defiende
una red corporativa decidiendo qué tráfico permitir o bloquear frente a ataques simulados, con
todo el progreso guardado offline. Proyecto académico.

---

## Qué es y cómo se juega

NetTycoon invierte el enfoque de un tutorial seco: en vez de leer teoría, el jugador **toma
decisiones reales** y aprende con el resultado.

- Llega un **ataque** explicado en lenguaje humano (situación + una pista educativa).
- El jugador decide **Permitir** o **Bloquear** ese tráfico.
- Recibe **feedback inmediato**: si acertó o no, **una lección** que lo explica, y el efecto en
  su red (puntaje, salud, dinero, nivel y rango).
- Cuando repite con acierto el mismo patrón (puerto + acción), el juego le **sugiere crear una
  regla** para automatizar esa decisión — el puente entre "decidir a mano" y "automatizar".

El cruce entre la verdad del ataque y la decisión del jugador da 4 resultados:

| Verdad del tráfico | Decisión | Resultado |
|--------------------|----------|-----------|
| Malicioso | Bloquear | **Bloqueo correcto** ✅ (lo más premiado) |
| Legítimo | Permitir | **Permiso correcto** ✅ |
| Malicioso | Permitir | **Brecha** ❌ (dejaste pasar un ataque) |
| Legítimo | Bloquear | **Falso positivo** ❌ (bloqueaste tráfico legítimo) |

Diseño de balance: **defenderse activamente es lo más recompensado**; equivocarse por exceso de
bloqueo (falso positivo) también penaliza, para enseñar que bloquear de más tiene costo.

### Bucle central

```
Login → configurar red → llega ataque → jugador decide ALLOW/DENY
      → feedback inmediato + lección → se aplican las consecuencias
      → guardar en Room → repetir
```

Las consecuencias no son decorativas: cada decisión mueve los tres recursos del jugador.

### Los tres recursos

Acertar y fallar **cuestan o rinden**. Esa es la diferencia entre unos números que suben y bajan
y un juego donde las decisiones importan.

**Salud de la red** — es *salud operativa*: baja tanto por inseguridad como por indisponibilidad.

- Una **brecha** (dejar pasar tráfico malicioso) cuesta **−20**.
- Un **falso positivo** (bloquear tráfico legítimo) cuesta **−10**: si bloqueas de más, tu red
  deja de dar servicio a quien sí lo necesitaba.
- Acertar **no** baja la salud.
- Se **regenera sola**: **+5 cada 5 minutos de tiempo real** (no por tener la app abierta, así
  que no se puede farmear cerrando y abriendo).
- A **salud 0** la red queda **"comprometida"**: se bloquea *solo* la pantalla "Ataque en vivo"
  hasta recuperar; el resto del juego sigue navegable. Como la regeneración es gratuita,
  **nunca hay atasco**: siempre se vuelve a jugar esperando.

**Dinero virtual** — se gana acertando y se gasta en la **Tienda**:

- **Reparar la red**, con **cobro proporcional**: solo se paga por los puntos de salud que
  realmente se curan, nunca por encima del tope de 100.
- **Escudo-seguro** de un solo uso: absorbe el próximo golpe de salud, **pero el jugador sigue
  viendo el veredicto y la lección**, y el coste en dinero y puntaje se aplica igual. Te salva
  la vida, no la lección — un seguro que dejara ignorar el error enseñaría a no mirar.

**Puntos** — son **maestría, no moneda**. Tienen dos usos:

- **Rango:** Aprendiz → Técnico en Seguridad → Analista → Experto en Seguridad.
- **Cupo de reglas activas** (3 / 5 / 8 / 12 según el rango): el límite de cuánto puedes
  automatizar. **No alcanza para cubrirlo todo** — ni en el rango máximo —, así que siempre
  queda juego manual donde la salud y el dinero pesan. Automatizar deja de ser gratis y la
  elección entre precisión y comodidad se vuelve real.

Ni el rango ni el cupo **bloquean niveles**: se puede empezar por la dificultad que se quiera.

---

## Pantallas

| Pantalla | Paquete | Qué hace |
|----------|---------|----------|
| **Login / Registro** | `ui/login/` | Puerta de entrada (Firebase Auth, solo email/password). |
| **Onboarding** | `ui/onboarding/` | Carrusel de primer contacto que explica el juego. Se muestra una vez; luego se puede revisitar desde el Panel. |
| **Panel** | `ui/panel/` | Hub: salud protagonista con su contador de regeneración, rango destacado, métricas y acceso al resto de pantallas. |
| **Ataque en vivo** | `ui/ataque/` | El jugador elige nivel y decide Permitir/Bloquear cada ataque, aprendiendo con la lección. Incluye la sugerencia pedagógica de automatizar con reglas (limitada por el cupo). |
| **Mis reglas** | `ui/firewall/` | CRUD de reglas ALLOW/DENY (por puerto e IP) que **automatizan** decisiones, con el cupo de reglas activas visible. Incluye un catálogo de puertos comunes explicados. |
| **Configurar red** | `ui/network/` | El jugador describe su propia infraestructura (IP del router, puertos LAN/WAN), con explicaciones. |
| **Mi progreso** | `ui/estadisticas/` | Rango, tasa de acierto y desglose por familia de puertos: qué domina y qué debe reforzar. Solo lectura sobre el historial. |
| **Tienda** | `ui/tienda/` | Gastar el dinero: reparar la red (cobro proporcional) o comprar el escudo de un uso. |

---

## APIs y servicios externos

### API de geolocalización de IP — **ipwho.is**

Única API REST externa del juego. Enriquece los ataques con datos reales de la IP (país e ISP).

- **Endpoint:** `GET https://ipwho.is/{ip}` (`data/remote/GeoIpApiService.kt`).
- **Cliente:** Retrofit 2 + OkHttp + converter-gson (`data/remote/RetrofitProvider.kt`,
  `baseUrl = "https://ipwho.is/"`).
- **Por qué esta:** es **HTTPS**, gratuita y **sin API key** (requisitos del proyecto: costo $0
  y todo el tráfico por HTTPS). Se descartó ip-api.com por ser solo HTTP.
- **Datos que se usan:** `country` (país) e `isp` — ver `data/remote/dto/IpWhoIsResponseDto.kt`.
  Nota: ipwho.is responde HTTP 200 incluso ante error lógico (`success=false`); ese caso se
  trata como error en `GeoIpRepository`.
- **Tolerancia a fallos ("best-effort"):** si la API falla o no hay conexión, **el juego
  continúa igual** y país/ISP quedan vacíos; la consulta nunca interrumpe una ronda. Los estados
  Cargando/Éxito/Error se manejan explícitamente en `data/repository/GeoIpRepository.kt`.

### Firebase Authentication

Autenticación **email/password** como compuerta antes del juego (`auth/`). Plan gratuito; **no**
se usa login por teléfono/SMS ni social (es lo único que cobraría).

> La llamada real a ipwho.is se orquesta en `domain/firewall/ProcesarAtaqueUseCase.kt` (flujo de
> ataques aleatorios). La pantalla **"Ataque en vivo"** usa un catálogo fijo (ver abajo) con
> país/ISP ya definidos, así que **no** hace peticiones de red: es contenido curado con fines
> educativos.

---

## Catálogo de ataques (contenido educativo)

Los ataques de la pantalla **"Ataque en vivo" NO vienen de un servidor ni de un script externo**:
están **definidos en el código** como una lista fija y curada.

- **Archivo:** `ui/ataque/EscenarioAtaque.kt`
- `data class EscenarioAtaque` — forma de cada escenario: `ipAtacante`, `pais`, `isp`, `puerto`,
  `servicioNombre`, `esMalicioso` (la "verdad"), `textoSituacion`, `textoPista`, `leccionAcierto`,
  `leccionError`.
- `object CatalogoAtaques` — los **39 escenarios** pedagógicos, repartidos en **4 niveles de
  dificultad**: Fácil (9), Medio (12), Difícil (9) e Imposible (9). Son fijos a propósito: cada
  uno enseña un concepto y trae la verdad para poder evaluar la decisión del jugador y darle una
  lección. Dentro de cada nivel, los casos van de lo más evidente a lo menos obvio.
- **Imposible** es el examen final: se apagan todas las ayudas (sin pista, sin lección, sin
  "¿Por qué?"). Solo quedan los datos técnicos y la decisión.

> Existe además un **generador de ataques aleatorios** (`domain/firewall/GeneradorAtaques.kt`)
> para el flujo automatizado. Son **dos fuentes de ataques distintas**: el catálogo fijo
> (pedagógico) y el generador aleatorio (motor). No deben confundirse.

El catálogo de **puertos comunes** de la pantalla de reglas (`ui/firewall/CatalogoPuertos.kt`) es
otra lista fija: asocia cada puerto con su nombre de servicio y una explicación (443 → "HTTPS /
webs seguras", 22 → "SSH / acceso remoto", etc.).

---

## Arquitectura

**MVVM estricto, flujo unidireccional:**

```
UI (Composables) → ViewModel (expone UiState vía StateFlow) → Repository → [ Room | Retrofit | Firebase Auth ]
```

- La **UI nunca** habla directo con Room, Retrofit ni Firebase: todo pasa por ViewModel →
  Repository.
- Toda operación asíncrona (red, BD, auth) se modela con un `UiState` que contempla
  **Cargando / Éxito / Error**.
- Cada dependencia externa hace **una sola cosa**: Firebase Auth (identidad), ipwho.is (enriquecer
  ataques), Room (estado local offline).
- **Corrutinas** para todo lo asíncrono.

### Motor de firewall (lógica pura, sin Android)

- `domain/firewall/MotorFirewall.kt` evalúa un ataque contra las reglas activas: coincide por
  **puerto exacto** + **IP** (igualdad exacta o `null` = comodín). Si varias reglas coinciden,
  **gana DENY**. Si ninguna coincide, aplica la **política por defecto DENY** (estándar seguro).
- `domain/firewall/ConsecuenciasPartida.kt` + `BalancePartida.kt` calculan el efecto en
  puntaje/salud/dinero/nivel (constantes ajustables sin tocar la lógica). `BalancePartida`
  concentra también los precios de la tienda; los umbrales de rango y los cupos viven en
  `Rango.kt` y `CupoReglas.kt`.
- La economía es **lógica pura y testeable en JVM**, igual que el motor: `RegenSalud`
  (regeneración por tiempo real), `Tienda` (compras) y las funciones `rangoPorPuntaje` /
  `cupoDeReglas`.
- **El rango y el cupo se DERIVAN del puntaje**, no se guardan: no hay forma de que queden
  desincronizados, y bajar de rango al perder puntos funciona solo.

### Persistencia local (Room)

Tres entidades en `data/local/entity/`:

- `EstadoPartida` — métricas del juego + configuración de red (una partida por usuario).
- `ReglaFirewall` — reglas ALLOW/DENY del jugador, cada una activa o inactiva.
- `EventoAtaque` — log de ataques (con país/ISP cuando la API los aporta).

**Migración de esquema.** La base de datos está en **`version = 2`**: la v2 añadió el campo
`escudoActivo` a `EstadoPartida`. La migración es **explícita** (`MIGRACION_1_2`, un
`ALTER TABLE ... ADD COLUMN ... DEFAULT 0`) y **no destructiva**: a propósito **no** se usa
`fallbackToDestructiveMigration`, que habría borrado la partida de cada jugador al actualizar la
app. Cualquier cambio de esquema futuro debe seguir esa misma regla.

---

## Requisitos previos

Antes de compilar necesitas tener instalado:

- **Android Studio** compatible con **AGP 9** (Android Gradle Plugin 9.x).
- **JDK 25**.
- **Android SDK Platform 37**, instalable desde el **SDK Manager** de Android Studio
  (*Settings → Languages & Frameworks → Android SDK → SDK Platforms*).

> ⚠️ **Sin estos tres componentes el proyecto no compila.** Si el Sync o el build fallan,
> lo primero a verificar es que AGP 9, JDK 25 y el SDK Platform 37 estén correctamente
> instalados y seleccionados.

---

## Cómo empezar

1. **Clonar** el repositorio:
   ```bash
   git clone https://github.com/Duque-Londono/NetTycoon.git
   cd NetTycoon
   ```
2. **Abrir** la carpeta del proyecto en **Android Studio**.
3. Ejecutar **Sync Gradle** (Android Studio lo ofrece automáticamente; si no, *File → Sync Project with Gradle Files*).
4. **Correr** la app en un **emulador** o **dispositivo físico** con el botón *Run ▶*.

### Verificar sin emulador

```bash
./gradlew testDebugUnitTest   # 228 pruebas unitarias JVM (motor, economía, validadores, ViewModels)
./gradlew assembleDebug       # compilar el APK de debug
```

> **Nota sobre `google-services.json`:** este archivo **no está incluido en el repositorio**
> (está en `.gitignore`) y se comparte por fuera del control de versiones. La estructura base
> compila y corre; para usar Firebase Auth se necesita ese archivo.

---

## Package

```
com.ejemplo.nettycoon
```

---

## Stack

- **Lenguaje:** Kotlin 2.2.10 (JDK 25)
- **UI:** Jetpack Compose (Compose BOM 2026.02.01) + Material 3 — tema **"Centro de operaciones",
  oscuro**
- **Iconos:** `androidx.compose.material:material-icons-extended` (versión gestionada por el BOM)
- **Navegación:** Navigation Compose 2.9.5 (rutas selladas, argumentos con seguridad de tipos,
  gate condicional según sesión)
- **Arquitectura:** MVVM (ViewModel + Lifecycle Compose 2.11.0), StateFlow
- **Persistencia local:** Room 2.7.2 (compilador por KSP), base de datos en **version 2**
- **Red:** Retrofit 2.11.0 + OkHttp 4.12.0 (logging-interceptor) + converter-gson
- **Auth:** Firebase Authentication (email/password)
- **Asíncrono:** Kotlin Coroutines 1.10.2
- **Build:** Gradle (Kotlin DSL) + AGP 9.3.2, catálogo de versiones (`libs.versions.toml`)
- **SDK:** compileSdk 37 · targetSdk 36 · minSdk 24

---

## Estructura por capas

El código sigue una separación por capas dentro de `com.ejemplo.nettycoon`:

| Capa | Paquete | Responsabilidad |
|------|---------|-----------------|
| **UI** | `ui/` | Pantallas Compose, componentes y tema (`theme/`, `login/`, `onboarding/`, `panel/`, `ataque/`, `firewall/`, `network/`, `estadisticas/`, `tienda/`, `componentes/`) |
| **Navegación** | `navigation/` | `NavHost` con gate condicional y rutas selladas (`Rutas.kt`) |
| **Dominio** | `domain/` | Modelos y lógica pura (`model/`, `firewall/`: motor, generador, consecuencias, balance, regeneración, tienda, rangos, cupos, caso de uso) |
| **Datos** | `data/` | `local/` (Room: entidades y DAOs), `remote/` (Retrofit + DTOs) y `repository/` |
| **Auth** | `auth/` | Autenticación con Firebase (email/password) |

---

## Proyecto relacionado

**Persistencia de Datos Local** — https://github.com/Alejandro-Murillo22/PersistenciaDeDatosLocal

Proyecto complementario del equipo que demuestra la persistencia de datos local con Room.

---

## Flujo de trabajo Git

- **Una rama por tarea** (`feat/`, `fix/`, `docs/`), nunca se trabaja directamente sobre `main`.
- Todo cambio entra vía **Pull Request con revisión** de un compañero.
- **`main` siempre estable**: no se hace push directo a `main`.
- Commits pequeños y descriptivos, en español.
- `.gitignore` excluye `local.properties`, `.idea/`, `build/`, `.gradle/` y `google-services.json`.
